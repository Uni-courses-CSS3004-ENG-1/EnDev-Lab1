package com.endev.tutoring.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// the status table from the README
class LessonPolicyTest {

    private final LessonPolicy policy = new LessonPolicy(List.of(new UnpaidCannotComplete(), new TransitionRule()));

    @Test
    void requestedToConfirmed() {
        assertEquals(LessonStatus.CONFIRMED, policy.move(LessonStatus.REQUESTED, LessonStatus.CONFIRMED));
    }

    @Test
    void requestedToCancelled() {
        assertEquals(LessonStatus.CANCELLED, policy.move(LessonStatus.REQUESTED, LessonStatus.CANCELLED));
    }

    @Test
    void confirmedToCompleted() {
        assertEquals(LessonStatus.COMPLETED, policy.move(LessonStatus.CONFIRMED, LessonStatus.COMPLETED));
    }

    @Test
    void confirmedToCancelled() {
        assertEquals(LessonStatus.CANCELLED, policy.move(LessonStatus.CONFIRMED, LessonStatus.CANCELLED));
    }

    @Test
    void requestedToCompletedIsForbidden() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> policy.move(LessonStatus.REQUESTED, LessonStatus.COMPLETED));
        // the unpaid rule runs first
        assertEquals("An unpaid lesson cannot be completed", e.getMessage());
    }

    @Test
    void completedToCancelledIsForbidden() {
        assertThrows(IllegalStateException.class,
                () -> policy.move(LessonStatus.COMPLETED, LessonStatus.CANCELLED));
    }

    @Test
    void nullStatusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> policy.move(null, LessonStatus.CONFIRMED));
        assertThrows(IllegalArgumentException.class, () -> policy.move(LessonStatus.REQUESTED, null));
    }
}
