package com.endev.tutoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

// A student books a tutor. There is no studentId field: the student is the caller's own account.
public record BookLessonRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID tutorId,
        @NotBlank @Size(max = 120)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Algebra") String subject,
        @NotNull @Future
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "2030-01-15T16:00:00+05:00")
        OffsetDateTime startsAt) {
}
