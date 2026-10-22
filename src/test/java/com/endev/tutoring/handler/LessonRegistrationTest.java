package com.endev.tutoring.handler;

import com.endev.tutoring.config.Application;
import com.endev.tutoring.domain.DuplicateLesson;
import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonStatus;
import com.endev.tutoring.persistence.LessonJpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
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
    void recreateSchema() throws Exception {
        jdbc.execute("DROP TABLE IF EXISTS lesson");
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/schema.sql")));
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

            registration.register(id1, LESSON, "Algebra");
            assertThrows(DuplicateLesson.class, () -> registration.register(id2, LESSON, "Again"));
            assertEquals(1, lessons.count(LESSON.value()));
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        void commitsARequestedLesson() {
            registration.register(UUID.randomUUID(), LESSON, "Algebra");

            assertAll(
                    () -> assertEquals(1, lessons.count(LESSON.value())),
                    () -> assertEquals(LessonStatus.REQUESTED, registration.status(LESSON)));
        }

        @Test
        void duplicateMessageNamesBusinessKey() {
            registration.register(UUID.randomUUID(), LESSON, "Algebra");

            DuplicateLesson duplicate = assertThrows(DuplicateLesson.class,
                    () -> registration.register(UUID.randomUUID(), LESSON, "Again"));

            assertEquals("duplicate lesson: LES-2026-0042", duplicate.getMessage());
        }

        @Test
        void blankSubjectIsRejectedWithoutWriting() {
            assertThrows(IllegalArgumentException.class,
                    () -> registration.register(UUID.randomUUID(), LESSON, "  "));

            assertEquals(0, lessons.count(LESSON.value()));
        }

        @Test
        void missingArgumentsAreRejected() {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> registration.register(null, LESSON, "Algebra")),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> registration.register(UUID.randomUUID(), null, "Algebra")),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> registration.register(UUID.randomUUID(), LESSON, null)));
        }
    }
}
