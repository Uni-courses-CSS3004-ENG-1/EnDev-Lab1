package com.endev.tutoring.exception;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class LessonNotFoundExceptionTest {

    private static final UUID ID = UUID.fromString("7d3f0a52-1c2b-4c55-9a51-2f9d0c1e4b6a");

    @Test
    void namesTheId() {
        assertEquals("lesson not found: 7d3f0a52-1c2b-4c55-9a51-2f9d0c1e4b6a", new LessonNotFoundException(ID).getMessage());
    }

    @Test
    void isUnchecked() {
        assertInstanceOf(RuntimeException.class, new LessonNotFoundException(ID));
    }
}
