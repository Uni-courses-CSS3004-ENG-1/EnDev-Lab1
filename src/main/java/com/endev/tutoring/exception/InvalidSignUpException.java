package com.endev.tutoring.exception;

// A sign-up that the field annotations cannot catch alone, such as a tutor without a subject. 400.
public class InvalidSignUpException extends RuntimeException {

    public InvalidSignUpException(String message) {
        super(message);
    }
}
