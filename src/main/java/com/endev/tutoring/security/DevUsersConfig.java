package com.endev.tutoring.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

// Two classroom users that exist only on the laptop. No user table this lab.
// The passwords are encoded at start-up, so the manager only ever holds BCrypt hashes.
@Configuration
@Profile("dev")
public class DevUsersConfig {

    @Bean
    UserDetailsService devUsers(PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(
                // roles(...) stores ROLE_STUDENT and ROLE_TUTOR, which hasRole(...) looks for
                User.withUsername("aliya")
                        .password(passwordEncoder.encode("student-dev"))
                        .roles(SecurityConfig.STUDENT)
                        .build(),
                User.withUsername("dana")
                        .password(passwordEncoder.encode("tutor-dev"))
                        .roles(SecurityConfig.TUTOR)
                        .build());
    }
}
