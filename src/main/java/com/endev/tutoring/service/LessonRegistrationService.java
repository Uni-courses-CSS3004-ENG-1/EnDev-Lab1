package com.endev.tutoring.service;

import com.endev.tutoring.exception.DuplicateLessonException;
import com.endev.tutoring.model.Lesson;
import com.endev.tutoring.model.LessonId;
import com.endev.tutoring.exception.LessonNotFoundException;
import com.endev.tutoring.model.LessonStatus;
import com.endev.tutoring.repository.LessonRepository;
import com.endev.tutoring.mapper.LessonMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.UUID;

@Service
public class LessonRegistrationService {

    // the V2 column default: a new request has no tutor yet
    static final String NO_TUTOR_YET = "UNASSIGNED";

    private final LessonRepository lessons;

    public LessonRegistrationService(LessonRepository lessons) {
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

    // A new request: Java picks the UUID, the row starts REQUESTED, and the write is the Lab 3 register.
    @Transactional
    public Lesson create(LessonId lesson, String subject) {
        UUID id = UUID.randomUUID();
        register(id, lesson, subject, NO_TUTOR_YET);
        return new Lesson(id, lesson, LessonStatus.REQUESTED, subject);
    }

    // Reads one row by its UUID. It never registers, moves or flushes.
    @Transactional(readOnly = true)
    public Lesson find(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Lesson database id must not be null");
        }
        return lessons.find(id)
                .map(LessonMapper::toLesson)
                .orElseThrow(() -> new LessonNotFoundException(id));
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
            throw new DuplicateLessonException(lesson.value());
        }
        throw e;
    }
}
