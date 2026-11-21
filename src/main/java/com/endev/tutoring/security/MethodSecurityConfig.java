package com.endev.tutoring.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

// Turns @PreAuthorize on. The second lock: even a controller that forgets the URL rule
// cannot reach a TUTOR-only service method with a student's token.
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
