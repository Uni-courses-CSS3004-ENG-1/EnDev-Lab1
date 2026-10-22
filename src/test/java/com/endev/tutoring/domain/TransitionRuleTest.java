package com.endev.tutoring.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// the status table from the README, one @Nested group per result
class TransitionRuleTest {

    private final TransitionRule rule = new TransitionRule();

    @Nested
    @DisplayName("allowed moves")
    class Allowed {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "REQUESTED, CONFIRMED",
                "REQUESTED, CANCELLED",
                "CONFIRMED, COMPLETED",
                "CONFIRMED, CANCELLED"
        })
        void passes(LessonStatus from, LessonStatus to) {
            assertDoesNotThrow(() -> rule.check(from, to));
        }
    }

    @Nested
    @DisplayName("forbidden moves")
    class Forbidden {

        // the two forbidden rows, moves out of a final status, and moves backwards
        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "REQUESTED, COMPLETED",
                "COMPLETED, CANCELLED",
                "CANCELLED, CONFIRMED",
                "COMPLETED, REQUESTED",
                "CONFIRMED, REQUESTED"
        })
        void throwsIllegalState(LessonStatus from, LessonStatus to) {
            assertThrows(IllegalStateException.class, () -> rule.check(from, to));
        }

        @Test
        void messageNamesBothStatuses() {
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> rule.check(LessonStatus.COMPLETED, LessonStatus.CANCELLED));
            assertEquals("Cannot move a lesson from COMPLETED to CANCELLED", e.getMessage());
        }

        // every status, including the final ones, may not stay where it is
        @Test
        void noStatusMovesToItself() {
            assertAll(
                    () -> assertThrows(IllegalStateException.class,
                            () -> rule.check(LessonStatus.REQUESTED, LessonStatus.REQUESTED)),
                    () -> assertThrows(IllegalStateException.class,
                            () -> rule.check(LessonStatus.CONFIRMED, LessonStatus.CONFIRMED)),
                    () -> assertThrows(IllegalStateException.class,
                            () -> rule.check(LessonStatus.COMPLETED, LessonStatus.COMPLETED)),
                    () -> assertThrows(IllegalStateException.class,
                            () -> rule.check(LessonStatus.CANCELLED, LessonStatus.CANCELLED)));
        }
    }
}
