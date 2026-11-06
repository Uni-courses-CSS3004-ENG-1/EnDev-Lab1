package com.endev.tutoring.handler;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// The POST body: the business key and the subject, nothing the server decides.
// The constraints live here in the web package; the domain has no jakarta.validation.
public record CreateLessonRequest(
        @NotBlank String businessKey,
        @NotBlank @Size(max = 120) String subject) {
}
