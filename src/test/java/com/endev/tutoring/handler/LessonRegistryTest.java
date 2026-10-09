package com.endev.tutoring.handler;

import com.endev.tutoring.config.Application;
import com.endev.tutoring.domain.DuplicateLesson;
import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonStatus;
import com.endev.tutoring.persistence.LessonJdbc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Needs PostgreSQL on localhost (docker compose up -d).
// No @Transactional here: the test must see what production would really commit.
@SpringBootTest(classes = Application.class)
class LessonRegistryTest {

    private static final LessonId NUMBER = new LessonId("LES-2026-0042");

    @Autowired
    private LessonRegistry registry;

    @Autowired
    private LessonJdbc lessons;

    @Autowired
    private JdbcTemplate jdbc;

    // Start from an empty database: build the table from schema.sql every time
    @BeforeEach
    void cleanDatabase() throws IOException {
        jdbc.execute("DROP TABLE IF EXISTS lesson");
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/schema.sql")));
    }

    @Test
    void secondStatementRollsBack() {
        assertThrows(DuplicateLesson.class, () -> registry.insertTwice(NUMBER));
        assertEquals(0, lessons.count(NUMBER));
    }

    @Test
    void secondRequestKeepsTheFirst() {
        registry.register(UUID.randomUUID(), NUMBER, "Algebra with Dana");
        assertThrows(DuplicateLesson.class,
                () -> registry.register(UUID.randomUUID(), NUMBER, "Algebra again"));
        assertEquals(1, lessons.count(NUMBER));
    }

    @Test
    void registeredLessonStartsAsRequested() {
        registry.register(UUID.randomUUID(), NUMBER, "Algebra with Dana");
        assertEquals(Optional.of(LessonStatus.REQUESTED), lessons.findStatus(NUMBER));
    }

    @Test
    void unknownLessonHasNoStatusAndCountZero() {
        assertEquals(0, lessons.count(NUMBER));
        assertEquals(Optional.empty(), lessons.findStatus(NUMBER));
    }

    @Test
    void differentNumbersAreBothKept() {
        registry.register(UUID.randomUUID(), NUMBER, "Algebra with Dana");
        LessonId other = new LessonId("LES-2026-0043");
        registry.register(UUID.randomUUID(), other, "Physics with Sam");
        assertEquals(1, lessons.count(NUMBER));
        assertEquals(1, lessons.count(other));
    }

    // The database defends itself even if Java is bypassed

    @Test
    void databaseRejectsAnUnknownStatus() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO lesson (id, business_key, status, title) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), "LES-2026-0099", "PAID", "Chemistry"));
    }

    @Test
    void databaseRejectsANullBusinessKey() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO lesson (id, business_key, status, title) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), null, "REQUESTED", "Chemistry"));
    }
}
