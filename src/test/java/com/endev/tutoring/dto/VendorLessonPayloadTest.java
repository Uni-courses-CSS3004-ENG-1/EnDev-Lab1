package com.endev.tutoring.dto;

import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VendorLessonPayloadTest {

    @Test
    void pendingIsRequested() {
        assertEquals(LessonStatus.REQUESTED, new VendorLessonPayload("LES-1", "PENDING").toLessonStatus());
    }

    @Test
    void bookedIsConfirmed() {
        assertEquals(LessonStatus.CONFIRMED, new VendorLessonPayload("LES-1", "BOOKED").toLessonStatus());
    }

    @Test
    void finishedIsCompleted() {
        assertEquals(LessonStatus.COMPLETED, new VendorLessonPayload("LES-1", "FINISHED").toLessonStatus());
    }

    @Test
    void canceledIsCancelled() {
        assertEquals(LessonStatus.CANCELLED, new VendorLessonPayload("LES-1", "CANCELED").toLessonStatus());
    }

    @Test
    void unknownStatusIsRejected() {
        VendorLessonPayload payload = new VendorLessonPayload("LES-1", "REFUNDED");
        assertThrows(IllegalArgumentException.class, payload::toLessonStatus);
    }

    @Test
    void lowercaseStatusIsRejected() {
        VendorLessonPayload payload = new VendorLessonPayload("LES-1", "booked");
        assertThrows(IllegalArgumentException.class, payload::toLessonStatus);
    }

    @Test
    void nullStatusIsRejected() {
        VendorLessonPayload payload = new VendorLessonPayload("LES-1", null);
        assertThrows(IllegalArgumentException.class, payload::toLessonStatus);
    }

    @Test
    void lessonIdIsMapped() {
        assertEquals(new LessonId("LES-1"), new VendorLessonPayload("LES-1", "BOOKED").toLessonId());
    }
}
