package com.endev.tutoring.persistence;

import com.endev.tutoring.config.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = Application.class)
class SchemaTest {

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void recreateSchema() throws Exception {
        jdbc.execute("DROP TABLE IF EXISTS lesson");
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/schema.sql")));
    }

    @Test
    void validLessonCanBeInserted() {
        String businessKey = "LES-2026-0042";

        jdbc.update("""
                INSERT INTO lesson (id, business_key, status, subject)
                VALUES (?, ?, ?, ?)
                """, UUID.randomUUID(), businessKey, "REQUESTED", "Algebra");

        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM lesson WHERE business_key = ?", Integer.class, businessKey);

        assertEquals(1, count);
    }

    @Test
    void unknownStatusIsRejected() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO lesson (id, business_key, status, subject)
                VALUES (?, ?, ?, ?)
                """, UUID.randomUUID(), "LES-2026-0042", "PAID", "Algebra"));
    }

    @Test
    void nullBusinessKeyIsRejected() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO lesson (id, business_key, status, subject)
                VALUES (?, ?, ?, ?)
                """, UUID.randomUUID(), null, "REQUESTED", "Algebra"));
    }

    @Test
    void duplicateBusinessKeyIsRejected() {
        String businessKey = "LES-2026-0042";
        String insert = """
                INSERT INTO lesson (id, business_key, status, subject)
                VALUES (?, ?, ?, ?)
                """;

        jdbc.update(insert, UUID.randomUUID(), businessKey, "REQUESTED", "Algebra");

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(insert, UUID.randomUUID(), businessKey, "CONFIRMED", "Algebra"));
    }
}
