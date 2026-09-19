package com.endev.tutoring;

import com.endev.tutoring.LessonStatus.Cancelled;
import com.endev.tutoring.LessonStatus.Completed;
import com.endev.tutoring.LessonStatus.Confirmed;
import com.endev.tutoring.LessonStatus.Requested;

public final class TransitionRule implements Rule {

    @Override
    public void check(LessonStatus from, LessonStatus to) {
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
    }
}
