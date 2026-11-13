package com.endev.tutoring.service;

import com.endev.tutoring.TutoringApplication;
import com.endev.tutoring.model.LessonStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = TutoringApplication.class)
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
