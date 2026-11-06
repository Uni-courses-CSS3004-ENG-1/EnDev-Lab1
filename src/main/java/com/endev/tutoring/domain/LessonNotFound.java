package com.endev.tutoring.domain;

import java.util.UUID;

// No lesson row has this UUID. Unchecked, like DuplicateLesson; the web layer decides what it means for HTTP.
public class LessonNotFound extends RuntimeException {

    public LessonNotFound(UUID id) {
        super("lesson not found: " + id);
    }
}
