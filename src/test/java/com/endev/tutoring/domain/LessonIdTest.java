package com.endev.tutoring.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LessonIdTest {

    @Test
    void keepsValue() {
        LessonId id = new LessonId("LES-2026-0042");
        assertEquals("LES-2026-0042", id.value());
    }

    @Test
    void nullIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LessonId(null));
    }

    @Test
    void emptyIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LessonId(""));
    }

    @Test
    void blankIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LessonId("   "));
    }
}
