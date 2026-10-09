package com.endev.tutoring.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// The status table from the README, one row per line
class LessonStatusTableTest {

    private final LessonPolicy policy =
            new LessonPolicy(List.of(new UnpaidCannotComplete(), new TransitionRule()));

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "REQUESTED, CONFIRMED",
            "REQUESTED, CANCELLED",
            "CONFIRMED, COMPLETED",
            "CONFIRMED, CANCELLED"
    })
    void allowedMovesReturnTheNewStatus(LessonStatus from, LessonStatus to) {
        assertEquals(to, policy.move(from, to));
    }

    @ParameterizedTest(name = "{0} -> {1} is forbidden")
    @CsvSource({
            "REQUESTED, COMPLETED",
            "COMPLETED, CANCELLED"
    })
    void forbiddenMovesThrow(LessonStatus from, LessonStatus to) {
        assertThrows(IllegalStateException.class, () -> policy.move(from, to));
    }

    // "Any move not in the table is forbidden too"
    @ParameterizedTest(name = "{0} -> {1} is not in the table")
    @CsvSource({
            "COMPLETED, CONFIRMED",
            "CANCELLED, CONFIRMED",
            "CONFIRMED, REQUESTED",
            "REQUESTED, REQUESTED"
    })
    void movesOutsideTheTableThrow(LessonStatus from, LessonStatus to) {
        assertThrows(IllegalStateException.class, () -> policy.move(from, to));
    }
}
