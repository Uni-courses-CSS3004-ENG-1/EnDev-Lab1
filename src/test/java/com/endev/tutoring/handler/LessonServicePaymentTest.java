package com.endev.tutoring.handler;

import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonPolicy;
import com.endev.tutoring.domain.LessonStatus;
import com.endev.tutoring.domain.PaymentPort;
import com.endev.tutoring.domain.TransitionRule;
import com.endev.tutoring.domain.UnpaidCannotComplete;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

// The payment port is a mock: no payment provider and no HTTP call is involved.
@ExtendWith(MockitoExtension.class)
class LessonServicePaymentTest {

    private static final LessonId LESSON = new LessonId("LES-2026-0042");

    @Mock
    private PaymentPort payments;

    private LessonService service;

    @BeforeEach
    void setUp() {
        service = new LessonService(new LessonPolicy(List.of(new UnpaidCannotComplete(), new TransitionRule())), payments);
    }

    @Test
    void confirmingHoldsThePayment() {
        assertEquals(LessonStatus.CONFIRMED, service.move(LESSON, LessonStatus.REQUESTED, LessonStatus.CONFIRMED));
        verify(payments).hold(LESSON);
        verifyNoMoreInteractions(payments);
    }

    @Test
    void completingReleasesThePayment() {
        service.move(LESSON, LessonStatus.CONFIRMED, LessonStatus.COMPLETED);
        verify(payments).release(LESSON);
        verifyNoMoreInteractions(payments);
    }

    @Test
    void cancellingAConfirmedLessonRefunds() {
        service.move(LESSON, LessonStatus.CONFIRMED, LessonStatus.CANCELLED);
        verify(payments).refund(LESSON);
        verifyNoMoreInteractions(payments);
    }

    @Test
    void cancellingARequestedLessonMovesNoMoney() {
        service.move(LESSON, LessonStatus.REQUESTED, LessonStatus.CANCELLED);
        verifyNoInteractions(payments);
    }

    @Test
    void forbiddenMoveMovesNoMoney() {
        assertThrows(IllegalStateException.class,
                () -> service.move(LESSON, LessonStatus.REQUESTED, LessonStatus.COMPLETED));
        verifyNoInteractions(payments);
    }
}
