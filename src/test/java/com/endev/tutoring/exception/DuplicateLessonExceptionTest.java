package com.endev.tutoring.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DuplicateLessonExceptionTest {

    @Test
    void namesTheBusinessKey() {
        assertEquals("duplicate lesson: LES-2026-0042", new DuplicateLessonException("LES-2026-0042").getMessage());
    }

    // unchecked, so Spring rolls the transaction back without rollbackFor
    @Test
    void isUnchecked() {
        assertInstanceOf(RuntimeException.class, new DuplicateLessonException("LES-2026-0042"));
    }
}
