package com.endev.tutoring.service;

import com.endev.tutoring.entity.LessonEntity;
import com.endev.tutoring.exception.SlotTakenException;
import com.endev.tutoring.mapper.LessonMapper;
import com.endev.tutoring.model.Account;
import com.endev.tutoring.model.Lesson;
import com.endev.tutoring.model.LessonId;
import com.endev.tutoring.model.LessonView;
import com.endev.tutoring.model.Student;
import com.endev.tutoring.model.Tutor;
import com.endev.tutoring.repository.LessonRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

// A student books a tutor for a time, and anyone signed in sees their own lessons.
// The student is always the caller's own account, found from the token's username: never a JSON field.
@Service
public class BookingService {

    // every lesson is one hour; two lessons of one tutor may not overlap
    static final Duration LESSON_LENGTH = Duration.ofHours(1);

    private final AccountService accounts;
    private final TutorService tutors;
    private final StudentService students;
    private final LessonRegistrationService registration;
    private final LessonRepository lessons;

    public BookingService(AccountService accounts, TutorService tutors, StudentService students,
                          LessonRegistrationService registration, LessonRepository lessons) {
        this.accounts = accounts;
        this.tutors = tutors;
        this.students = students;
        this.registration = registration;
        this.lessons = lessons;
    }

    // Students only: the same role as the URL rule, a second lock on the same door.
    @Transactional
    @PreAuthorize("hasRole('STUDENT')")
    public LessonView book(String username, UUID tutorId, String subject, OffsetDateTime startsAt) {
        Account me = accounts.require(username);
        Tutor tutor = tutors.find(tutorId);
        long clashes = lessons.countActiveBetween(tutorId,
                startsAt.minus(LESSON_LENGTH), startsAt.plus(LESSON_LENGTH));
        if (clashes > 0) {
            throw new SlotTakenException();
        }
        Lesson lesson = registration.create(newBusinessKey(), subject, tutorId, me.studentId(), startsAt);
        Student student = students.find(me.studentId());
        return new LessonView(lesson, tutor.name(), student.name(), tutor.hourlyRate());
    }

    // A tutor sees the lessons booked with them; a student sees the lessons they booked.
    @Transactional(readOnly = true)
    public List<LessonView> mine(String username) {
        Account me = accounts.require(username);
        List<LessonEntity> rows = me.isTutor() ? lessons.findByTutor(me.tutorId())
                : lessons.findByStudent(me.studentId());

        // look each tutor and student up once, however many lessons they share
        Map<UUID, Tutor> tutorById = new HashMap<>();
        Map<UUID, String> studentName = new HashMap<>();
        return rows.stream().map(row -> {
            Tutor tutor = row.getTutorId() == null ? null
                    : tutorById.computeIfAbsent(row.getTutorId(), tutors::find);
            String learner = row.getStudentId() == null ? null
                    : studentName.computeIfAbsent(row.getStudentId(), id -> students.find(id).name());
            BigDecimal price = tutor == null ? null : tutor.hourlyRate();
            return new LessonView(LessonMapper.toLesson(row), row.getTutor(), learner, price);
        }).toList();
    }

    // LES-2026-7F3A9C21: the year and 8 random hex characters. The UNIQUE column is the final guard.
    static LessonId newBusinessKey() {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return new LessonId("LES-" + Year.now() + "-" + random);
    }
}
