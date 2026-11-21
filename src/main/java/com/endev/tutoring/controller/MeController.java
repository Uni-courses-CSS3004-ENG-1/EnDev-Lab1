package com.endev.tutoring.controller;

import com.endev.tutoring.dto.MeResponse;
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
    public MeResponse me(Authentication authentication) {
        return new MeResponse(authentication.getName(), authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList());
    }
}
