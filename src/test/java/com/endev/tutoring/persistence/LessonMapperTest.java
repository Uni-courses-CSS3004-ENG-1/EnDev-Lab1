package com.endev.tutoring.persistence;

import com.endev.tutoring.domain.LessonStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LessonMapperTest {

    @Test
    void mapsEveryStatusToTheColumnValue() {
        assertEquals("REQUESTED", LessonMapper.toText(LessonStatus.REQUESTED));
        assertEquals("CONFIRMED", LessonMapper.toText(LessonStatus.CONFIRMED));
        assertEquals("COMPLETED", LessonMapper.toText(LessonStatus.COMPLETED));
        assertEquals("CANCELLED", LessonMapper.toText(LessonStatus.CANCELLED));
    }

    @ParameterizedTest
    @EnumSource(LessonStatus.class)
    void everyStatusSurvivesTheRoundTrip(LessonStatus status) {
        assertEquals(status, LessonMapper.toStatus(LessonMapper.toText(status)));
    }

    @Test
    void unknownTextIsRejected() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> LessonMapper.toStatus("PAID"));
        assertEquals("Unknown lesson status in the database: PAID", e.getMessage());
    }

    @Test
    void enumStyleTextIsRejected() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> LessonMapper.toStatus("Requested"));
        assertEquals("Unknown lesson status in the database: Requested", e.getMessage());
    }

    @Test
    void nullTextIsRejected() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> LessonMapper.toStatus(null));
        assertEquals("Unknown lesson status in the database: null", e.getMessage());
    }
}
