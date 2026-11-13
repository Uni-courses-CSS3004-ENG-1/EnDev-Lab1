package com.endev.tutoring.exception;

// A second lesson with a business key that is already taken.
// Unchecked, so a transaction that lets it escape is rolled back.
public class DuplicateLessonException extends RuntimeException {

    public DuplicateLessonException(String businessKey) {
        super("duplicate lesson: " + businessKey);
    }
}
