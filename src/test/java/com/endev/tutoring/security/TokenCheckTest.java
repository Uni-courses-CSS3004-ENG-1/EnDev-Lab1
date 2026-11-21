package com.endev.tutoring.security;

import com.endev.tutoring.TutoringApplication;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The real chain and the real HS256 decoder on the dev profile. Each row of the "check, in order"
// table: a header, three pieces, our signature, the time and the issuer, then the role.
@SpringBootTest(classes = TutoringApplication.class)
@AutoConfigureMockMvc
class TokenCheckTest {

    private static final String LESSON = "/api/v1/lessons/7d3f0a52-1c2b-4c55-9a51-2f9d0c1e4b6a";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SecretKey jwtKey;

    private String sign(SecretKey key, String issuer, Instant issuedAt, List<String> roles) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("aliya")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(15, ChronoUnit.MINUTES))
                .claim("roles", roles)
                .build();
        return new NimbusJwtEncoder(new ImmutableSecret<>(key))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private ResultActions getLesson(String authorization) throws Exception {
        return mvc.perform(get(LESSON).header("Authorization", authorization)
                .header("X-Correlation-Id", "check-1"));
    }

    private static void isUnauthenticated(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHENTICATED")))
                .andExpect(jsonPath("$.message", is("valid token required")))
                .andExpect(jsonPath("$.correlationId", is("check-1")))
                .andExpect(jsonPath("$.*", hasSize(3)));
    }

    @Nested
    @DisplayName("who are you: 401")
    class WhoAreYou {

        @Test
        void noHeader() throws Exception {
            mvc.perform(get(LESSON).header("X-Correlation-Id", "check-1"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate", "Bearer"))
                    .andExpect(jsonPath("$.code", is("UNAUTHENTICATED")))
                    .andExpect(jsonPath("$.message", is("valid token required")))
                    .andExpect(jsonPath("$.correlationId", is("check-1")))
                    .andExpect(jsonPath("$.*", hasSize(3)));
        }

        @Test
        void notThreePieces() throws Exception {
            isUnauthenticated(getLesson("Bearer hello"));
        }

        @Test
        void signedWithAnotherSecret() throws Exception {
            SecretKey other = JwtConfig.key("another-secret-that-is-also-32-chars-long!!");
            isUnauthenticated(getLesson("Bearer " + sign(other, "tutoring", Instant.now(), List.of("STUDENT"))));
        }

        @Test
        void expired() throws Exception {
            Instant anHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
            isUnauthenticated(getLesson("Bearer " + sign(jwtKey, "tutoring", anHourAgo, List.of("STUDENT"))));
        }

        @Test
        void issuedByAnotherProduct() throws Exception {
            isUnauthenticated(getLesson("Bearer " + sign(jwtKey, "helpdesk", Instant.now(), List.of("STUDENT"))));
        }

        @Test
        void basicIsNotTheWayIn() throws Exception {
            isUnauthenticated(getLesson("Basic YWxpeWE6c3R1ZGVudC1kZXY="));
        }
    }

    @Nested
    @DisplayName("may you do this: 403")
    class MayYou {

        @Test
        void aRoleThatIsNotOursIsForbidden() throws Exception {
            mvc.perform(get(LESSON).header("Authorization",
                            "Bearer " + sign(jwtKey, "tutoring", Instant.now(), List.of("ADMIN"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")))
                    .andExpect(jsonPath("$.message", is("not allowed for your role")))
                    .andExpect(jsonPath("$.*", hasSize(3)));
        }

        @Test
        void aPathOutsideTheTableIsForbiddenWithAGoodToken() throws Exception {
            mvc.perform(get("/api/v2/lessons").header("Authorization",
                            "Bearer " + sign(jwtKey, "tutoring", Instant.now(), List.of("TUTOR"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")));
        }
    }

    @Nested
    @DisplayName("open rows")
    class Open {

        @Test
        void aGoodTokenReachesTheController() throws Exception {
            // the lesson does not exist, so the controller answering 404 proves the chain let it through
            mvc.perform(get(LESSON).header("Authorization",
                            "Bearer " + sign(jwtKey, "tutoring", Instant.now(), List.of("STUDENT"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")));
        }

        @Test
        void healthNeedsNoToken() throws Exception {
            mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        }

        @Test
        void apiDocsNeedNoToken() throws Exception {
            mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        }
    }
}
