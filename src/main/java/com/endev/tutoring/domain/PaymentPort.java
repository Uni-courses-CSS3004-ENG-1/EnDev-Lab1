package com.endev.tutoring.domain;

// The payment provider, seen from the domain. The adapter lives in client.
public interface PaymentPort {

    // the tutor accepted: take the student's payment and hold it
    void hold(LessonId lesson);

    // the lesson took place: pay the tutor, the platform keeps its commission
    void release(LessonId lesson);

    // called off after payment: give the held money back to the student
    void refund(LessonId lesson);
}
