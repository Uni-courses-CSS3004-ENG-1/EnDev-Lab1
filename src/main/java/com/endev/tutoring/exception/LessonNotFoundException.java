package com.endev.tutoring.exception;

import java.util.UUID;

// No lesson row has this UUID. Unchecked, like DuplicateLessonException; the web layer decides what it means for HTTP.
public class LessonNotFoundException extends RuntimeException {

    public LessonNotFoundException(UUID id) {
        super("lesson not found: " + id);
    }
}
