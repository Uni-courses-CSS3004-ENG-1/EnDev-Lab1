package com.endev.tutoring.client;

import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.PaymentGateway;
import org.springframework.stereotype.Component;

// Stand-in for the real payment provider. It only prints, so no HTTP call is made.
// The real HTTP client replaces this class later.
@Component
public class LoggingPaymentGateway implements PaymentGateway {

    @Override
    public void hold(LessonId lesson) {
        System.out.println("  [payment] hold payment for " + lesson.value());
    }

    @Override
    public void payTutor(LessonId lesson) {
        System.out.println("  [payment] pay tutor for " + lesson.value());
    }

    @Override
    public void refund(LessonId lesson) {
        System.out.println("  [payment] refund student for " + lesson.value());
    }
}
