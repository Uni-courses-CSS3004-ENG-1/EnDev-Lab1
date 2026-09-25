package com.endev.tutoring.handler;

import com.endev.tutoring.config.Application;
import com.endev.tutoring.domain.LessonStatus.Cancelled;
import com.endev.tutoring.domain.LessonStatus.Completed;
import com.endev.tutoring.domain.LessonStatus.Confirmed;
import com.endev.tutoring.domain.LessonStatus.Requested;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = Application.class)
class LessonServiceTest {

    @Autowired
    private LessonService service;

    @Test
    @DisplayName("an allowed move returns the target status")
    void allowedMoveReturnsTarget() {
        assertEquals(new Confirmed(), service.move(new Requested(), new Confirmed()));
    }

    @Test
    @DisplayName("the wired chain runs UnpaidCannotComplete")
    void chainRunsUnpaidCannotComplete() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.move(new Requested(), new Completed()));

        assertEquals("An unpaid lesson cannot be completed", error.getMessage());
    }

    @Test
    @DisplayName("the wired chain runs TransitionRule")
    void chainRunsTransitionRule() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.move(new Completed(), new Cancelled()));

        assertEquals("Cannot move a lesson from Completed to Cancelled", error.getMessage());
    }
}
