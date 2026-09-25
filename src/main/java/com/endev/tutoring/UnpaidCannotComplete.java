package com.endev.tutoring;

import com.endev.tutoring.LessonStatus.Completed;
import com.endev.tutoring.LessonStatus.Requested;

public final class UnpaidCannotComplete implements Rule {

    @Override
    public void check(LessonStatus from, LessonStatus to) {
        if (from instanceof Requested && to instanceof Completed) {
            throw new IllegalStateException("An unpaid lesson cannot be completed");
        }
    }
}
