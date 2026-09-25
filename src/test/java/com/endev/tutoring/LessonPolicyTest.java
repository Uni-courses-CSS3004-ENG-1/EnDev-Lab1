package com.endev.tutoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

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
    @DisplayName("the transition rule rejects a final status change")
    void transitionRuleRejectsFinalStatusChange() {
        Rule rule = new TransitionRule();

        assertThrows(IllegalStateException.class,
                () -> rule.check(new LessonStatus.Completed(), new LessonStatus.Cancelled()));
    }

    @Test
    @DisplayName("an unpaid lesson cannot be completed")
    void unpaidLessonCannotBeCompleted() {
        Rule rule = new UnpaidCannotComplete();

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> rule.check(new LessonStatus.Requested(), new LessonStatus.Completed()));

        assertEquals("An unpaid lesson cannot be completed", error.getMessage());
    }

    @Test
    @DisplayName("the default lesson policy keeps the unpaid stop rule")
    void defaultPolicyKeepsTheUnpaidStopRule() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> policy.move(new LessonStatus.Requested(), new LessonStatus.Completed()));

        assertEquals("An unpaid lesson cannot be completed", error.getMessage());
    }

    @ParameterizedTest(name = "id=\"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("a null or blank lesson id is rejected")
    void nullOrBlankIdIsRejected(String value) {
        assertThrows(IllegalArgumentException.class, () -> new LessonId(value));
    }

    @Test
    @DisplayName("a valid lesson id keeps its value")
    void validIdIsKept() {
        assertEquals("LES-2026-0042", new LessonId("LES-2026-0042").value());
    }
}
