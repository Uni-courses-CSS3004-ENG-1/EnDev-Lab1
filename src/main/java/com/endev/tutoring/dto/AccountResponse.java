package com.endev.tutoring.dto;

import com.endev.tutoring.model.Profile;

import java.math.BigDecimal;
import java.util.UUID;

// The signed-in person's own profile. Never the password hash.
public record AccountResponse(String username, String role, String name, String email,
                              UUID studentId, UUID tutorId, String subject, BigDecimal hourlyRate, String bio) {

    public static AccountResponse from(Profile profile) {
        var account = profile.account();
        var tutor = profile.tutor();
        return new AccountResponse(account.username(), account.role(), profile.name(), profile.email(),
                account.studentId(), account.tutorId(),
                tutor == null ? null : tutor.subject(),
                tutor == null ? null : tutor.hourlyRate(),
                tutor == null ? null : tutor.bio());
    }
}
