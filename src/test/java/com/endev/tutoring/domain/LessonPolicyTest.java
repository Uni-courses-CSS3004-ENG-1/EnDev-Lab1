package com.endev.tutoring.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static com.endev.tutoring.domain.LessonStatus.COMPLETED;
import static com.endev.tutoring.domain.LessonStatus.CONFIRMED;
import static com.endev.tutoring.domain.LessonStatus.REQUESTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class LessonPolicyTest {

    private final LessonPolicy policy =
            new LessonPolicy(List.of(new UnpaidCannotComplete(), new TransitionRule()));

    /** The rows of the status table in README.md, word for word. */
    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @CsvSource({
            "REQUESTED, CONFIRMED, Allowed",
            "REQUESTED, CANCELLED, Allowed",
            "CONFIRMED, COMPLETED, Allowed",
            "CONFIRMED, CANCELLED, Allowed",
            "REQUESTED, COMPLETED, Forbidden",
            "COMPLETED, CANCELLED, Forbidden"
    })
    @DisplayName("the status table from the README is enforced")
    void statusTableFromReadme(LessonStatus from, LessonStatus to, String result) {
        switch (result) {
            case "Allowed" -> assertEquals(to, policy.move(from, to));
            case "Forbidden" -> assertThrows(IllegalStateException.class, () -> policy.move(from, to));
            default -> fail("Unknown result in the status table: " + result);
        }
    }

    @Test
    @DisplayName("the rules run in the order they are given")
    void rulesRunInOrder() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> policy.move(REQUESTED, COMPLETED));

        assertEquals("An unpaid lesson cannot be completed", error.getMessage());
    }

    @Test
    @DisplayName("a missing status is rejected")
    void missingStatusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> policy.move(null, CONFIRMED));
        assertThrows(IllegalArgumentException.class, () -> policy.move(REQUESTED, null));
    }
}
