package com.endev.tutoring.handler;

import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonStatus;
import com.endev.tutoring.domain.PaymentPort;
import com.endev.tutoring.domain.Rule;
import org.springframework.stereotype.Service;

@Service
public class LessonService {

    private final Rule rules;
    private final PaymentPort payments;

    public LessonService(Rule rules, PaymentPort payments) {
        this.rules = rules;
        this.payments = payments;
    }

    public LessonStatus move(LessonStatus from, LessonStatus to) {
        rules.check(from, to);
        return to;
    }

    // checks the move, then moves the money the README table describes
    public LessonStatus move(LessonId lesson, LessonStatus from, LessonStatus to) {
        move(from, to);
        if (to == LessonStatus.CONFIRMED) {
            payments.hold(lesson);
        } else if (to == LessonStatus.COMPLETED) {
            payments.release(lesson);
        } else if (from == LessonStatus.CONFIRMED && to == LessonStatus.CANCELLED) {
            payments.refund(lesson);
        }
        return to;
    }
}
