package com.endev.tutoring.exception;

// Somebody already has this username. GlobalExceptionHandler turns it into 409.
public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException(String username) {
        super("username already taken: " + username);
    }
}
