package com.endev.tutoring.exception;

// The tutor already has a lesson within an hour of this start time. 409.
public class SlotTakenException extends RuntimeException {

    public SlotTakenException() {
        super("the tutor already has a lesson at that time");
    }
}
