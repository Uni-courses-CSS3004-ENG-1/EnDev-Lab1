package com.endev.tutoring.handler;

import com.endev.tutoring.config.Application;
import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.persistence.LessonJpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

// PostgreSQL integration test: it is deliberately not @Transactional.
@SpringBootTest(classes = Application.class)
class TransactionIsolationTest {

    private static final LessonId LESSON = new LessonId("LES-2026-0042");

    @Autowired
    private LessonRegistration registration;

    @Autowired
    private LessonJpa lessons;

    @Autowired
    private JdbcTemplate jdbc;

    private TransactionTemplate transactionTemplate;

    @Autowired
    void setTransactionManager(PlatformTransactionManager transactionManager) {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @BeforeEach
    void cleanRows() {
        jdbc.update("DELETE FROM lesson");
    }

    @Test
    void defaultIsolationIsReadCommitted() {
        assertEquals("read committed", jdbc.queryForObject("SHOW transaction_isolation", String.class));
    }

    @Test
    void uncommittedLessonIsInvisibleToAnotherTransaction() throws Exception {
        transactionTemplate.executeWithoutResult(status -> {
            registration.register(UUID.randomUUID(), LESSON, "Algebra", "Dana");
            assertEquals(0, countFromAnotherThread());
        });

        assertEquals(1, lessons.count(LESSON.value()));
    }

    @Test
    void rolledBackLessonIsNeverSeen() throws Exception {
        transactionTemplate.executeWithoutResult(status -> {
            registration.register(UUID.randomUUID(), LESSON, "Algebra", "Dana");
            assertEquals(0, countFromAnotherThread());
            status.setRollbackOnly();
        });

        assertEquals(0, lessons.count(LESSON.value()));
    }

    private int countFromAnotherThread() {
        try {
            return CompletableFuture
                    .supplyAsync(() -> lessons.count(LESSON.value()))
                    .get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new AssertionError("Could not read from the second transaction", e);
        }
    }
}
