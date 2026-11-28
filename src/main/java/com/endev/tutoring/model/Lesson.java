package com.endev.tutoring.model;

import java.time.OffsetDateTime;
import java.util.UUID;

// A stored lesson as the rest of the code sees it: the row's UUID, the business key, the status,
// the subject, the optional tutor and student ids, and the optional start time (V6).
// The JPA entity stays behind the repository.
public record Lesson(UUID id, LessonId businessKey, LessonStatus status, String subject,
                     UUID tutorId, UUID studentId, OffsetDateTime startsAt) {

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

    // a lesson with no start time yet
    public Lesson(UUID id, LessonId businessKey, LessonStatus status, String subject,
                  UUID tutorId, UUID studentId) {
        this(id, businessKey, status, subject, tutorId, studentId, null);
    }

    // a lesson with no tutor or student yet
    public Lesson(UUID id, LessonId businessKey, LessonStatus status, String subject) {
        this(id, businessKey, status, subject, null, null, null);
    }
}
