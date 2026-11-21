package com.endev.tutoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// Who the checked token says is calling. The page shows the role from here, never from a body.
public record MeResponse(
        @Schema(example = "aliya") String username,
        @Schema(example = "[\"ROLE_STUDENT\"]") List<String> authorities) {
}
