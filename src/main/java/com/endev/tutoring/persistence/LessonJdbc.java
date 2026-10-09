package com.endev.tutoring.persistence;

import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// All SQL for the lesson table lives here. Nobody outside this package
// sees a ResultSet or a Connection.
@Repository
public class LessonJdbc {

    private final JdbcTemplate jdbc;

    public LessonJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UUID id, LessonId number, LessonStatus status, String title) {
        jdbc.update("""
                INSERT INTO lesson (id, business_key, status, title)
                VALUES (?, ?, ?, ?)
                """, id, number.value(), status.name(), title);
    }

    public int count(LessonId number) {
        Integer n = jdbc.queryForObject(
                "SELECT count(*) FROM lesson WHERE business_key = ?",
                Integer.class,
                number.value());
        return n == null ? 0 : n;
    }

    public Optional<LessonStatus> findStatus(LessonId number) {
        List<String> found = jdbc.queryForList(
                "SELECT status FROM lesson WHERE business_key = ?",
                String.class,
                number.value());
        return found.stream().findFirst().map(LessonJdbc::toStatus);
    }

    // text column -> enum. Unknown text is an error, not a new status.
    static LessonStatus toStatus(String text) {
        try {
            return LessonStatus.valueOf(text);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unknown lesson status in database: " + text, e);
        }
    }
}
