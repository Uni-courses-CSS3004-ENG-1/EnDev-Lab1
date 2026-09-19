package com.endev.tutoring.domain;

import com.endev.tutoring.domain.LessonStatus.Cancelled;
import com.endev.tutoring.domain.LessonStatus.Completed;
import com.endev.tutoring.domain.LessonStatus.Confirmed;
import com.endev.tutoring.domain.LessonStatus.Requested;

public final class LessonPolicy {

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
