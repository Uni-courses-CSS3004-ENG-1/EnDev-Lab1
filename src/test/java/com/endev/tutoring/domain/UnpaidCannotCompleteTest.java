package com.endev.tutoring.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnpaidCannotCompleteTest {

    private final Rule rule = new UnpaidCannotComplete();

    @Test
    @DisplayName("an unpaid lesson cannot be completed")
    void unpaidLessonCannotBeCompleted() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> rule.check(new LessonStatus.Requested(), new LessonStatus.Completed()));

        assertEquals("An unpaid lesson cannot be completed", error.getMessage());
    }

    /** This rule only looks at the payment; the order of statuses is TransitionRule's job. */
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "Confirmed, Completed",
            "Requested, Confirmed",
            "Completed, Cancelled"
    })
    @DisplayName("any other move passes this rule")
    void otherMovesPass(LessonStatus from, LessonStatus to) {
        assertDoesNotThrow(() -> rule.check(from, to));
    }
}
