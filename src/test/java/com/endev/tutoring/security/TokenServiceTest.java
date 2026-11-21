package com.endev.tutoring.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// No web layer and no mocks: TokenService signs a real token, a decoder with the same secret reads it.
class TokenServiceTest {

    private static final String SECRET = "test-secret-at-least-32-bytes-long!!";
    private static final SecretKey KEY = JwtConfig.key(SECRET);

    private final TokenService tokens = new TokenService(new NimbusJwtEncoder(new ImmutableSecret<>(KEY)),
            new JwtProperties(SECRET, "tutoring"));

    private String issueForAliya() {
        return tokens.issue(UsernamePasswordAuthenticationToken.authenticated(
                "aliya", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
    }

    @Test
    void issuedTokenCarriesUserRoleIssuerAndFifteenMinutes() {
        Jwt jwt = JwtConfig.decoder(KEY, "tutoring").decode(issueForAliya());

        assertAll(
                () -> assertEquals("aliya", jwt.getSubject()),
                () -> assertEquals(List.of("STUDENT"), jwt.getClaimAsStringList("roles")),
                () -> assertEquals("tutoring", jwt.getClaimAsString("iss")),
                () -> assertEquals("HS256", jwt.getHeaders().get("alg").toString()),
                () -> assertTrue(jwt.getExpiresAt().isAfter(Instant.now())),
                () -> assertEquals(Duration.ofMinutes(15),
                        Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())));
    }

    @Test
    void payloadHoldsNoPassword() {
        String payload = new String(Base64.getUrlDecoder().decode(issueForAliya().split("\\.")[1]), UTF_8);
        assertFalse(payload.contains("student-dev"), payload);
        assertFalse(payload.contains("password"), payload);
    }

    @Test
    void anotherProductsDecoderRejectsIt() {
        JwtDecoder helpdesk = JwtConfig.decoder(KEY, "helpdesk");
        assertThrows(JwtException.class, () -> helpdesk.decode(issueForAliya()));
    }

    @Test
    void anotherSecretRejectsIt() {
        JwtDecoder other = JwtConfig.decoder(JwtConfig.key("a-completely-different-secret-of-32+"), "tutoring");
        assertThrows(JwtException.class, () -> other.decode(issueForAliya()));
    }
}
