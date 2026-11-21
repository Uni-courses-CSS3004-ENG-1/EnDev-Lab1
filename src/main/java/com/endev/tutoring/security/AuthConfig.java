package com.endev.tutoring.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

// "Check this name and this password": find the user, compare the typed password with the hash.
@Configuration
public class AuthConfig {

    // BCrypt by default; the stored value starts with {bcrypt}, never the plain-text prefix
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // The users come from the dev profile on a laptop. Prod ships none yet, so nobody can log in
    // there until a real user store exists, instead of the app failing to start.
    @Bean
    AuthenticationManager authenticationManager(ObjectProvider<UserDetailsService> users,
                                                PasswordEncoder passwordEncoder) {
        return manager(users.getIfAvailable(InMemoryUserDetailsManager::new), passwordEncoder);
    }

    static AuthenticationManager manager(UserDetailsService users, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder);
        provider.setUserDetailsService(users);
        return new ProviderManager(provider);
    }
}
