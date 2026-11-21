package com.endev.tutoring.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// No Spring context: the same beans the dev profile builds, checked by hand.
class DevUsersTest {

    private final PasswordEncoder encoder = new AuthConfig().passwordEncoder();
    private final UserDetailsService users = new DevUsersConfig().devUsers(encoder);
    private final AuthenticationManager manager = AuthConfig.manager(users, encoder);

    @Nested
    @DisplayName("stored users")
    class Stored {

        @Test
        void passwordsAreBcryptHashesNotText() {
            UserDetails aliya = users.loadUserByUsername("aliya");
            assertAll(
                    () -> assertTrue(aliya.getPassword().startsWith("{bcrypt}$2"), aliya.getPassword()),
                    () -> assertFalse(aliya.getPassword().contains("student-dev")),
                    () -> assertTrue(encoder.matches("student-dev", aliya.getPassword())));
        }

        @Test
        void twoHashesOfOnePasswordDifferButBothMatch() {
            String first = encoder.encode("tutor-dev");
            String second = encoder.encode("tutor-dev");
            assertAll(
                    () -> assertNotEquals(first, second),
                    () -> assertTrue(encoder.matches("tutor-dev", first)),
                    () -> assertTrue(encoder.matches("tutor-dev", second)));
        }

        @Test
        void rolesCarryThePrefixHasRoleLooksFor() {
            assertAll(
                    () -> assertEquals("ROLE_STUDENT", onlyAuthority(users.loadUserByUsername("aliya"))),
                    () -> assertEquals("ROLE_TUTOR", onlyAuthority(users.loadUserByUsername("dana"))));
        }
    }

    @Nested
    @DisplayName("authentication manager")
    class Manager {

        @Test
        void rightPasswordAuthenticates() {
            Authentication dana = manager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated("dana", "tutor-dev"));
            assertAll(
                    () -> assertTrue(dana.isAuthenticated()),
                    () -> assertEquals("dana", dana.getName()));
        }

        // the same exception for both, so the login answer cannot tell an attacker which names exist
        @Test
        void wrongPasswordAndUnknownUserFailTheSameWay() {
            assertAll(
                    () -> assertThrows(BadCredentialsException.class, () -> manager.authenticate(
                            UsernamePasswordAuthenticationToken.unauthenticated("aliya", "wrong"))),
                    () -> assertThrows(BadCredentialsException.class, () -> manager.authenticate(
                            UsernamePasswordAuthenticationToken.unauthenticated("nobody", "student-dev"))));
        }
    }

    private static String onlyAuthority(UserDetails user) {
        return user.getAuthorities().stream().map(GrantedAuthority::getAuthority).reduce((a, b) -> a + "," + b)
                .orElse("");
    }
}
