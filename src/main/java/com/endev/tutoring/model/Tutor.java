package com.endev.tutoring.model;

import java.math.BigDecimal;
import java.util.UUID;

// A tutor as the rest of the code sees it.
public record Tutor(UUID id, String name, String email, String subject, BigDecimal hourlyRate) {
}
