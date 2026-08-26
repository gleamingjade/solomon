# Session Takeover (HTTP + WebSocket)

**Status: implemented (both layers).**

Goal: a user can only be logged in from one place at a time. Logging in from a new device/browser should silently kick out the old HTTP session *and* the old chat WebSocket connection, if either exists. This doc covers both layers and how they interact.

## HTTP Layer: Concurrent Session Control

Spring Security's built-in feature for this is `maximumSessions`:

```java
http.sessionManagement(session -> session
    .maximumSessions(1)
    .maxSessionsPreventsLogin(false) // new login wins, old session gets invalidated
    .sessionRegistry(sessionRegistry())
);
```

`maxSessionsPreventsLogin(false)` is the chosen behavior: a new login is always allowed, and it evicts whatever session existed before. (The alternative, `true`, would reject the new login while an old session is still alive — not what we want here.)

### The registry has to be Redis-backed, not the default

The default `SessionRegistry` (`SessionRegistryImpl`) is a plain in-memory `Map`. That's fine on one instance, but this app already runs `spring-session-data-redis`, and the chat layer (see [[Chat Routing System Design]]) is explicitly built to scale to multiple instances. An in-memory registry would only see sessions created on its own JVM — a user could exceed the session cap by hitting different instances.

Fix: use `SpringSessionBackedSessionRegistry` (from `spring-session-core`) instead of the default. It queries Redis directly via `FindByIndexNameSessionRepository`, so it's correct regardless of how many app instances are running.

