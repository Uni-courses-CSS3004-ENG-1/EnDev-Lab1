package com.endev.tutoring.exception;

import java.util.UUID;

// No student row has this UUID. GlobalExceptionHandler turns it into 404.
public class StudentNotFoundException extends RuntimeException {

    public StudentNotFoundException(UUID id) {
        super("student not found: " + id);
    }
}
