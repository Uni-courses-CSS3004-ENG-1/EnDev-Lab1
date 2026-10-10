package com.endev.tutoring.domain;

// A second lesson with a business key that is already taken.
// Unchecked, so a transaction that lets it escape is rolled back.
public class DuplicateLesson extends RuntimeException {

    public DuplicateLesson(String businessKey) {
        super("duplicate lesson: " + businessKey);
    }
}
