package com.endev.tutoring.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransitionRuleTest {

    private final Rule rule = new TransitionRule();

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "Requested, Confirmed",
            "Requested, Cancelled",
            "Confirmed, Completed",
            "Confirmed, Cancelled"
    })
    @DisplayName("an allowed move passes")
    void allowedMovePasses(LessonStatus from, LessonStatus to) {
        assertDoesNotThrow(() -> rule.check(from, to));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "Requested, Completed",
            "Completed, Cancelled",
            "Cancelled, Confirmed",
            "Confirmed, Requested"
    })
    @DisplayName("a forbidden move throws and names both statuses")
    void forbiddenMoveThrows(LessonStatus from, LessonStatus to) {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> rule.check(from, to));

        assertEquals("Cannot move a lesson from " + from.name() + " to " + to.name(), error.getMessage());
    }
}
