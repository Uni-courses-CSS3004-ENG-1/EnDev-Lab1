package com.endev.tutoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

// The POST body. tutorId and studentId are optional, so old clients that send only
// the business key and subject still work on v1.
public record CreateLessonRequest(
        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "LES-2026-0042") String businessKey,
        @NotBlank @Size(max = 120)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Algebra") String subject,
        @Schema(description = "Optional: an existing tutor's UUID") UUID tutorId,
        @Schema(description = "Optional: an existing student's UUID") UUID studentId) {
}
