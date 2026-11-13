package com.endev.tutoring.exception;

import java.util.UUID;

// No tutor row has this UUID. GlobalExceptionHandler turns it into 404.
public class TutorNotFoundException extends RuntimeException {

    public TutorNotFoundException(UUID id) {
        super("tutor not found: " + id);
    }
}
