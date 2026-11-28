package com.endev.tutoring.model;

import java.math.BigDecimal;
import java.util.UUID;

// A tutor as the rest of the code sees it. bio is the short line on the tutor card (V5).
public record Tutor(UUID id, String name, String email, String subject, BigDecimal hourlyRate, String bio) {

    // a tutor with no bio yet
    public Tutor(UUID id, String name, String email, String subject, BigDecimal hourlyRate) {
        this(id, name, email, subject, hourlyRate, "");
    }
}
