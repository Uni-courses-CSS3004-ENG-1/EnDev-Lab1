package com.endev.tutoring.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class LessonPolicyTest {

    private final LessonPolicy policy = new LessonPolicy();

    /** The four rows of the status table in README.md, word for word. */
    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @CsvSource({
            "Requested, Confirmed, Allowed",
            "Confirmed, Completed, Allowed",
            "Requested, Completed, Forbidden",
            "Completed, Cancelled, Forbidden"
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
    @DisplayName("the default policy runs the unpaid stop rule first")
    void defaultPolicyRunsTheUnpaidStopRule() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> policy.move(new LessonStatus.Requested(), new LessonStatus.Completed()));

        assertEquals("An unpaid lesson cannot be completed", error.getMessage());
    }

    @Test
    @DisplayName("a missing status is rejected")
    void missingStatusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> policy.move(null, new LessonStatus.Confirmed()));
        assertThrows(IllegalArgumentException.class, () -> policy.move(new LessonStatus.Requested(), null));
    }
}
