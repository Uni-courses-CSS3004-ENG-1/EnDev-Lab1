package com.endev.tutoring.domain;

// Thrown when a lesson number is registered twice.
// Unchecked on purpose: Spring only rolls back a transaction for unchecked exceptions.
public class DuplicateLesson extends RuntimeException {

    public DuplicateLesson(LessonId number) {
        super("duplicate lesson: " + number.value());
    }
}
