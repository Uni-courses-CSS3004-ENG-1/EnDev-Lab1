package com.endev.tutoring.security;

import com.endev.tutoring.dto.ApiError;
import com.endev.tutoring.exception.CorrelationId;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// 401 and 403 are decided in the filters, before any controller, so the advice never sees them.
// This writes the same three-field ApiError the advice writes, instead of Spring's empty body.
public class SecurityErrors implements AuthenticationEntryPoint, AccessDeniedHandler {

    public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
    public static final String NO_TOKEN = "valid token required";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String WRONG_ROLE = "not allowed for your role";

    private final ObjectMapper json;

    public SecurityErrors(ObjectMapper json) {
        this.json = json;
    }

    // no header, not three pieces, wrong signature, expired, or another issuer
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(response, HttpServletResponse.SC_UNAUTHORIZED,
                new ApiError(UNAUTHENTICATED, NO_TOKEN, CorrelationId.of(request)));
    }

    // a valid token whose role the rules do not allow
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(response, HttpServletResponse.SC_FORBIDDEN,
                new ApiError(FORBIDDEN, WRONG_ROLE, CorrelationId.of(request)));
    }

    private void write(HttpServletResponse response, int status, ApiError body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        json.writeValue(response.getOutputStream(), body);
    }
}
