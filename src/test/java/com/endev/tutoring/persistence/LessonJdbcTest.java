package com.endev.tutoring.persistence;

import com.endev.tutoring.config.Application;
import com.endev.tutoring.domain.LessonStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Runs against the classroom PostgreSQL. Each insert really commits for the test.
@SpringBootTest(classes = Application.class)
class LessonJdbcTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private LessonJdbc lessons;

    @BeforeEach
    void recreateSchema() throws Exception {
        jdbc.execute("DROP TABLE IF EXISTS lesson");
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/schema.sql")));
    }

    @Test
    void insertedLessonIsCounted() {
        lessons.insert(UUID.randomUUID(), "LES-2026-0042", LessonStatus.REQUESTED, "Algebra");

        assertEquals(1, lessons.count("LES-2026-0042"));
    }

    @Test
    void unknownKeyCountsZero() {
        assertEquals(0, lessons.count("LES-2026-9999"));
    }

    @ParameterizedTest
    @EnumSource(LessonStatus.class)
    void statusIsReadBack(LessonStatus status) {
        lessons.insert(UUID.randomUUID(), "LES-2026-0042", status, "Algebra");

        assertEquals(status, lessons.status("LES-2026-0042"));
    }

    // the placeholder binds the key as a value, so SQL inside it is just text
    @Test
    void keyIsBoundAsAValue() {
        String sneaky = "x' OR '1'='1";
        lessons.insert(UUID.randomUUID(), "LES-2026-0042", LessonStatus.REQUESTED, "Algebra");
        lessons.insert(UUID.randomUUID(), sneaky, LessonStatus.REQUESTED, "Algebra");

        assertEquals(1, lessons.count(sneaky));
    }

    @Test
    void duplicateKeyReachesTheCallerAsDataIntegrityViolation() {
        lessons.insert(UUID.randomUUID(), "LES-2026-0042", LessonStatus.REQUESTED, "Algebra");

        assertThrows(DataIntegrityViolationException.class,
                () -> lessons.insert(UUID.randomUUID(), "LES-2026-0042", LessonStatus.REQUESTED, "Again"));
        assertEquals(1, lessons.count("LES-2026-0042"));
    }

    // the CHECK normally stops this; drop it to see what the mapper does with a status it does not know
    @Test
    void unknownStatusTextThrows() {
        jdbc.execute("ALTER TABLE lesson DROP CONSTRAINT lesson_status_known");
        jdbc.update("INSERT INTO lesson (id, business_key, status, subject) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), "LES-2026-0042", "PAID", "Algebra");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> lessons.status("LES-2026-0042"));
        assertEquals("Unknown lesson status in the database: PAID", e.getMessage());
    }
}
