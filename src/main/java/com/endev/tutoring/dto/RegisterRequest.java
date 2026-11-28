package com.endev.tutoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// The sign-up body. role is read once, here, to pick the kind of account; after that every call
// takes the role from the signed token, never from JSON. subject, hourlyRate and bio are for tutors.
public record RegisterRequest(
        // optional: without it, the email is the username and the login
        @Pattern(regexp = "[a-z0-9._-]{3,32}",
                message = "must be 3-32 characters: lowercase letters, digits, dot, dash or underscore")
        @Schema(description = "Optional; the email is used when it is left out", example = "madina") String username,
        // BCrypt only reads the first 72 bytes, so a longer password would be silently cut
        @NotBlank @Size(min = 8, max = 72)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "correct-horse", format = "password")
        String password,
        @NotBlank @Pattern(regexp = "STUDENT|TUTOR", message = "must be STUDENT or TUTOR")
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "STUDENT") String role,
        @NotBlank @Size(max = 120)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Madina Sultan") String name,
        @NotBlank @Email @Size(max = 160)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "madina@example.com") String email,
        @Size(max = 120) @Schema(description = "Tutors: what you teach", example = "Biology") String subject,
        @Positive @Digits(integer = 8, fraction = 2)
        @Schema(description = "Tutors: price of one hour", example = "20.00") BigDecimal hourlyRate,
        @Size(max = 300) @Schema(description = "Tutors: one or two lines about you") String bio) {

    // the record's default toString would put the password into any log line that prints it
    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", role=" + role + ", password=****]";
    }
}
