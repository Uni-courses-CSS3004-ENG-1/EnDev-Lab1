package com.endev.tutoring.dto;

import com.endev.tutoring.model.Tutor;

import java.math.BigDecimal;
import java.util.UUID;

public record TutorResponse(UUID id, String name, String email, String subject, BigDecimal hourlyRate) {

    public static TutorResponse from(Tutor tutor) {
        return new TutorResponse(tutor.id(), tutor.name(), tutor.email(), tutor.subject(), tutor.hourlyRate());
    }
}
