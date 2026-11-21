package com.endev.tutoring.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

// The README rules table, top to bottom. The first matching rule wins, so the narrow confirm
// rule sits above the wide /api/v1/** one, and anything not listed is denied.
@Configuration
public class SecurityConfig {

    public static final String STUDENT = "STUDENT";
    public static final String TUTOR = "TUTOR";

    @Bean
    SecurityFilterChain api(HttpSecurity http, SecurityErrors errors) throws Exception {
        return http
                .authorizeHttpRequests(rules -> rules
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        // the page itself, by exact name; never a pattern that also covers /api
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/app.css", "/app.js").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/lessons/*/confirm").hasRole(TUTOR)
                        .requestMatchers(HttpMethod.GET, "/api/me").hasAnyRole(STUDENT, TUTOR)
                        .requestMatchers("/api/v1/**").hasAnyRole(STUDENT, TUTOR)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(server -> server
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolesFromToken()))
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                // the credential is a Bearer header, never a cookie, so there is no session and no CSRF
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

    // The token says "roles": ["TUTOR"]; hasRole("TUTOR") looks for ROLE_TUTOR.
    static JwtAuthenticationConverter rolesFromToken() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }
}
