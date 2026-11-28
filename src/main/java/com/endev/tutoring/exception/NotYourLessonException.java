package com.endev.tutoring.exception;

// The role is right, but the lesson belongs to another tutor or student. 403.
public class NotYourLessonException extends RuntimeException {

    public NotYourLessonException() {
        super("not your lesson");
    }
}
