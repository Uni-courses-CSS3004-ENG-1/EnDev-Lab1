package com.endev.tutoring;

/**
 * Where a lesson stands, from the student asking for a slot to the tutor being paid.
 *
 * <p>The type is sealed, so {@link LessonPolicy} switches over every status without a
 * default branch: a new status here does not compile until the policy decides where
 * that status may move.
 */
public sealed interface LessonStatus
        permits LessonStatus.Requested, LessonStatus.Confirmed,
                LessonStatus.Completed, LessonStatus.Cancelled {

    /** The student asked a tutor for a time slot. No money has moved yet. */
    record Requested() implements LessonStatus {}

    /** The tutor accepted the slot and the platform holds the student's payment. */
    record Confirmed() implements LessonStatus {}

    /** The lesson took place: the tutor is paid and the platform keeps its commission. Final. */
    record Completed() implements LessonStatus {}

    /** The lesson was called off before it took place; any held payment went back to the student. Final. */
    record Cancelled() implements LessonStatus {}

    /**
     * Looks a status up by the name used in the README status table, e.g. {@code "Confirmed"}.
     *
     * @throws IllegalArgumentException if no status has that name
     */
    static LessonStatus of(String name) {
        return switch (name) {
            case "Requested" -> new Requested();
            case "Confirmed" -> new Confirmed();
            case "Completed" -> new Completed();
            case "Cancelled" -> new Cancelled();
            case null, default -> throw new IllegalArgumentException("Unknown lesson status: " + name);
        };
    }

    /** The name used in the README status table, e.g. {@code "Confirmed"}. */
    default String name() {
        return getClass().getSimpleName();
    }
}
