package com.endev.tutoring.domain;

import com.endev.tutoring.domain.LessonStatus.Completed;
import com.endev.tutoring.domain.LessonStatus.Requested;

public final class UnpaidCannotComplete implements Rule {

    @Override
    public void check(LessonStatus from, LessonStatus to) {
        if (from instanceof Requested && to instanceof Completed) {
            throw new IllegalStateException("An unpaid lesson cannot be completed");
        }
    }
}
