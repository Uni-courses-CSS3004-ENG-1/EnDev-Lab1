package com.endev.tutoring.service;

import com.endev.tutoring.TutoringApplication;
import com.endev.tutoring.exception.LessonNotFoundException;
import com.endev.tutoring.exception.LessonTransitionException;
import com.endev.tutoring.model.Lesson;
import com.endev.tutoring.model.LessonId;
import com.endev.tutoring.model.LessonStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// The second lock, without HTTP: @PreAuthorize on the service method, against PostgreSQL.
@SpringBootTest(classes = TutoringApplication.class)
class LessonWorkflowServiceTest {

    @Autowired
    private LessonWorkflowService workflow;

    @Autowired
    private LessonRegistrationService registration;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID requested;

    @BeforeEach
    void oneRequestedLesson() {
        jdbc.update("DELETE FROM lesson");
        requested = registration.create(new LessonId("LES-2026-0700"), "Algebra").id();
    }

    private String status(UUID id) {
        return jdbc.queryForObject("SELECT status FROM lesson WHERE id = ?", String.class, id);
    }

    @Nested
    @DisplayName("who may confirm")
    class Who {

        @Test
        @WithMockUser(username = "aliya", roles = "STUDENT")
        void aStudentIsDeniedAndTheRowStays() {
            assertThrows(AccessDeniedException.class, () -> workflow.confirm(requested, "aliya"));
            assertEquals("REQUESTED", status(requested));
        }

        @Test
        void nobodyLoggedInIsDenied() {
            assertThrows(AuthenticationCredentialsNotFoundException.class,
                    () -> workflow.confirm(requested, "dana"));
            assertEquals("REQUESTED", status(requested));
        }

        @Test
        @WithMockUser(username = "dana", roles = "TUTOR")
        void aTutorConfirms() {
            Lesson confirmed = workflow.confirm(requested, "dana");
            assertAll(
                    () -> assertEquals(LessonStatus.CONFIRMED, confirmed.status()),
                    () -> assertEquals("CONFIRMED", status(requested)));
        }
    }

    @Nested
    @DisplayName("what the rules allow")
    @WithMockUser(username = "dana", roles = "TUTOR")
    class Rules {

        @Test
        void confirmingTwiceIsRefusedAndTheRowStaysConfirmed() {
            workflow.confirm(requested, "dana");

            LessonTransitionException refused = assertThrows(LessonTransitionException.class,
                    () -> workflow.confirm(requested, "dana"));
            assertAll(
                    () -> assertEquals("Cannot move a lesson from CONFIRMED to CONFIRMED", refused.getMessage()),
                    () -> assertEquals("CONFIRMED", status(requested)));
        }

        @Test
        void aCancelledLessonCannotBeConfirmed() {
            jdbc.update("UPDATE lesson SET status = 'CANCELLED' WHERE id = ?", requested);

            assertThrows(LessonTransitionException.class, () -> workflow.confirm(requested, "dana"));
            assertEquals("CANCELLED", status(requested));
        }

        @Test
        void anUnknownLessonIsNotFound() {
            assertThrows(LessonNotFoundException.class, () -> workflow.confirm(UUID.randomUUID(), "dana"));
        }
    }
}
