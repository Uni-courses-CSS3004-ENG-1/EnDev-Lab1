package com.endev.tutoring.domain;

import static com.endev.tutoring.domain.LessonStatus.CANCELLED;
import static com.endev.tutoring.domain.LessonStatus.COMPLETED;
import static com.endev.tutoring.domain.LessonStatus.CONFIRMED;

/** The status table from the README: which status may follow which. */
public final class TransitionRule implements Rule {

    @Override
    public void check(LessonStatus from, LessonStatus to) {
        boolean allowed = switch (from) {
            case REQUESTED -> to == CONFIRMED || to == CANCELLED;
            case CONFIRMED -> to == COMPLETED || to == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new IllegalStateException("Cannot move a lesson from " + from + " to " + to);
        }
    }
}
