package com.endev.tutoring.persistence;

import com.endev.tutoring.config.Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(classes = Application.class)
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void v1MigrationIsRecordedAsSuccessful() {
        List<Map<String, Object>> history = jdbc.queryForList("""
                SELECT version, description, success
                FROM flyway_schema_history
                ORDER BY installed_rank
                """);

        assertEquals(1, history.size());
        assertEquals("1", history.get(0).get("version"));
        assertEquals("create lesson", history.get(0).get("description"));
        assertEquals(Boolean.TRUE, history.get(0).get("success"));
    }

    @Test
    void oldSchemaSqlIsNotOnTheClasspath() {
        assertFalse(new ClassPathResource("db/schema.sql").exists());
    }
}
