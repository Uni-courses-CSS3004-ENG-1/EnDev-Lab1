package com.endev.tutoring.persistence;

import com.endev.tutoring.config.Application;
import com.endev.tutoring.domain.LessonStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.dao.PersistenceExceptionTranslationAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = Application.class)
@Import(LessonJpa.class)
@ImportAutoConfiguration(PersistenceExceptionTranslationAutoConfiguration.class)
class LessonJpaTest {

    @Autowired
    private LessonJpa lessons;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanRows() {
        jdbc.update("DELETE FROM lesson");
    }

    @Test
    void persistedLessonIsCountedAfterFlush() {
        lessons.insert(lesson("LES-2026-0042", LessonStatus.REQUESTED));
        lessons.flush();

        assertEquals(1, lessons.count("LES-2026-0042"));
    }

    @Test
    void unknownKeyCountsZero() {
        assertEquals(0, lessons.count("LES-2026-9999"));
    }

    @ParameterizedTest
    @EnumSource(LessonStatus.class)
    void everyStatusIsStoredAsTheCheckWord(LessonStatus status) {
        lessons.insert(lesson("LES-2026-0042", status));
        lessons.flush();

        assertEquals(LessonMapper.toText(status), jdbc.queryForObject(
                "SELECT status FROM lesson WHERE business_key = ?", String.class, "LES-2026-0042"));
    }

    @Test
    void keyIsBoundAsAValue() {
        String sneaky = "x' OR '1'='1";
        lessons.insert(lesson("LES-2026-0042", LessonStatus.REQUESTED));
        lessons.insert(lesson(sneaky, LessonStatus.REQUESTED));
        lessons.flush();

        assertEquals(1, lessons.count(sneaky));
    }

    @Test
    void duplicateKeyDoesNotFailUntilFlush() {
        lessons.insert(lesson("LES-2026-0042", LessonStatus.REQUESTED));
        lessons.flush();

        assertDoesNotThrow(() -> lessons.insert(lesson("LES-2026-0042", LessonStatus.REQUESTED)));
        assertThrows(DataIntegrityViolationException.class, lessons::flush);
    }

    @Test
    void databaseCheckStopsUnknownStatusOnFlush() {
        LessonEntity lesson = lesson("LES-2026-0042", LessonStatus.REQUESTED);
        lesson.setStatus("BANANA");

        lessons.insert(lesson);

        assertThrows(DataIntegrityViolationException.class, lessons::flush);
    }

    private static LessonEntity lesson(String businessKey, LessonStatus status) {
        return LessonMapper.toEntity(UUID.randomUUID(), businessKey, status, "Algebra", "Dana");
    }
}
