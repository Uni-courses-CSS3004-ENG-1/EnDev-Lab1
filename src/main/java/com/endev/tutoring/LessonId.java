package com.endev.tutoring;

/**
 * Identifies one lesson on the marketplace, e.g. {@code "LES-2026-0042"}.
 *
 * <p>Every booking, payment and payout points at a lesson through this id, so a
 * lesson can never exist without a usable one.
 */
public record LessonId(String value) {

    public LessonId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Lesson id must not be null or blank");
        }
    }
}
