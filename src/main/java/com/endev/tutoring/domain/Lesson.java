package com.endev.tutoring.domain;

import java.util.UUID;

// A stored lesson as the rest of the code sees it: the row's UUID, the business key,
// the status and the subject. The JPA entity stays in persistence; this is what leaves a service.
public record Lesson(UUID id, LessonId businessKey, LessonStatus status, String subject) {

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
}
