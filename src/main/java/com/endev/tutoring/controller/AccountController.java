package com.endev.tutoring.controller;

import com.endev.tutoring.dto.AccountResponse;
import com.endev.tutoring.dto.ApiError;
import com.endev.tutoring.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// The signed-in person's profile. GET /api/me stays the Lab 5 answer: name and authorities only.
@RestController
@Tag(name = "Auth", description = "Sign up or log in, and get a 15-minute Bearer token")
public class AccountController {

    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping(path = "/api/v1/account", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "My profile: name, email, role, and a tutor's subject and price")
    @ApiResponse(responseCode = "200", description = "The caller's own profile")
    @ApiResponse(responseCode = "401", description = "valid token required",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "The token's user has no account",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public AccountResponse me(Authentication authentication) {
        return AccountResponse.from(accounts.profile(authentication.getName()));
    }
}
