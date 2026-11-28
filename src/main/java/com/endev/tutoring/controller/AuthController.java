package com.endev.tutoring.controller;

import com.endev.tutoring.dto.ApiError;
import com.endev.tutoring.dto.LoginRequest;
import com.endev.tutoring.dto.RegisterRequest;
import com.endev.tutoring.dto.TokenResponse;
import com.endev.tutoring.exception.CorrelationId;
import com.endev.tutoring.exception.SessionExpiredException;
import com.endev.tutoring.model.Account;
import com.endev.tutoring.model.SignUp;
import com.endev.tutoring.security.TokenService;
import com.endev.tutoring.service.AccountService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Duration;

// Sign up, log in, stay logged in, log out.
//
// Two tokens:
//  - the access token (15 minutes) is in the JSON body. The page keeps it in memory and sends it as
//    "Authorization: Bearer". It is the only thing the API accepts.
//  - the refresh token (14 days) is an httpOnly cookie that only /api/v1/auth sees. JavaScript cannot
//    read it. POST /refresh trades it for a new access token, so a page reload does not log you out.
//
// A wrong name or password throws BadCredentialsException, which the advice turns into one 401 for
// both, so nobody learns which names exist.
@RestController
@RequestMapping(path = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Auth", description = "Sign up, log in, stay logged in with a refresh cookie, log out")
public class AuthController {

    static final String REFRESH_COOKIE = "refresh_token";

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authentication;
    private final TokenService tokens;
    private final AccountService accounts;
    private final UserDetailsService users;
    // true everywhere except the laptop (http://localhost): the browser then sends the cookie over HTTPS only
    private final boolean secureCookie;

    public AuthController(AuthenticationManager authentication, TokenService tokens, AccountService accounts,
                          UserDetailsService users, @Value("${app.auth.secure-cookie:true}") boolean secureCookie) {
        this.authentication = authentication;
        this.tokens = tokens;
        this.accounts = accounts;
        this.users = users;
        this.secureCookie = secureCookie;
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Log in with a username and password", security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token issued; the refresh cookie is set",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "username or password blank",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "bad username or password",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        Authentication user = checkPassword(body, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(user.getName()).toString())
                .body(accessToken(user));
    }

    // Sign up, then log in with the same name and password, so a new account gets exactly what a
    // returning one gets.
    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a student or tutor account and get a token", security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created, token issued, refresh cookie set",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "A field is missing or invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Username or email already taken",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest body,
                                                  HttpServletRequest request) {
        Account account = accounts.register(new SignUp(body.username(), body.password(), body.role(), body.name(),
                body.email(), body.subject(), body.hourlyRate(), body.bio()));
        log.info("sign-up: account {} created as {}", account.username(), account.role());
        Authentication user = checkPassword(new LoginRequest(account.username(), body.password()), request);
        return ResponseEntity.created(URI.create("/api/v1/account"))
                .header(HttpHeaders.SET_COOKIE, refreshCookie(user.getName()).toString())
                .body(accessToken(user));
    }

    // The refresh cookie -> a new access token (and a new cookie). The roles are read from the account
    // again, so a deleted account or a changed role takes effect here.
    @PostMapping("/refresh")
    @Operation(summary = "Trade the refresh cookie for a new access token", security = {})
    @ApiResponse(responseCode = "200", description = "New access token; the cookie is renewed")
    @ApiResponse(responseCode = "401", description = "No cookie, or it is expired or forged",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        String username = refreshToken == null ? null : tokens.refreshSubject(refreshToken).orElse(null);
        if (username == null) {
            throw new SessionExpiredException();
        }
        UserDetails account;
        try {
            account = users.loadUserByUsername(username);
        } catch (UsernameNotFoundException gone) {
            throw new SessionExpiredException();
        }
        Authentication user = UsernamePasswordAuthenticationToken.authenticated(
                account.getUsername(), null, account.getAuthorities());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(user.getName()).toString())
                .body(accessToken(user));
    }

    // Removes the refresh cookie. The page also forgets its access token, which expires within 15 minutes.
    @PostMapping("/logout")
    @Operation(summary = "Log out: remove the refresh cookie", security = {})
    @ApiResponse(responseCode = "204", description = "Cookie removed")
    public ResponseEntity<Void> logout() {
        ResponseCookie gone = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true).secure(secureCookie).sameSite("Strict").path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, gone.toString()).build();
    }

    private Authentication checkPassword(LoginRequest body, HttpServletRequest request) {
        try {
            Authentication user = authentication.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));
            log.info("login: token issued for {}", user.getName());
            return user;
        } catch (AuthenticationException refused) {
            // the typed name and the correlation id, never the password; the advice still answers 401
            log.warn("login refused for {} [{}]", forLog(body.username()), CorrelationId.of(request));
            throw refused;
        }
    }

    private TokenResponse accessToken(Authentication user) {
        return TokenResponse.bearer(tokens.issue(user), TokenService.LIFETIME.toSeconds());
    }

    // httpOnly: JavaScript cannot read it. SameSite=Strict: another site cannot make the browser send it.
    // Path /api/v1/auth: the rest of the API never receives it.
    private ResponseCookie refreshCookie(String username) {
        String value = tokens.issueRefresh(username);
        return ResponseCookie.from(REFRESH_COOKIE, value == null ? "" : value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(TokenService.REFRESH_LIFETIME)
                .build();
    }

    private static final int MAX_LOGGED_NAME = 64;

    // The name is whatever the caller typed: one line, no control characters, not too long,
    // so a name with a line break cannot forge a second log line.
    static String forLog(String typed) {
        if (typed == null) {
            return "";
        }
        String oneLine = typed.replaceAll("\\p{Cntrl}", "_");
        return oneLine.length() > MAX_LOGGED_NAME ? oneLine.substring(0, MAX_LOGGED_NAME) + "…" : oneLine;
    }
}
