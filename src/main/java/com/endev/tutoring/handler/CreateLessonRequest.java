package com.endev.tutoring.handler;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// The POST body: the business key and subject, nothing the server decides.
public record CreateLessonRequest(
        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "LES-2026-0042") String businessKey,
        @NotBlank @Size(max = 120)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Algebra") String subject) {
}