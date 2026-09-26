package com.endev.tutoring.dto;

import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonStatus;

/**
 * A lesson as the scheduling vendor sends it, for example
 * {@code {"lessonId": "LES-2026-0042", "status": "BOOKED"}}.
 */
public record VendorLessonPayload(String lessonId, String status) {

    public LessonId toLessonId() {
        return new LessonId(lessonId);
    }

    public LessonStatus toLessonStatus() {
        return switch (status) {
            case "PENDING" -> LessonStatus.REQUESTED;
            case "BOOKED" -> LessonStatus.CONFIRMED;
            case "FINISHED" -> LessonStatus.COMPLETED;
            case "CANCELED" -> LessonStatus.CANCELLED;
            case null, default -> throw new IllegalArgumentException("Unknown vendor lesson status: " + status);
        };
    }
}
