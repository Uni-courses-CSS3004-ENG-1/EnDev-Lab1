package com.endev.tutoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// What a successful login returns. expiresIn is in seconds: 900 is 15 minutes.
public record TokenResponse(
        @Schema(description = "Send as Authorization: Bearer <accessToken>") String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(example = "900") long expiresIn) {

    public static TokenResponse bearer(String accessToken, long expiresIn) {
        return new TokenResponse(accessToken, "Bearer", expiresIn);
    }
}
