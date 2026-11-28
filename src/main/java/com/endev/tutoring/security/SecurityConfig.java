package com.endev.tutoring.security;

import com.endev.tutoring.model.Account;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    public static final String STUDENT = Account.STUDENT;
    public static final String TUTOR = Account.TUTOR;

    @Bean
    SecurityFilterChain api(HttpSecurity http, SecurityErrors errors) throws Exception {
        return http
                // Who may call what. Read top to bottom: the FIRST matching rule wins.
                .authorizeHttpRequests(rules -> rules
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/register",
                                "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/app.css", "/app.js").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tutors").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/lessons/*/confirm", "/api/v1/lessons/*/complete")
                        .hasRole(TUTOR)
                        .requestMatchers(HttpMethod.POST, "/api/v1/lessons/book").hasRole(STUDENT)
                        .requestMatchers(HttpMethod.GET, "/api/me").hasAnyRole(STUDENT, TUTOR)
                        .requestMatchers("/api/v1/**").hasAnyRole(STUDENT, TUTOR)
                        .anyRequest().denyAll())

                .oauth2ResourceServer(server -> server
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolesFromToken()))
                        .authenticationEntryPoint(errors))

                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))

                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    SecurityErrors securityErrors(ObjectMapper json) {
        return new SecurityErrors(json);
    }

    static JwtAuthenticationConverter rolesFromToken() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }
}
