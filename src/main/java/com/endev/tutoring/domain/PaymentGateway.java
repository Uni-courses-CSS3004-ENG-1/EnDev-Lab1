package com.endev.tutoring.domain;

// Outbound port: the platform asks the payment provider to move money.
// The domain only knows this interface, never the provider's HTTP API.
public interface PaymentGateway {

    // REQUESTED -> CONFIRMED: take the student's payment and keep it
    void hold(LessonId lesson);

    // CONFIRMED -> COMPLETED: pay the tutor, minus the platform commission
    void payTutor(LessonId lesson);

    // CONFIRMED -> CANCELLED: give the held payment back to the student
    void refund(LessonId lesson);
}
