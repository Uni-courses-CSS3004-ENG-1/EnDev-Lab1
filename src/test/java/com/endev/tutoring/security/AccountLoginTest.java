package com.endev.tutoring.security;

import com.endev.tutoring.TutoringApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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

// TestAccountsConfig registers the test accounts into app_user (test schema). This reads them back
// from PostgreSQL: hashes, roles, and one answer for a wrong password and an unknown name.
@SpringBootTest(classes = TutoringApplication.class)
class AccountLoginTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private UserDetailsService users;

    @Autowired
    private AuthenticationManager manager;

    private String storedHash(String username) {
        return jdbc.queryForObject("SELECT password_hash FROM app_user WHERE username = ?", String.class, username);
    }

    @Nested
    @DisplayName("stored users")
    class Stored {

        @Test
        void passwordsAreBcryptHashesNotText() {
            String aliya = storedHash("aliya");
            assertAll(
                    () -> assertTrue(aliya.startsWith("{bcrypt}$2"), aliya),
                    () -> assertFalse(aliya.contains("student-dev")),
                    () -> assertTrue(encoder.matches("student-dev", aliya)));
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

        @Test
        void eachAccountPointsAtItsProfileRow() {
            assertAll(
                    () -> assertEquals("Aliya Nurlanova", jdbc.queryForObject("""
                            SELECT s.name FROM app_user u JOIN student s ON s.id = u.student_id
                            WHERE u.username = 'aliya'""", String.class)),
                    () -> assertEquals("Algebra", jdbc.queryForObject("""
                            SELECT t.subject FROM app_user u JOIN tutor t ON t.id = u.tutor_id
                            WHERE u.username = 'dana'""", String.class)));
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
