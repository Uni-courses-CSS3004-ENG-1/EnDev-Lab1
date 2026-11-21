package com.endev.tutoring.exception;

// The lesson rules refused the move, for example confirming a lesson that is already COMPLETED.
// Unchecked, so the transaction rolls back and the row keeps its status.
public class LessonTransitionException extends RuntimeException {

    public LessonTransitionException(String message) {
        super(message);
    }
}
