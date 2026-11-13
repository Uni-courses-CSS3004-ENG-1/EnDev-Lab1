package com.endev.tutoring.model;

import java.util.UUID;

// A stored lesson as the rest of the code sees it: the row's UUID, the business key, the status,
// the subject, and the optional tutor and student ids. The JPA entity stays behind the repository.
public record Lesson(UUID id, LessonId businessKey, LessonStatus status, String subject,
                     UUID tutorId, UUID studentId) {

    public Lesson {
        if (id == null) {
            throw new IllegalArgumentException("Lesson database id must not be null");
        }
        if (businessKey == null) {
            throw new IllegalArgumentException("Lesson id must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
    }

    // a lesson with no tutor or student yet
    public Lesson(UUID id, LessonId businessKey, LessonStatus status, String subject) {
        this(id, businessKey, status, subject, null, null);
    }
}
