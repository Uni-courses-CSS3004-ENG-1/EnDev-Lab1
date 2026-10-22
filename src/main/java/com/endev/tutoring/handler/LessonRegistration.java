package com.endev.tutoring.handler;

import com.endev.tutoring.domain.DuplicateLesson;
import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonStatus;
import com.endev.tutoring.persistence.LessonJpa;
import com.endev.tutoring.persistence.LessonMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.UUID;

@Service
public class LessonRegistration {

    private final LessonJpa lessons;

    public LessonRegistration(LessonJpa lessons) {
        this.lessons = lessons;
    }

    @Transactional
    public void register(UUID id, LessonId lesson, String subject, String tutor) {
        if (id == null) {
            throw new IllegalArgumentException("Lesson database id must not be null");
        }
        if (lesson == null) {
            throw new IllegalArgumentException("Lesson id must not be null");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Lesson subject must not be blank");
        }
        // the column is NOT NULL, and Hibernate would send a null instead of the default
        if (tutor == null || tutor.isBlank()) {
            throw new IllegalArgumentException("Lesson tutor must not be blank");
        }

        try {
            lessons.insert(LessonMapper.toEntity(id, lesson.value(), LessonStatus.REQUESTED, subject, tutor));
            // Flush inside the try: otherwise the INSERT runs at commit, outside this catch.
            lessons.flush();
        } catch (DataIntegrityViolationException e) {
            throwDuplicateOrRethrow(e, lesson);
        }
    }

    @Transactional
    public void insertTwice(LessonId lesson) {
        try {
            lessons.insert(LessonMapper.toEntity(UUID.randomUUID(), lesson.value(), LessonStatus.REQUESTED,
                    "Algebra", "Dana"));
            lessons.insert(LessonMapper.toEntity(UUID.randomUUID(), lesson.value(), LessonStatus.REQUESTED,
                    "Algebra again", "Dana"));
            // Flush inside the try so the duplicate reaches this method before the transaction ends.
            lessons.flush();
        } catch (DataIntegrityViolationException e) {
            throwDuplicateOrRethrow(e, lesson);
        }
    }

    @Transactional(readOnly = true)
    public LessonStatus status(LessonId lesson) {
        return LessonMapper.toStatus(lessons.statusText(lesson.value()));
    }

    private void throwDuplicateOrRethrow(DataIntegrityViolationException e, LessonId lesson) {
        // Only PostgreSQL's unique-key SQLState is a duplicate lesson; other integrity errors stay errors.
        Throwable cause = e.getMostSpecificCause();
        if (cause instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
            throw new DuplicateLesson(lesson.value());
        }
        throw e;
    }
}
