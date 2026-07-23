# Calude do the belows

spring-session-data-redis 라이브러리로 oicd 로그인 구현해줘.

json 직렬화를 사용하고 Member엔티티를 그댁로 넣지 마록 따로 SessionMember를 만들어줘
private final String id;

private Collection<? extends GrantedAuthority> authorities;

private final String httpSessionId;

필드는 이 정도면 돼. 

oicd 관련 설정은 여기에 만들어줘 구글이랑 카카오로 할 거고 아래를 참고해. 대략 이렇게 할거야. 

Projects/solomon/src/main/resources/config/local
spring:
config:
import:
- optional:file:/workspaces/stomp/env/local/local.env[.properties]

security:
oauth2:
client:
registration:
google:
client-id: ${GOOGLE_CLIENT_ID}
client-secret: ${GOOGLE_CLIENT_SECRET}

            redirect-uri: "https://humble-goggles-v6gv597j9qw9fjr4-8080.app.github.dev/login/oauth2/code/google"

          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}

            redirect-uri: "https://humble-goggles-v6gv597j9qw9fjr4-8080.app.github.dev/login/oauth2/code/kakao"

            authorization-grant-type: authorization_code
            client-authentication-method: client_secret_post
            
            scope:
              - openid       
              - account_email
              - profile_image

        provider:
          kakao:
            issuer-uri: https://kauth.kakao.com

로그인 성공 후 SuccesHandler는 생성만하고 일단 비워둬.
EntiryPoint랑 Failurehander, AccessDinered 핸들러는 slf4j로 왜 예외가 났는지 상세하게 로그를 남겨. 그냥 에러가 났습니다. 말고 
어디서(처음 예외가 발생된 곳(클래스)), 메시지를 남겨줘. 다른 작업은 안 해도 돼. 401 응답을 보낸다던가. 이런 거는 필요 없어 . 내가 할 게.

그리고 네가 하는 작업은 Projects/solomon/src/main/java/com/example/solomon/claude/ 이 밑에 경로에 다 해. 내가 나중에 패키지 구조 다시 짤게
지금 패키기 구조 확정이 안 돼서 그래. 하다가 애매한 점 있으면 나한테 말 하고 