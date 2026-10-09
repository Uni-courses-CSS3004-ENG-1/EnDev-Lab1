package com.endev.tutoring.handler;

import com.endev.tutoring.domain.DuplicateLesson;
import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonStatus;
import com.endev.tutoring.persistence.LessonJdbc;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LessonRegistry {

    private final LessonJdbc lessons;

    public LessonRegistry(LessonJdbc lessons) {
        this.lessons = lessons;
    }

    // One transaction per call. If it returns, the lesson is committed.
    @Transactional
    public void register(UUID id, LessonId number, String title) {
        insertOnce(id, number, title);
    }

    // ONE transaction with two inserts. The second one hits the UNIQUE key,
    // DuplicateLesson leaves the method, Spring rolls back, so the first insert is gone too.
    @Transactional
    public void insertTwice(LessonId number) {
        insertOnce(UUID.randomUUID(), number, "Algebra with Dana");
        insertOnce(UUID.randomUUID(), number, "Algebra again");
    }

    private void insertOnce(UUID id, LessonId number, String title) {
        try {
            lessons.insert(id, number, LessonStatus.REQUESTED, title);
        } catch (DuplicateKeyException ex) {
            // PostgreSQL SQLState 23505. Rethrow as a domain error, never swallow it:
            // a catch that returns normally would COMMIT the first insert.
            throw new DuplicateLesson(number);
        }
    }
}
