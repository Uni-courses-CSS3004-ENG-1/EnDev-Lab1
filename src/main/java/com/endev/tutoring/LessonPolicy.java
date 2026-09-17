package com.endev.tutoring;

import com.endev.tutoring.LessonStatus.Cancelled;
import com.endev.tutoring.LessonStatus.Completed;
import com.endev.tutoring.LessonStatus.Confirmed;
import com.endev.tutoring.LessonStatus.Requested;

/**
 * Decides which status change a lesson is allowed to make.
 *
 * <p>Money moves with the status. {@code Confirmed} holds the student's payment,
 * {@code Completed} pays the tutor and books the commission, and {@code Cancelled}
 * returns the payment. So a lesson has to be confirmed before it can be completed,
 * and once it is completed or cancelled it stays that way.
 */
public final class LessonPolicy {

    /**
     * Applies a status change.
     *
     * @param from the current status of the lesson
     * @param to   the status the lesson should move to
     * @return {@code to}, when the change is allowed
     * @throws IllegalArgumentException if either status is {@code null}
     * @throws IllegalStateException    if the change is forbidden
     */
    public LessonStatus move(LessonStatus from, LessonStatus to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Both the current and the target status are required");
        }
        boolean allowed = switch (from) {
            case Requested() -> to instanceof Confirmed || to instanceof Cancelled;
            case Confirmed() -> to instanceof Completed || to instanceof Cancelled;
            case Completed() -> false;
            case Cancelled() -> false;
        };
        if (!allowed) {
            throw new IllegalStateException(
                    "Cannot move a lesson from " + from.name() + " to " + to.name());
        }
        return to;
    }
}
