package com.endev.tutoring.controller;

import com.endev.tutoring.TutoringApplication;
import com.endev.tutoring.security.SecurityConfig;
import com.endev.tutoring.security.TokenService;
import com.endev.tutoring.service.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Login behind the real chain, with no token: the one call that needs none.
@ExtendWith(OutputCaptureExtension.class)
@WebMvcTest(AuthController.class)
@ContextConfiguration(classes = TutoringApplication.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthenticationManager authentication;

    @MockitoBean
    private TokenService tokens;

    // sign-up's service; the login tests never reach it
    @MockitoBean
    private AccountService accounts;

    // refresh's account lookup; the login tests never reach it
    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService users;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private ResultActions login(String body) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                .header("X-Correlation-Id", "login-1").content(body));
    }

    private void loginIs(String username, String password) {
        when(authentication.authenticate(argThat(attempt -> attempt != null
                && username.equals(attempt.getName()) && password.equals(attempt.getCredentials()))))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated(username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
    }

    @Nested
    @DisplayName("good login")
    class Good {

        @Test
        void returnsABearerTokenForFifteenMinutes() throws Exception {
            loginIs("aliya", "student-dev");
            when(tokens.issue(any(Authentication.class))).thenReturn("header.payload.signature");

            login("{\"username\":\"aliya\",\"password\":\"student-dev\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", is("header.payload.signature")))
                    .andExpect(jsonPath("$.tokenType", is("Bearer")))
                    .andExpect(jsonPath("$.expiresIn", is(900)))
                    .andExpect(jsonPath("$.*", hasSize(3)))
                    .andExpect(content().string(not(containsString("student-dev"))));
        }
    }

    @Nested
    @DisplayName("bad login")
    class Bad {

        @Test
        void unknownUserIs401() throws Exception {
            when(authentication.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            login("{\"username\":\"nobody\",\"password\":\"student-dev\"}")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHENTICATED")))
                    .andExpect(jsonPath("$.message", is("bad username or password")))
                    .andExpect(jsonPath("$.correlationId", is("login-1")))
                    .andExpect(jsonPath("$.*", hasSize(3)));
            verify(tokens, never()).issue(any());
        }

        @Test
        void wrongPasswordIsTheSame401() throws Exception {
            when(authentication.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            login("{\"username\":\"aliya\",\"password\":\"wrong\"}")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHENTICATED")))
                    .andExpect(jsonPath("$.message", is("bad username or password")));
            verify(tokens, never()).issue(any());
        }

        @Test
        void aRefusedLoginIsLoggedWithTheNameAndNeverThePassword(CapturedOutput output) throws Exception {
            when(authentication.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            login("{\"username\":\"aliya\",\"password\":\"typo-s3cret\"}")
                    .andExpect(status().isUnauthorized());

            assertTrue(output.getOut().contains("login refused for aliya [login-1]"), "no audit line");
            assertFalse(output.getAll().contains("typo-s3cret"), "the password reached the log");
        }

        @Test
        void aNameWithALineBreakCannotForgeASecondLogLine(CapturedOutput output) throws Exception {
            when(authentication.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            login("{\"username\":\"aliya\\nlogin: token issued for dana\",\"password\":\"x\"}")
                    .andExpect(status().isUnauthorized());

            assertTrue(output.getOut().contains("login refused for aliya_login: token issued for dana"));
            assertFalse(output.getAll().contains("\nlogin: token issued for dana"), "a forged log line");
        }

        @Test
        void withoutAHeaderTheLogAndTheBodyShareOneMintedCorrelationId(CapturedOutput output)
                throws Exception {
            when(authentication.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            String body = mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                            .content("{\"username\":\"aliya\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnauthorized())
                    .andReturn().getResponse().getContentAsString();
            String minted = body.replaceAll(".*\"correlationId\":\"([^\"]+)\".*", "$1");

            assertTrue(output.getOut().contains("login refused for aliya [" + minted + "]"),
                    "log and body show different ids");
        }

        @Test
        void aVeryLongNameIsCutInTheLog() {
            String logged = AuthController.forLog("a".repeat(500));

            assertEquals(65, logged.length());
            assertTrue(logged.endsWith("…"));
        }

        @Test
        void blankUsernameIs400AndNeverChecksAPassword() throws Exception {
            login("{\"username\":\" \",\"password\":\"student-dev\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION")))
                    .andExpect(jsonPath("$.message", is("username must not be blank")));
            verify(authentication, never()).authenticate(any());
        }

        @Test
        void blankPasswordIs400() throws Exception {
            login("{\"username\":\"aliya\",\"password\":\"\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION")))
                    .andExpect(jsonPath("$.message", is("password must not be blank")));
            verify(authentication, never()).authenticate(any());
        }
    }
}
