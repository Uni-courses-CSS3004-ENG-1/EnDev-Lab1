package com.endev.tutoring.persistence;

import com.endev.tutoring.domain.LessonStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// The text <-> status mapping, no database needed
class LessonJdbcTest {

    @ParameterizedTest
    @EnumSource(LessonStatus.class)
    void everyStatusSurvivesTheRoundTrip(LessonStatus status) {
        assertEquals(status, LessonJdbc.toStatus(status.name()));
    }

    @Test
    void unknownTextIsRejected() {
        assertThrows(IllegalStateException.class, () -> LessonJdbc.toStatus("PAID"));
    }

    @Test
    void lowercaseTextIsRejected() {
        assertThrows(IllegalStateException.class, () -> LessonJdbc.toStatus("requested"));
    }
}
