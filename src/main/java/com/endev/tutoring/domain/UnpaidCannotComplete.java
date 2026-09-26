package com.endev.tutoring.domain;

public class UnpaidCannotComplete implements Rule {

    @Override
    public void check(LessonStatus from, LessonStatus to) {
        // the student only pays when the lesson is CONFIRMED
        if (from == LessonStatus.REQUESTED && to == LessonStatus.COMPLETED) {
            throw new IllegalStateException("An unpaid lesson cannot be completed");
        }
    }
}
