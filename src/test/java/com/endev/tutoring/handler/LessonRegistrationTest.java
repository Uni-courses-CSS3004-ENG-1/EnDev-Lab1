package com.endev.tutoring.handler;

import com.endev.tutoring.config.Application;
import com.endev.tutoring.domain.DuplicateLesson;
import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonStatus;
import com.endev.tutoring.persistence.LessonJpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = Application.class)
class LessonRegistrationTest {

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

    @Test
    void secondStatementRollsBack() {
        assertThrows(DuplicateLesson.class,
                () -> registration.insertTwice(new LessonId("LES-2026-0042")));
        assertEquals(0, lessons.count("LES-2026-0042"));
    }

    @Test
    void secondRequestKeepsTheFirst() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        registration.register(id1, new LessonId("LES-2026-0042"), "Algebra");
        assertThrows(DuplicateLesson.class,
                () -> registration.register(id2, new LessonId("LES-2026-0042"), "Again"));
        assertEquals(1, lessons.count("LES-2026-0042"));
    }

    @Test
    void oneRegisterCommitsRequestedLesson() {
        registration.register(UUID.randomUUID(), new LessonId("LES-2026-0042"), "Algebra");

        assertEquals(1, lessons.count("LES-2026-0042"));
        assertEquals(LessonStatus.REQUESTED, registration.status(new LessonId("LES-2026-0042")));
    }

    @Test
    void duplicateLessonMessageNamesBusinessKey() {
        LessonId lesson = new LessonId("LES-2026-0042");
        registration.register(UUID.randomUUID(), lesson, "Algebra");

        DuplicateLesson duplicate = assertThrows(DuplicateLesson.class,
                () -> registration.register(UUID.randomUUID(), lesson, "Again"));

        assertEquals("duplicate lesson: LES-2026-0042", duplicate.getMessage());
    }

    @Test
    void blankSubjectIsRejectedWithoutWriting() {
        assertThrows(IllegalArgumentException.class,
                () -> registration.register(UUID.randomUUID(), new LessonId("LES-2026-0042"), "  "));

        assertEquals(0, lessons.count("LES-2026-0042"));
    }
}
