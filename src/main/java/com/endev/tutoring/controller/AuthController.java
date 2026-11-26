package com.endev.tutoring.controller;

import com.endev.tutoring.dto.ApiError;
import com.endev.tutoring.dto.LoginRequest;
import com.endev.tutoring.dto.TokenResponse;
import com.endev.tutoring.exception.CorrelationId;
import com.endev.tutoring.security.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// The only call that sees a password. A wrong name or password throws BadCredentialsException,
// which the advice turns into one 401 for both, so nobody learns which names exist.
@RestController
@RequestMapping(path = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Auth", description = "Log in once and get a 15-minute Bearer token")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authentication;
    private final TokenService tokens;

    public AuthController(AuthenticationManager authentication, TokenService tokens) {
        this.authentication = authentication;
        this.tokens = tokens;
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Log in with a username and password", security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token issued",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "username or password blank",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "bad username or password",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    public TokenResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        Authentication user;
        try {
            user = authentication.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));
        } catch (AuthenticationException refused) {
            // the typed name and the correlation id, never the password; the advice still answers 401
            log.warn("login refused for {} [{}]", forLog(body.username()), CorrelationId.of(request));
            throw refused;
        }
        log.info("login: token issued for {}", user.getName());
        return TokenResponse.bearer(tokens.issue(user), TokenService.LIFETIME.toSeconds());
    }

    private static final int MAX_LOGGED_NAME = 64;

    // The name is whatever the caller typed: one line, no control characters, not too long,
    // so a name with a line break cannot forge a second log line.
    static String forLog(String typed) {
        String oneLine = typed.replaceAll("\\p{Cntrl}", "_");
        return oneLine.length() > MAX_LOGGED_NAME ? oneLine.substring(0, MAX_LOGGED_NAME) + "…" : oneLine;
    }
}
