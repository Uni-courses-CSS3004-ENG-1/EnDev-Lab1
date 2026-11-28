package com.endev.tutoring.repository;

import com.endev.tutoring.TutoringApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TutoringApplication.class)
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void allMigrationsAreRecordedAsSuccessfulInOrder() {
        List<Map<String, Object>> history = jdbc.queryForList("""
                SELECT version, description, success
                FROM flyway_schema_history
                WHERE version IS NOT NULL
                ORDER BY installed_rank
                """);

        assertEquals(6, history.size());
        assertAll(
                () -> assertEquals("1", history.get(0).get("version")),
                () -> assertEquals("create lesson", history.get(0).get("description")),
                () -> assertEquals(Boolean.TRUE, history.get(0).get("success")),
                () -> assertEquals("2", history.get(1).get("version")),
                () -> assertEquals("add tutor", history.get(1).get("description")),
                () -> assertEquals(Boolean.TRUE, history.get(1).get("success")),
                () -> assertEquals("3", history.get(2).get("version")),
                () -> assertEquals("create tutor and student", history.get(2).get("description")),
                () -> assertEquals(Boolean.TRUE, history.get(2).get("success")),
                () -> assertEquals("4", history.get(3).get("version")),
                () -> assertEquals("create app user", history.get(3).get("description")),
                () -> assertEquals("5", history.get(4).get("version")),
                () -> assertEquals("add tutor bio", history.get(4).get("description")),
                () -> assertEquals("6", history.get(5).get("version")),
                () -> assertEquals("add lesson starts at", history.get(5).get("description")),
                () -> assertTrue(history.stream().allMatch(row -> Boolean.TRUE.equals(row.get("success")))));
    }

    @Test
    void oldSchemaSqlIsNotOnTheClasspath() {
        assertFalse(new ClassPathResource("db/schema.sql").exists());
    }
}
