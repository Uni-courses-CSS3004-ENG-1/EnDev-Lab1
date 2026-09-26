package com.endev.tutoring.domain;

public class TransitionRule implements Rule {

    @Override
    public void check(LessonStatus from, LessonStatus to) {
        boolean allowed = false;

        if (from == LessonStatus.REQUESTED) {
            allowed = to == LessonStatus.CONFIRMED || to == LessonStatus.CANCELLED;
        } else if (from == LessonStatus.CONFIRMED) {
            allowed = to == LessonStatus.COMPLETED || to == LessonStatus.CANCELLED;
        }
        // COMPLETED and CANCELLED are final, nothing can come after them

        if (!allowed) {
            throw new IllegalStateException("Cannot move a lesson from " + from + " to " + to);
        }
    }
}
