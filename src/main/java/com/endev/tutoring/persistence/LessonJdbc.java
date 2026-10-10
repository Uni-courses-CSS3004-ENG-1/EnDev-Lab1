package com.endev.tutoring.persistence;

import com.endev.tutoring.domain.LessonStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

// The only place that writes SQL for the lesson table (src/main/resources/db/schema.sql).
// Callers never see a Connection or a ResultSet.
@Repository
public class LessonJdbc {

    private final JdbcTemplate jdbc;

    public LessonJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UUID id, String businessKey, LessonStatus status, String subject) {
        jdbc.update("""
                INSERT INTO lesson (id, business_key, status, subject)
                VALUES (?, ?, ?, ?)
                """, id, businessKey, toText(status), subject);
    }

    public int count(String businessKey) {
        Integer n = jdbc.queryForObject(
                "SELECT count(*) FROM lesson WHERE business_key = ?",
                Integer.class,
                businessKey);
        return n == null ? 0 : n;
    }

    public LessonStatus status(String businessKey) {
        String text = jdbc.queryForObject(
                "SELECT status FROM lesson WHERE business_key = ?",
                String.class,
                businessKey);
        return toStatus(text);
    }

    private static String toText(LessonStatus status) {
        return switch (status) {
            case REQUESTED -> "REQUESTED";
            case CONFIRMED -> "CONFIRMED";
            case COMPLETED -> "COMPLETED";
            case CANCELLED -> "CANCELLED";
        };
    }

    // an unknown text is an error, it never becomes a new status
    private static LessonStatus toStatus(String text) {
        if (text == null) {
            throw new IllegalStateException("Lesson status in the database is null");
        }
        return switch (text) {
            case "REQUESTED" -> LessonStatus.REQUESTED;
            case "CONFIRMED" -> LessonStatus.CONFIRMED;
            case "COMPLETED" -> LessonStatus.COMPLETED;
            case "CANCELLED" -> LessonStatus.CANCELLED;
            default -> throw new IllegalStateException("Unknown lesson status in the database: " + text);
        };
    }
}
