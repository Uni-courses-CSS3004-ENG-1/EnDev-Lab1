package com.endev.tutoring.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LessonIdTest {

    @ParameterizedTest(name = "id=\"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("a null or blank lesson id is rejected")
    void nullOrBlankIdIsRejected(String value) {
        assertThrows(IllegalArgumentException.class, () -> new LessonId(value));
    }

    @Test
    @DisplayName("a valid lesson id keeps its value")
    void validIdIsKept() {
        assertEquals("LES-2026-0042", new LessonId("LES-2026-0042").value());
    }
}
