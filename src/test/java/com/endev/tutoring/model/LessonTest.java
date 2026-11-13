package com.endev.tutoring.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LessonTest {

    private static final LessonId KEY = new LessonId("LES-2026-0042");

    @Test
    void idKeyAndStatusAreRequired() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Lesson(null, KEY, LessonStatus.REQUESTED, "Algebra")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Lesson(UUID.randomUUID(), null, LessonStatus.REQUESTED, "Algebra")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Lesson(UUID.randomUUID(), KEY, null, "Algebra")));
    }
}
