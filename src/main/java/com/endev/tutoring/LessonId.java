package com.endev.tutoring;

public record LessonId(String value) {

    public LessonId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Lesson id must not be null or blank");
        }
    }
}
