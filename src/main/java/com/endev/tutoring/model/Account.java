package com.endev.tutoring.model;

import java.util.UUID;

// A login as the rest of the code sees it. No password here: the hash stays in the entity.
// A STUDENT account has a studentId, a TUTOR account has a tutorId.
public record Account(UUID id, String username, String role, UUID studentId, UUID tutorId) {

    public static final String STUDENT = "STUDENT";
    public static final String TUTOR = "TUTOR";

    public boolean isTutor() {
        return TUTOR.equals(role);
    }
}
