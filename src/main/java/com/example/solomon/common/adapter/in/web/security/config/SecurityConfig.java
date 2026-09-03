package com.example.solomon.common.adapter.in.web.security.config;

import com.example.solomon.common.adapter.in.web.security.form.FormLoginFailureHandler;
import com.example.solomon.common.adapter.in.web.security.form.FormLoginSuccessHandler;
import com.example.solomon.common.adapter.in.web.security.SessionAccessDeniedHandler;
import com.example.solomon.common.adapter.in.web.security.SessionAuthenticationEntryPoint;
import com.example.solomon.common.adapter.in.web.security.oauth.OAuth2LoginFailureHandler;
import com.example.solomon.common.adapter.in.web.security.oauth.OAuth2LoginSuccessHandler;
import com.example.solomon.common.adapter.in.web.security.oauth.SessionRedisOidcUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SessionAuthenticationEntryPoint authenticationEntryPoint;
    private final SessionAccessDeniedHandler accessDeniedHandler;
    private final SessionRegistry sessionRegistry;

    private final SessionRedisOidcUserService sessionRedisOidcUserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    private final FormLoginSuccessHandler formLoginSuccessHandler;
    private final FormLoginFailureHandler formLoginFailureHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain claudeSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(sessionRedisOidcUserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler))
                .formLogin(form -> form
                        .loginProcessingUrl("/api/member/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(formLoginSuccessHandler)
                        .failureHandler(formLoginFailureHandler))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .sessionRegistry(sessionRegistry));

        return http.build();
    }

}