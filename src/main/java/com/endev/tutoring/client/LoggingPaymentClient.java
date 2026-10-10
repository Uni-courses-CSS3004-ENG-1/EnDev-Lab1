package com.endev.tutoring.client;

import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.PaymentPort;
import org.springframework.stereotype.Component;

// Stands in for the payment provider until there is a real HTTP client. It only prints.
@Component
public class LoggingPaymentClient implements PaymentPort {

    @Override
    public void hold(LessonId lesson) {
        System.out.println("  payment: hold for " + lesson.value());
    }

    @Override
    public void release(LessonId lesson) {
        System.out.println("  payment: release to the tutor for " + lesson.value());
    }

    @Override
    public void refund(LessonId lesson) {
        System.out.println("  payment: refund to the student for " + lesson.value());
    }
}
