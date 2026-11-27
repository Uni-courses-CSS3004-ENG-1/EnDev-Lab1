package com.endev.tutoring.security;

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

// All the security rules in one place. The URL rules below are the README rules table, row by row.
// @EnableMethodSecurity turns on @PreAuthorize, the second lock on LessonWorkflowService.confirm.
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    public static final String STUDENT = "STUDENT";
    public static final String TUTOR = "TUTOR";

    @Bean
    SecurityFilterChain api(HttpSecurity http, SecurityErrors errors) throws Exception {
        return http
                // Who may call what. Read top to bottom: the FIRST matching rule wins.
                .authorizeHttpRequests(rules -> rules
                        // 1. Login: anyone, no token yet
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        // 2. The auth page, by exact file name (never a pattern that would also open /api)
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/app.css", "/app.js").permitAll()
                        // 3. Health and info only; env, heapdump and the rest fall through to 8. denyAll
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        // 4. The API docs
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 5. The locked action: tutors only. It must sit ABOVE rule 7, or a student matches 7 first
                        .requestMatchers(HttpMethod.POST, "/api/v1/lessons/*/confirm").hasRole(TUTOR)
                        // 6. Who am I
                        .requestMatchers(HttpMethod.GET, "/api/me").hasAnyRole(STUDENT, TUTOR)
                        // 7. Everything else in the API: any signed-in user
                        .requestMatchers("/api/v1/**").hasAnyRole(STUDENT, TUTOR)
                        // 8. Not listed above: nobody
                        .anyRequest().denyAll())

                // How we know who is calling: a JWT in "Authorization: Bearer ...".
                // A bad token (wrong signature, expired, other issuer) goes to errors -> 401.
                .oauth2ResourceServer(server -> server
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolesFromToken()))
                        .authenticationEntryPoint(errors))

                // No token at all -> 401. Valid token, wrong role -> 403. Both as our ApiError JSON.
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))

                // The token travels in a header, never a cookie: no session, so no CSRF to protect.
                // No Spring login page and no HTTP Basic: login is our own POST /api/v1/auth/login.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .build();
    }

    // Writes the 401 and 403 bodies (see SecurityErrors).
    @Bean
    SecurityErrors securityErrors(ObjectMapper json) {
        return new SecurityErrors(json);
    }

    // The token carries "roles": ["TUTOR"], but hasRole("TUTOR") checks for "ROLE_TUTOR".
    // This converter reads the roles claim and adds the ROLE_ prefix back.
    static JwtAuthenticationConverter rolesFromToken() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }
}
