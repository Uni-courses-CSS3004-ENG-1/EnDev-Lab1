package com.endev.tutoring.service;

import com.endev.tutoring.entity.LessonEntity;
import com.endev.tutoring.entity.TutorEntity;
import com.endev.tutoring.exception.DuplicateLessonException;
import com.endev.tutoring.exception.LessonNotFoundException;
import com.endev.tutoring.exception.StudentNotFoundException;
import com.endev.tutoring.exception.TutorNotFoundException;
import com.endev.tutoring.mapper.LessonMapper;
import com.endev.tutoring.model.Lesson;
import com.endev.tutoring.model.LessonId;
import com.endev.tutoring.model.LessonStatus;
import com.endev.tutoring.repository.LessonRepository;
import com.endev.tutoring.repository.StudentRepository;
import com.endev.tutoring.repository.TutorRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LessonRegistrationService {

    // the V2 column default: a new request has no tutor yet
    static final String NO_TUTOR_YET = "UNASSIGNED";

    private final LessonRepository lessons;
    private final TutorRepository tutors;
    private final StudentRepository students;

    public LessonRegistrationService(LessonRepository lessons, TutorRepository tutors, StudentRepository students) {
        this.lessons = lessons;
        this.tutors = tutors;
        this.students = students;
    }

    @Transactional
    public void register(UUID id, LessonId lesson, String subject, String tutor) {
        insert(id, lesson, subject, tutor, null, null);
    }

    // A new request: Java picks the UUID, the row starts REQUESTED, and the write is the Lab 3 register.
    @Transactional
    public Lesson create(LessonId lesson, String subject) {
        return create(lesson, subject, null, null);
    }

    // Same, with an optional tutor and student. An unknown id is a 404, and nothing is written.
    @Transactional
    public Lesson create(LessonId lesson, String subject, UUID tutorId, UUID studentId) {
        String tutorName = NO_TUTOR_YET;
        if (tutorId != null) {
            TutorEntity tutor = tutors.find(tutorId).orElseThrow(() -> new TutorNotFoundException(tutorId));
            tutorName = tutor.getName();
        }
        if (studentId != null && students.find(studentId).isEmpty()) {
            throw new StudentNotFoundException(studentId);
        }
        UUID id = UUID.randomUUID();
        insert(id, lesson, subject, tutorName, tutorId, studentId);
        return new Lesson(id, lesson, LessonStatus.REQUESTED, subject, tutorId, studentId);
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

    private void insert(UUID id, LessonId lesson, String subject, String tutor, UUID tutorId, UUID studentId) {
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

        LessonEntity row = LessonMapper.toEntity(id, lesson.value(), LessonStatus.REQUESTED, subject, tutor);
        row.setTutorId(tutorId);
        row.setStudentId(studentId);
        try {
            lessons.insert(row);
            // Flush inside the try: otherwise the INSERT runs at commit, outside this catch.
            lessons.flush();
        } catch (DataIntegrityViolationException e) {
            throwDuplicateOrRethrow(e, lesson);
        }
    }

    private void throwDuplicateOrRethrow(DataIntegrityViolationException e, LessonId lesson) {
        // Only the unique-key SQLState is a duplicate lesson; other integrity errors stay errors.
        if (UniqueViolation.is(e)) {
            throw new DuplicateLessonException(lesson.value());
        }
        throw e;
    }
}
