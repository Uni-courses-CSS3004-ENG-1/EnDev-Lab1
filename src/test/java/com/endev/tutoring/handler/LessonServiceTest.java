package com.endev.tutoring.handler;

import com.endev.tutoring.config.Application;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.endev.tutoring.domain.LessonStatus.CANCELLED;
import static com.endev.tutoring.domain.LessonStatus.COMPLETED;
import static com.endev.tutoring.domain.LessonStatus.CONFIRMED;
import static com.endev.tutoring.domain.LessonStatus.REQUESTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = Application.class)
class LessonServiceTest {

    @Autowired
    private LessonService service;

    @Test
    @DisplayName("an allowed move returns the target status")
    void allowedMoveReturnsTarget() {
        assertEquals(CONFIRMED, service.move(REQUESTED, CONFIRMED));
    }

    @Test
    @DisplayName("the wired chain runs UnpaidCannotComplete")
    void chainRunsUnpaidCannotComplete() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.move(REQUESTED, COMPLETED));

        assertEquals("An unpaid lesson cannot be completed", error.getMessage());
    }

    @Test
    @DisplayName("the wired chain runs TransitionRule")
    void chainRunsTransitionRule() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.move(COMPLETED, CANCELLED));

        assertEquals("Cannot move a lesson from COMPLETED to CANCELLED", error.getMessage());
    }

    @Test
    @DisplayName("a missing status is rejected the same way as in LessonPolicy")
    void missingStatusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.move(null, CONFIRMED));
    }
}
