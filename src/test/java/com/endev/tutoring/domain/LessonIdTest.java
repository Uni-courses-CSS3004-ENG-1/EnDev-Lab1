package com.endev.tutoring.domain;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LessonIdTest {

    @Test
    void keepsValue() {
        LessonId id = new LessonId("LES-2026-0042");
        assertEquals("LES-2026-0042", id.value());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void nullOrBlankIdIsRejected(String value) {
        assertThrows(IllegalArgumentException.class, () -> new LessonId(value));
    }

    // the README shows ids like LES-2026-0042, but LessonId only rejects blanks today
    @Disabled("LessonId does not check the LES-yyyy-nnnn format yet")
    @Test
    void malformedIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LessonId("lesson 42"));
    }
}
