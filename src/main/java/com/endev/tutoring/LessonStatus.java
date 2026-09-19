package com.endev.tutoring;

/** Sealed so {@link LessonPolicy} switches over every status without a default branch. */
public sealed interface LessonStatus
        permits LessonStatus.Requested, LessonStatus.Confirmed,
                LessonStatus.Completed, LessonStatus.Cancelled {

    record Requested() implements LessonStatus {}

    record Confirmed() implements LessonStatus {}

    record Completed() implements LessonStatus {}

    record Cancelled() implements LessonStatus {}

    // No production caller: JUnit uses this to convert the names in LessonPolicyTest's CSV rows.
    static LessonStatus of(String name) {
        return switch (name) {
            case "Requested" -> new Requested();
            case "Confirmed" -> new Confirmed();
            case "Completed" -> new Completed();
            case "Cancelled" -> new Cancelled();
            case null, default -> throw new IllegalArgumentException("Unknown lesson status: " + name);
        };
    }

    default String name() {
        return getClass().getSimpleName();
    }
}
