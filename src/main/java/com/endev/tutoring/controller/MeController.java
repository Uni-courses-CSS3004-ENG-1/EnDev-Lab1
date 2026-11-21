package com.endev.tutoring.controller;

import com.endev.tutoring.dto.ApiError;
import com.endev.tutoring.dto.MeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Reads the Authentication the filter put into the SecurityContext for this request.
@RestController
@Tag(name = "Auth", description = "Log in once and get a 15-minute Bearer token")
public class MeController {

    @GetMapping(path = "/api/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Who the token says is calling")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Username and authorities from the token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeResponse.class))),
            @ApiResponse(responseCode = "401", description = "valid token required",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    public MeResponse me(Authentication authentication) {
        return new MeResponse(authentication.getName(), authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList());
    }
}
