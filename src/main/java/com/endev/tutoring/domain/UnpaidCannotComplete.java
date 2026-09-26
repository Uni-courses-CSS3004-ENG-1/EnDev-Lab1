package com.endev.tutoring.domain;

import static com.endev.tutoring.domain.LessonStatus.COMPLETED;
import static com.endev.tutoring.domain.LessonStatus.REQUESTED;

/** Stop-factor: the platform only holds the payment from CONFIRMED on, so a REQUESTED lesson cannot be completed. */
public final class UnpaidCannotComplete implements Rule {

    @Override
    public void check(LessonStatus from, LessonStatus to) {
        if (from == REQUESTED && to == COMPLETED) {
            throw new IllegalStateException("An unpaid lesson cannot be completed");
        }
    }
}
