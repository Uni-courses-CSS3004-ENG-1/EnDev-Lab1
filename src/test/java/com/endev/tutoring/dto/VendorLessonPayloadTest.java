package com.endev.tutoring.dto;

import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VendorLessonPayloadTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "PENDING, Requested",
            "BOOKED, Confirmed",
            "FINISHED, Completed",
            "CANCELED, Cancelled"
    })
    @DisplayName("each vendor status maps onto one lesson status")
    void vendorStatusMapsOntoLessonStatus(String vendorStatus, LessonStatus expected) {
        assertEquals(expected, new VendorLessonPayload("LES-2026-0042", vendorStatus).toLessonStatus());
    }

    @ParameterizedTest(name = "status=\"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"REFUNDED", "booked", "Confirmed"})
    @DisplayName("an unknown vendor status is rejected")
    void unknownVendorStatusIsRejected(String vendorStatus) {
        VendorLessonPayload payload = new VendorLessonPayload("LES-2026-0042", vendorStatus);

        assertThrows(IllegalArgumentException.class, payload::toLessonStatus);
    }

    @Test
    @DisplayName("the vendor lesson id becomes a LessonId")
    void vendorLessonIdBecomesLessonId() {
        assertEquals(new LessonId("LES-2026-0042"),
                new VendorLessonPayload("LES-2026-0042", "BOOKED").toLessonId());
    }
}
