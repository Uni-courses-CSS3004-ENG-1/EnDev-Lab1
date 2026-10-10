package com.endev.tutoring.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// the status table from the README
class LessonPolicyTest {

    private final LessonPolicy policy = new LessonPolicy(List.of(new UnpaidCannotComplete(), new TransitionRule()));

    // the four allowed rows of the README table
    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "REQUESTED, CONFIRMED",
            "REQUESTED, CANCELLED",
            "CONFIRMED, COMPLETED",
            "CONFIRMED, CANCELLED"
    })
    void allowedMoveReturnsNewStatus(LessonStatus from, LessonStatus to) {
        assertEquals(to, policy.move(from, to));
    }

    // the two forbidden rows of the table, plus moves out of a final status and backwards
    @ParameterizedTest(name = "{0} -> {1} is forbidden")
    @CsvSource({
            "REQUESTED, COMPLETED",
            "COMPLETED, CANCELLED",
            "CANCELLED, CONFIRMED",
            "COMPLETED, REQUESTED",
            "CONFIRMED, REQUESTED"
    })
    void forbiddenMoveThrows(LessonStatus from, LessonStatus to) {
        assertThrows(IllegalStateException.class, () -> policy.move(from, to));
    }

    @Test
    void unpaidRuleRunsFirst() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> policy.move(LessonStatus.REQUESTED, LessonStatus.COMPLETED));
        assertEquals("An unpaid lesson cannot be completed", e.getMessage());
    }

    @Test
    void nullStatusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> policy.move(null, LessonStatus.CONFIRMED));
        assertThrows(IllegalArgumentException.class, () -> policy.move(LessonStatus.REQUESTED, null));
    }
}
