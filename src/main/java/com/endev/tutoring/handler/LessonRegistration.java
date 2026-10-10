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
public class LessonRegistration {

    private final LessonJdbc lessons;

    public LessonRegistration(LessonJdbc lessons) {
        this.lessons = lessons;
    }

    @Transactional
    public void register(UUID id, LessonId lesson, String subject) {
        if (id == null) {
            throw new IllegalArgumentException("Lesson database id must not be null");
        }
        if (lesson == null) {
            throw new IllegalArgumentException("Lesson id must not be null");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Lesson subject must not be blank");
        }

        try {
            lessons.insert(id, lesson.value(), LessonStatus.REQUESTED, subject);
        } catch (DuplicateKeyException e) {
            // Only a duplicate-key error becomes DuplicateLesson; CHECK and NOT NULL errors do not.
            throw new DuplicateLesson(lesson.value());
        }
    }

    @Transactional
    public void insertTwice(LessonId lesson) {
        try {
            lessons.insert(UUID.randomUUID(), lesson.value(), LessonStatus.REQUESTED, "Algebra");
            lessons.insert(UUID.randomUUID(), lesson.value(), LessonStatus.REQUESTED, "Algebra again");
        } catch (DuplicateKeyException e) {
            // Only a duplicate-key error becomes DuplicateLesson; CHECK and NOT NULL errors do not.
            throw new DuplicateLesson(lesson.value());
        }
    }
}
