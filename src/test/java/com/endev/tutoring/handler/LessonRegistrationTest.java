package com.endev.tutoring.handler;

import com.endev.tutoring.config.Application;
import com.endev.tutoring.domain.DuplicateLesson;
import com.endev.tutoring.domain.Lesson;
import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonNotFound;
import com.endev.tutoring.domain.LessonStatus;
import com.endev.tutoring.persistence.LessonJpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Integration test: the whole app against PostgreSQL. Not @Transactional,
// so every count is a separate query that sees only committed rows.
@SpringBootTest(classes = Application.class)
class LessonRegistrationTest {

    private static final LessonId LESSON = new LessonId("LES-2026-0042");

    @Autowired
    private LessonRegistration registration;

    @Autowired
    private LessonJpa lessons;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanRows() {
        jdbc.update("DELETE FROM lesson");
    }

    @Nested
    @DisplayName("the two counts")
    class TwoCounts {

        // one transaction, two inserts: the second fails, so the first is rolled back too
        @Test
        void secondStatementRollsBack() {
            assertThrows(DuplicateLesson.class, () -> registration.insertTwice(LESSON));
            assertEquals(0, lessons.count(LESSON.value()));
        }

        // two transactions: the second fails, the first already committed
        @Test
        void secondRequestKeepsTheFirst() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();

            registration.register(id1, LESSON, "Algebra", "Dana");
            assertThrows(DuplicateLesson.class, () -> registration.register(id2, LESSON, "Again", "Dana"));
            assertEquals(1, lessons.count(LESSON.value()));
        }
    }

    @Nested
    @DisplayName("create and find by UUID")
    class CreateAndFind {

        @Test
        void createdLessonIsReadBackByItsUuid() {
            Lesson created = registration.create(LESSON, "Algebra");

            Lesson found = registration.find(created.id());

            assertAll(
                    () -> assertEquals(created, found),
                    () -> assertEquals(LessonStatus.REQUESTED, found.status()),
                    () -> assertEquals(1, lessons.count(LESSON.value())));
        }

        @Test
        void createLeavesTheTutorAtTheColumnDefault() {
            registration.create(LESSON, "Algebra");

            assertEquals("UNASSIGNED", jdbc.queryForObject(
                    "SELECT tutor FROM lesson WHERE business_key = ?", String.class, LESSON.value()));
        }

        // the same business key twice: the first committed row stays
        @Test
        void secondCreateOfTheSameKeyKeepsTheFirst() {
            Lesson first = registration.create(LESSON, "Algebra");

            assertThrows(DuplicateLesson.class, () -> registration.create(LESSON, "Again"));

            assertAll(
                    () -> assertEquals(1, lessons.count(LESSON.value())),
                    () -> assertEquals("Algebra", registration.find(first.id()).subject()));
        }

        @Test
        void unknownUuidIsNotFound() {
            UUID missing = UUID.randomUUID();

            LessonNotFound e = assertThrows(LessonNotFound.class, () -> registration.find(missing));

            assertEquals("lesson not found: " + missing, e.getMessage());
        }

        @Test
        void findDoesNotWrite() {
            Lesson created = registration.create(LESSON, "Algebra");

            registration.find(created.id());

            assertAll(
                    () -> assertEquals(1, lessons.count(LESSON.value())),
                    () -> assertEquals(LessonStatus.REQUESTED, registration.status(LESSON)));
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        void commitsARequestedLesson() {
            registration.register(UUID.randomUUID(), LESSON, "Algebra", "Dana");

            assertAll(
                    () -> assertEquals(1, lessons.count(LESSON.value())),
                    () -> assertEquals(LessonStatus.REQUESTED, registration.status(LESSON)));
        }

        @Test
        void duplicateMessageNamesBusinessKey() {
            registration.register(UUID.randomUUID(), LESSON, "Algebra", "Dana");

            DuplicateLesson duplicate = assertThrows(DuplicateLesson.class,
                    () -> registration.register(UUID.randomUUID(), LESSON, "Again", "Dana"));

            assertEquals("duplicate lesson: LES-2026-0042", duplicate.getMessage());
        }

        @Test
        void blankSubjectIsRejectedWithoutWriting() {
            assertThrows(IllegalArgumentException.class,
                    () -> registration.register(UUID.randomUUID(), LESSON, "  ", "Dana"));

            assertEquals(0, lessons.count(LESSON.value()));
        }

        @Test
        void tutorIsStored() {
            registration.register(UUID.randomUUID(), LESSON, "Algebra", "Dana");

            assertEquals("Dana", jdbc.queryForObject(
                    "SELECT tutor FROM lesson WHERE business_key = ?", String.class, LESSON.value()));
        }

        @Test
        void blankTutorIsRejectedWithoutWriting() {
            assertThrows(IllegalArgumentException.class,
                    () -> registration.register(UUID.randomUUID(), LESSON, "Algebra", " "));

            assertEquals(0, lessons.count(LESSON.value()));
        }

        @Test
        void missingArgumentsAreRejected() {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> registration.register(null, LESSON, "Algebra", "Dana")),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> registration.register(UUID.randomUUID(), null, "Algebra", "Dana")),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> registration.register(UUID.randomUUID(), LESSON, null, "Dana")),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> registration.register(UUID.randomUUID(), LESSON, "Algebra", null)));
        }
    }
}
