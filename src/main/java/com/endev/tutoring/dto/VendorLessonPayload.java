package com.endev.tutoring.dto;

import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonStatus;

// JSON from the vendor looks like: {"lessonId": "LES-2026-0042", "status": "BOOKED"}
public record VendorLessonPayload(String lessonId, String status) {

    public LessonId toLessonId() {
        return new LessonId(lessonId);
    }

    public LessonStatus toLessonStatus() {
        if (status == null) {
            throw new IllegalArgumentException("Vendor status is null");
        }
        switch (status) {
            case "PENDING":
                return LessonStatus.REQUESTED;
            case "BOOKED":
                return LessonStatus.CONFIRMED;
            case "FINISHED":
                return LessonStatus.COMPLETED;
            case "CANCELED":
                return LessonStatus.CANCELLED;
            default:
                throw new IllegalArgumentException("Unknown vendor lesson status: " + status);
        }
    }
}
