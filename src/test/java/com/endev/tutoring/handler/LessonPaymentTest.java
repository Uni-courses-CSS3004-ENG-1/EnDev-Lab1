package com.endev.tutoring.handler;

import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonPolicy;
import com.endev.tutoring.domain.LessonStatus;
import com.endev.tutoring.domain.PaymentGateway;
import com.endev.tutoring.domain.TransitionRule;
import com.endev.tutoring.domain.UnpaidCannotComplete;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

// The payment port is mocked, so no real HTTP endpoint is called
class LessonPaymentTest {

    private final LessonId lesson = new LessonId("LES-2026-0042");

    private PaymentGateway payments;
    private LessonService service;

    @BeforeEach
    void setUp() {
        payments = mock(PaymentGateway.class);
        LessonPolicy policy = new LessonPolicy(List.of(new UnpaidCannotComplete(), new TransitionRule()));
        service = new LessonService(policy, payments);
    }

    @Test
    void confirmingHoldsThePayment() {
        assertEquals(LessonStatus.CONFIRMED,
                service.move(lesson, LessonStatus.REQUESTED, LessonStatus.CONFIRMED));
        verify(payments).hold(lesson);
    }

    @Test
    void completingPaysTheTutor() {
        service.move(lesson, LessonStatus.CONFIRMED, LessonStatus.COMPLETED);
        verify(payments).payTutor(lesson);
    }

    @Test
    void cancellingAPaidLessonRefundsTheStudent() {
        service.move(lesson, LessonStatus.CONFIRMED, LessonStatus.CANCELLED);
        verify(payments).refund(lesson);
    }

    @Test
    void cancellingAnUnpaidLessonMovesNoMoney() {
        service.move(lesson, LessonStatus.REQUESTED, LessonStatus.CANCELLED);
        verifyNoInteractions(payments);
    }

    @Test
    void forbiddenMoveMovesNoMoney() {
        assertThrows(IllegalStateException.class,
                () -> service.move(lesson, LessonStatus.REQUESTED, LessonStatus.COMPLETED));
        assertThrows(IllegalStateException.class,
                () -> service.move(lesson, LessonStatus.COMPLETED, LessonStatus.CANCELLED));
        verifyNoInteractions(payments);
    }

    @Test
    void missingLessonIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.move(null, LessonStatus.REQUESTED, LessonStatus.CONFIRMED));
        verifyNoInteractions(payments);
    }
}
