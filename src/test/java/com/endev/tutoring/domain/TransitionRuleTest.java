package com.endev.tutoring.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransitionRuleTest {

    private final TransitionRule rule = new TransitionRule();

    @Test
    void requestedToConfirmedIsAllowed() {
        assertDoesNotThrow(() -> rule.check(LessonStatus.REQUESTED, LessonStatus.CONFIRMED));
    }

    @Test
    void requestedToCancelledIsAllowed() {
        assertDoesNotThrow(() -> rule.check(LessonStatus.REQUESTED, LessonStatus.CANCELLED));
    }

    @Test
    void confirmedToCompletedIsAllowed() {
        assertDoesNotThrow(() -> rule.check(LessonStatus.CONFIRMED, LessonStatus.COMPLETED));
    }

    @Test
    void confirmedToCancelledIsAllowed() {
        assertDoesNotThrow(() -> rule.check(LessonStatus.CONFIRMED, LessonStatus.CANCELLED));
    }

    @Test
    void requestedToCompletedIsForbidden() {
        assertThrows(IllegalStateException.class,
                () -> rule.check(LessonStatus.REQUESTED, LessonStatus.COMPLETED));
    }

    @Test
    void completedToCancelledIsForbidden() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> rule.check(LessonStatus.COMPLETED, LessonStatus.CANCELLED));
        assertEquals("Cannot move a lesson from COMPLETED to CANCELLED", e.getMessage());
    }

    @Test
    void cancelledLessonCannotMove() {
        assertThrows(IllegalStateException.class,
                () -> rule.check(LessonStatus.CANCELLED, LessonStatus.CONFIRMED));
    }

    @Test
    void cannotGoBack() {
        assertThrows(IllegalStateException.class,
                () -> rule.check(LessonStatus.CONFIRMED, LessonStatus.REQUESTED));
    }
}
