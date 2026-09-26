package com.endev.tutoring.handler;

import com.endev.tutoring.config.Application;
import com.endev.tutoring.domain.LessonStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = Application.class)
class LessonServiceTest {

    @Autowired
    private LessonService lessonService;

    @Test
    void allowedMoveReturnsNewStatus() {
        assertEquals(LessonStatus.CONFIRMED, lessonService.move(LessonStatus.REQUESTED, LessonStatus.CONFIRMED));
    }

    @Test
    void unpaidRuleIsUsed() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> lessonService.move(LessonStatus.REQUESTED, LessonStatus.COMPLETED));
        assertEquals("An unpaid lesson cannot be completed", e.getMessage());
    }

    @Test
    void transitionRuleIsUsed() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> lessonService.move(LessonStatus.COMPLETED, LessonStatus.CANCELLED));
        assertEquals("Cannot move a lesson from COMPLETED to CANCELLED", e.getMessage());
    }

    @Test
    void nullStatusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> lessonService.move(null, LessonStatus.CONFIRMED));
    }
}
