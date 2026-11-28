package com.endev.tutoring.dto;

import com.endev.tutoring.model.TutorCard;

import java.math.BigDecimal;
import java.util.UUID;

// A tutor in the public catalogue. No email: anyone can read this list without a token.
public record TutorCardResponse(UUID id, String name, String subject, BigDecimal hourlyRate, String bio,
                                long lessonsTaught) {

    public static TutorCardResponse from(TutorCard card) {
        var tutor = card.tutor();
        return new TutorCardResponse(tutor.id(), tutor.name(), tutor.subject(), tutor.hourlyRate(), tutor.bio(),
                card.lessonsTaught());
    }
}
