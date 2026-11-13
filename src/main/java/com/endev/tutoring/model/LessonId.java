package com.endev.tutoring.model;

public record LessonId(String value) {

    public LessonId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Lesson id must not be empty");
        }
    }
}
