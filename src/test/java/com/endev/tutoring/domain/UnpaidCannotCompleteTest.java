package com.endev.tutoring.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnpaidCannotCompleteTest {

    private final UnpaidCannotComplete rule = new UnpaidCannotComplete();

    @Test
    void unpaidLessonCannotBeCompleted() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> rule.check(LessonStatus.REQUESTED, LessonStatus.COMPLETED));
        assertEquals("An unpaid lesson cannot be completed", e.getMessage());
    }

    @Test
    void paidLessonCanBeCompleted() {
        assertDoesNotThrow(() -> rule.check(LessonStatus.CONFIRMED, LessonStatus.COMPLETED));
    }

    @Test
    void otherMovesAreNotChecked() {
        assertDoesNotThrow(() -> rule.check(LessonStatus.REQUESTED, LessonStatus.CONFIRMED));
        assertDoesNotThrow(() -> rule.check(LessonStatus.COMPLETED, LessonStatus.CANCELLED));
    }
}
