package com.endev.tutoring.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

// How login checks a name and a password: find the user, then compare the typed password
// with the stored BCrypt hash. AuthController calls this; nothing else sees a password.
@Configuration
public class AuthConfig {

    // BCrypt. A stored password looks like {bcrypt}$2a$10$..., never the plain text.
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // The users are the app_user rows, read by AccountUsers.
    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder passwordEncoder) {
        return manager(users, passwordEncoder);
    }

    // Also used by AccountUsersTest, which checks login without starting Spring.
    static AuthenticationManager manager(UserDetailsService users, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder);
        provider.setUserDetailsService(users);
        return new ProviderManager(provider);
    }
}
