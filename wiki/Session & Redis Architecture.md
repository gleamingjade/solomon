# Session & Redis Architecture

How a login (OAuth2 or email/password) ends up as a session stored in Redis, and how that session is read back on later requests.

## Two Separate Layers

- **Spring Session** — decides *where* the session lives. It intercepts `HttpServletRequest.getSession()` and backs it with Redis instead of the servlet container's in-memory store.
- **Spring Security** — decides *what* goes into the session (the `SecurityContext`, i.e. who's logged in).

These two are independent libraries that happen to plug into each other through one shared concept: the `HttpSession` attribute map.

## Spring Session: Intercepting `getSession()`

Spring Session registers `SessionRepositoryFilter`, which wraps the incoming request in a `SessionRepositoryRequestWrapper`. That wrapper overrides `getSession()` / `getSession(boolean create)` / `changeSessionId()` and delegates all of it to a `SessionRepository<S extends Session>` interface — the filter itself has no idea what's behind that interface.

`spring-session-data-redis` supplies the Redis implementation of that interface (`RedisSessionRepository`). Swap the dependency for `spring-session-jdbc` and none of the filter/security code changes — only the storage backend does.

At the very end of the request, the wrapper commits whatever is currently in the session:

```java
// SessionRepositoryFilter.SessionRepositoryRequestWrapper (simplified)
private void commitSession() {
    HttpSessionWrapper wrappedSession = (HttpSessionWrapper) getSession(false);
    if (wrappedSession != null) {
        sessionRepository.save(wrappedSession.getSession());
    }
}
```

This only runs if `getSession()` was actually called during the request. If nothing touched the session, nothing is saved.

## Spring Security: Getting the Principal Into the Session

`HttpSessionSecurityContextRepository` is the bridge — it knows how to load/save a `SecurityContext` from/to an `HttpSession` attribute (`SPRING_SECURITY_CONTEXT`).

Two filters touch it, at very different points:

**`SecurityContextHolderFilter`** (outermost, runs on every request) — in modern Spring Security (5.7+/6.x) this filter *only loads*:

```java
Supplier<SecurityContext> deferredContext = securityContextRepository.loadDeferredContext(request);
try {
    securityContextHolderStrategy.setDeferredContext(deferredContext);
    chain.doFilter(request, response);
} finally {
    securityContextHolderStrategy.clearContext(); // no save here
}
```

Unlike the older, now-deprecated `SecurityContextPersistenceFilter`, it does **not** auto-save whatever ends up in `SecurityContextHolder` after the chain runs. Saving is each authentication mechanism's own responsibility.

**`AbstractAuthenticationProcessingFilter.successfulAuthentication()`** (runs once, right after a login attempt succeeds) — this is what actually saves:

```java
SecurityContext context = securityContextHolderStrategy.createEmptyContext();
context.setAuthentication(authResult);
securityContextHolderStrategy.setContext(context);
securityContextRepository.saveContext(context, request, response); // <- creates the session if needed
successHandler.onAuthenticationSuccess(request, response, authResult);
```

`HttpSessionSecurityContextRepository.saveContext()` is where, for a real (non-anonymous) context, the `HttpSession` gets created if it doesn't exist yet, and the context object is stored via `session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context)`.

`UsernamePasswordAuthenticationFilter` and `OAuth2LoginAuthenticationFilter` both extend `AbstractAuthenticationProcessingFilter`, so both go through this exact same save path.

## Why This App Swaps the Principal After Login (Form Login Only)

Both login methods end up with a `SessionMember`-shaped principal in Redis (`SessionRedisOidcUser` for OAuth2, `SessionRedisEmailUser` for email/password) — but *when* that shape gets built differs, because of one constraint: `UserDetailsService.loadUserByUsername()` runs **before** the password is checked.

- **OAuth2** (`SessionRedisOidcUserService.loadUser()`): by the time this runs, the identity provider has already verified the user externally. It's safe to call `request.getSession(true)` right here and build the final `SessionMember` (with `httpSessionId`) directly. When `saveContext()` runs afterward, the session already exists — it just reuses it.

- **Email/password** (`MemberUserDetailsService.loadUserByUsername()`): this runs for *every* login attempt with a valid email, including ones with the wrong password. It must not create a session (that would leak a Redis session per failed attempt), so it returns a plain Spring Security `User` carrying the real password hash — needed for `DaoAuthenticationProvider` to verify it. Only if that check passes does `successfulAuthentication()` run `saveContext()`, which is what actually creates the session this time.

  `FormLoginSuccessHandler` then runs, calls `request.getSession(true)` (returns the session `saveContext()` just created — not a new one), builds a `SessionMember` from it, and replaces the principal:

  ```java
  SessionMember sessionMember = new SessionMember(
          authentication.getName(),
          authentication.getAuthorities(),
          request.getSession(true).getId());

  SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(
                  new SessionRedisEmailUser(sessionMember), null, sessionMember.getAuthorities()));
  ```

  This works without an extra `saveContext()` call because `SecurityContextHolder.getContext()` returns the **same object reference** that `saveContext()` already stored in the session's attribute map. Mutating it in place is enough — whatever ends up in that object is what `SessionRepositoryFilter` serializes to Redis at the end of the request.

## Serialization: Why the Mixins Exist

The session gets serialized to Redis as JSON (`GenericJackson2JsonRedisSerializer`, configured in `SessionRedisConfig`). `SecurityJackson2Modules.getModules(...)` already knows how to (de)serialize Spring Security's own classes (`User`, `UsernamePasswordAuthenticationToken`, `SimpleGrantedAuthority`, ...) — that's why the plain `User` principal used mid-authentication for form login needs no extra setup.

Our own custom classes (`SessionMember`, `SessionRedisOidcUser`, `SessionRedisEmailUser`) are unknown to Spring Security's Jackson modules, so each needs a `@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)` mixin registered manually, plus `@JsonAutoDetect(getterVisibility = NONE, isGetterVisibility = NONE)` on the class itself so only the explicitly `@JsonProperty`-annotated field (`sessionMember`) gets serialized — nothing else leaks into Redis.

## End-to-End Request Trace

```
Login request (email/password)
 → SessionRepositoryFilter            wraps the request in SessionRepositoryRequestWrapper
                                       (outermost filter — everything below sees this wrapped request)
 → SecurityContextHolderFilter        loads (empty/anonymous) deferred context
 → UsernamePasswordAuthenticationFilter
     → MemberUserDetailsService.loadUserByUsername()   (no session yet, password not checked)
     → DaoAuthenticationProvider checks password
     → successfulAuthentication()
         → securityContextRepository.saveContext()     <-- session created HERE
         → FormLoginSuccessHandler
             → request.getSession(true)                 (reuses the session above)
             → builds SessionMember + SessionRedisEmailUser
             → SecurityContextHolder.getContext().setAuthentication(...)  (mutates in place)
 → ... rest of the filter chain ...
 → SessionRepositoryFilter (finally)   commitSession() → Redis: HSET + EXPIRE
```

```
Later, authenticated request
 → Browser sends the cookie
 → SessionRepositoryFilter wraps the request, backed by RedisSessionRepository
 → SecurityContextHolderFilter loads the SPRING_SECURITY_CONTEXT attribute (deserialized via the mixins)
   into SecurityContextHolder for this request
 → Controller reads Authentication.getName() → member id, regardless of login method
```

## Project-Specific Notes

- Session storage and the trial/chat cache share a single Redis instance (`redis.host`/`redis.port`, one `RedisConnectionFactory` bean in `SessionRedisConfig`, reused by `TrialCacheRedisConfig`) — they used to be two separate Redis connections, merged since neither key namespace collides (`solomon:session:*` vs `servers`/`server:*`/`trial:*`).
- `spring.session.redis.namespace: solomon:session`, `timeout: 1h` (`application-local-oauth2.yml`).
- Session cookie name is explicitly set to `SOLOMON_SESSION` via `server.servlet.session.cookie.name` (`application-local-oauth2.yml`) 
- `Authentication.getName()` resolves to the member id string for **both** login methods (`AbstractAuthenticationToken.getName()` checks `UserDetails.getUsername()` first, then falls back to `AuthenticatedPrincipal.getName()` — `OidcUser`/`OAuth2User` implement the latter). This is why a controller can just take `Authentication authentication` and call `.getName()` without caring which login method was used.

## References

- `common/adapter/in/web/security/config/SessionRedisConfig.java`
- `common/adapter/in/web/security/oauth/SessionRedisOidcUserService.java`, `SessionRedisOidcUser.java`, `SessionMember.java`
- `common/adapter/in/web/security/form/MemberUserDetailsService.java`, `SessionRedisEmailUser.java`
- `common/adapter/in/web/security/handler/FormLoginSuccessHandler.java`