package com.endev.tutoring.handler;

import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonStatus;
import com.endev.tutoring.domain.PaymentGateway;
import com.endev.tutoring.domain.Rule;
import org.springframework.stereotype.Service;

@Service
public class LessonService {

    private final Rule rules;
    private final PaymentGateway payments;

    public LessonService(Rule rules, PaymentGateway payments) {
        this.rules = rules;
        this.payments = payments;
    }

    // Only checks the rules, no money moves
    public LessonStatus move(LessonStatus from, LessonStatus to) {
        rules.check(from, to);
        return to;
    }

    // Checks the rules first. Money moves only after the move is allowed.
    public LessonStatus move(LessonId lesson, LessonStatus from, LessonStatus to) {
        if (lesson == null) {
            throw new IllegalArgumentException("Lesson id must not be null");
        }
        rules.check(from, to);
        switch (to) {
            case CONFIRMED -> payments.hold(lesson);
            case COMPLETED -> payments.payTutor(lesson);
            case CANCELLED -> {
                // a REQUESTED lesson was never paid, so there is nothing to give back
                if (from == LessonStatus.CONFIRMED) {
                    payments.refund(lesson);
                }
            }
            default -> { }
        }
        return to;
    }
}