Requirement: the Redis session repository needs to be the *indexed* variant (plain `RedisSessionRepository` can't look sessions up by principal):

```yaml
spring:
  session:
    redis:
      repository-type: indexed
```

### Why this works cleanly across both login methods

`maximumSessions` groups sessions by `Authentication.getName()`. In this app that already resolves to the **member id string** for both login paths — confirmed by reading the actual principal classes:

- Form login: `SessionRedisEmailUserService.loadUserByUsername()` builds `new User(String.valueOf(member.getId()), ...)` — username is the member id.
- OAuth2 login: `SessionRedisOidcUser.getName()` returns `sessionMember.getId()`.

So a session opened via Google login and one opened via email/password, for the same member, are correctly recognized as "the same principal" and count against the same limit. No extra wiring needed for this part — see [[Session & Redis Architecture]] for the full principal/session trace.

### TODO

- [x] Added `spring.session.redis.repository-type: indexed` to `application-local-oauth2.yml`.
- [x] `SessionRegistry` bean (`SpringSessionBackedSessionRegistry`) defined in `SessionRedisConfig`.
- [x] `maximumSessions(1).maxSessionsPreventsLogin(false).sessionRegistry(...)` wired into `SecurityConfig`'s filter chain.

## WebSocket Layer: Kicking the Old Chat Connection

Concurrent HTTP session control does **not** touch already-open WebSocket connections — Spring Security's `ConcurrentSessionFilter` only re-checks a session's validity on the *next HTTP request* from that session. A browser tab sitting on an open STOMP connection never makes another HTTP request, so the old chat connection would stay alive indefinitely unless the app closes it explicitly.

### Why cross-instance lookup is not needed here

In a generic horizontally-scaled STOMP setup, "find and kick this user's existing connection" is hard: the new handshake could land on a different instance than the old connection, and `SimpMessagingTemplate.convertAndSendToUser` only knows about sessions on its own JVM (`WebSocketConfig` uses `enableSimpleBroker`, the in-memory broker — no cross-instance user registry is wired up).

This app sidesteps that entirely because of the routing rule in [[Chat Routing System Design]]: a user can only be connected to **one chat room at a time**, and every connection for a given trial is always routed to the same server (`trial:{trial_id}:server` in Redis, "same-server locality"). So a re-login always means: same trial → same server → the old connection (if it exists) is on the exact instance handling the new handshake. Plain local `convertAndSendToUser` is sufficient; no Redis-backed multi-server user registry is needed.

(The one case where this reasoning could break — old server died, new server reassigned via failover — isn't actually a problem: if the old server is dead, the old WebSocket connection is already dead too. Nothing to kick.)

### How the connecting principal gets there

`determineUser()` (`SecurityContextIntegrationHandShakeHandler`) runs *during* the HTTP handshake, before the WebSocket connection exists yet — it resolves the STOMP `Principal` to the member's `SessionMember` (`getName()` = member id). Following the actual `spring-websocket` source: `AbstractHandshakeHandler.doHandshake()` calls `determineUser()` and passes the result straight into `requestUpgradeStrategy.upgrade(...)`, which (`AbstractStandardUpgradeStrategy.upgrade()`) does `new StandardWebSocketSession(headers, attrs, localAddr, remoteAddr, user)` — the principal is baked into the session object itself, *before* `afterConnectionEstablished()` ever fires. That means `session.getPrincipal()` is already available at the earliest point a `WebSocketHandler` sees the connection — no need to wait for a STOMP CONNECT frame to know who this is.

### Final design: one map, keyed by member id, in the same decorator that tracks sessions for force-close

`ChatWebSocketSessionTracker` (a `WebSocketHandlerDecoratorFactory`, wired via `configureWebSocketTransport`) does everything in one place:

```java
@Override
public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    Principal principal = session.getPrincipal();
    if (principal != null) {
        WebSocketSession previous = sessionsByMemberId.put(principal.getName(), session);
        if (previous != null) {
            kick(principal.getName(), previous);
        }
    }
    super.afterConnectionEstablished(session);
}
```

`Map.put(key, value)`'s contract is to atomically swap in the new value *and* return whatever was there before. That's exactly "register the new connection and find out if there was an old one" in a single call — no separate get-then-put race window, and no possibility of ever seeing the just-registered session as its own "previous" (it's definitionally a different object). This replaced an earlier, more roundabout design (see below) that leaned on `SimpUserRegistry`/`SessionConnectEvent` instead.

`afterConnectionClosed` removes the entry, but only if it's still the *current* session for that member (`sessionsByMemberId.remove(memberId, session)` — the two-arg form only removes when the mapped value still equals `session`), so a late close event for an already-superseded session can't wipe out the new one's registration.

`kick(memberId, previousSession)` does both:
- **Graceful**: `simpMessagingTemplate.convertAndSendToUser(memberId, "/queue/session-replaced", payload, headers-with-previous.getId())` — targeted at the specific old session id, not a broadcast to `memberId` (which could also hit the new connection once it's STOMP-connected). A real client sees this and can show a "logged in elsewhere" UI before disconnecting itself.
- **Forced**: `previousSession.close(CloseStatus.POLICY_VIOLATION)` directly. This is the part that actually enforces anything — a client that never processes STOMP frames at all (Postman, a raw WebSocket script, anything non-cooperating) would just ignore the graceful message and keep the connection alive forever. Only a server-initiated `.close()` on the raw session is guaranteed to end it regardless of client behavior.

### Evolution: why not `SimpUserRegistry` / `SessionConnectEvent`?

First attempt used `SimpUserRegistry` (Spring's own STOMP-level connection tracker) queried from a `SessionConnectEvent` listener, reasoning that since `DefaultSimpUserRegistry` only registers a session on `SessionConnectedEvent` (confirmed by reading its bytecode) — not `SessionConnectEvent` — querying it earlier would never see the not-yet-registered new connection.

That worked, but had two problems:
- It depends on an *undocumented* internal detail of `DefaultSimpUserRegistry` — nothing guarantees that ordering holds in a future Spring version. (Mitigated at the time by also explicitly excluding the new connection's session id from the query results — but that's a workaround for a design that shouldn't have needed it.)
- `SimpSession` (what `SimpUserRegistry` exposes) only has `getId()`/`getUser()`/`getSubscriptions()` — no reference to the actual transport-level `WebSocketSession`, so a *second*, separate `sessionId -> WebSocketSession` registry was still needed just to force-close anything. Two registries for one problem.

Realizing the principal is available on `session.getPrincipal()` from the moment `afterConnectionEstablished` fires (see above) made both of those unnecessary: one `Map<memberId, WebSocketSession>`, populated at the earliest possible moment (handshake completion, before the new client even sends its STOMP CONNECT), using a plain `Map` operation whose atomicity is a documented language-level guarantee rather than an assumption about a specific framework's internals.

### Implementation notes

`HandshakeInterceptor.afterHandshake()` turned out to be the wrong place for any of this — its signature (`request, response, wsHandler, exception`) doesn't expose the `attributes` map, the new `WebSocketSession`, or whatever `determineUser()` returned. The interceptor (renamed `AuthenticationRequiredHandshakeInterceptor` — it no longer has anything to do with the single-session policy) now only gates: reject the handshake if there's no authenticated `SessionMemberHolder` principal.

Also: `authentication.getPrincipal() instanceof SessionMember` is always `false` for real logins — the HTTP-side principal is `SessionRedisEmailUser`/`SessionRedisOidcUser`, which each *hold* a `SessionMember` field, not *are* one. Fixed by introducing `SessionMemberHolder` (`getSessionMember()`), implemented by both wrapper classes. (`SessionMember` itself doesn't need to implement it — nothing in this codebase ever does an `instanceof SessionMemberHolder` check against a raw `SessionMember`; the STOMP-side code just uses it as a plain `Principal`.)

Also: `configureMessageBroker` only had `registry.enableSimpleBroker("/topic")` — `/queue/session-replaced` would've silently gone nowhere, since the simple broker only routes destinations under prefixes it's told about. Added `/queue` to the enabled prefixes.

### TODO

- [x] `AuthenticationRequiredHandshakeInterceptor` rejects handshakes without an authenticated `SessionMemberHolder` principal.
- [x] `DefaultHandshakeHandler.determineUser()` (`SecurityContextIntegrationHandShakeHandler`) resolves the STOMP `Principal` to the member's `SessionMember` (whose `getName()` is the member id).
- [x] `ChatWebSocketSessionTracker` tracks `memberId -> WebSocketSession`, kicks (graceful message + forced `.close()`) whatever `Map.put()` returns as the previous value on each new connection.
- [ ] Client-side: handle the kick message (`/user/queue/session-replaced`) by calling `disconnect()` and showing a "logged in elsewhere" state (still worth doing for UX, even though the server now enforces it either way).

## References

- `common/adapter/in/web/security/config/SecurityConfig.java`, `SessionRedisConfig.java`
- `common/adapter/in/web/security/SessionMemberHolder.java`, `SessionMember.java`
- `common/adapter/in/web/security/form/SessionRedisEmailUserService.java`, `SessionRedisEmailUser.java`
- `common/adapter/in/web/security/oauth/SessionRedisOidcUser.java`
- `feature/chat/adapter/in/websocket/SecurityContextIntegrationHandShakeHandler.java`
- `feature/chat/adapter/in/websocket/AuthenticationRequiredHandshakeInterceptor.java`
- `feature/chat/adapter/in/websocket/ChatWebSocketSessionTracker.java`
- `feature/chat/adapter/websocket/config/WebSocketConfig.java` (neutral package — shared wiring, not itself an in/out adapter)
- `feature/chat/adapter/out/websocket/StompChatMessagePublisher.java`
- [[Session & Redis Architecture]]
- [[Chat Routing System Design]]
