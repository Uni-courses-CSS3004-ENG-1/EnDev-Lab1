package com.endev.tutoring.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

// MockMvc stand-ins for the two dev users' tokens. Nothing is signed: jwt() puts an already
// checked token into the request. A call without one of these is anonymous.
public final class TestTokens {

    private TestTokens() {
    }

    public static JwtRequestPostProcessor student() {
        return jwt().jwt(token -> token.subject("aliya"))
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"));
    }

    public static JwtRequestPostProcessor tutor() {
        return jwt().jwt(token -> token.subject("dana"))
                .authorities(new SimpleGrantedAuthority("ROLE_TUTOR"));
    }
}
