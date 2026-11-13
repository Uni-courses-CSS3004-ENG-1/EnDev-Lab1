package com.endev.tutoring.exception;

// A tutor or student with this email already exists. GlobalExceptionHandler turns it into 409.
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("email already exists: " + email);
    }
}
