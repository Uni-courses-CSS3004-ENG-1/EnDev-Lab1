package com.endev.tutoring.service;

import com.endev.tutoring.entity.LessonEntity;
import com.endev.tutoring.exception.LessonNotFoundException;
import com.endev.tutoring.exception.LessonTransitionException;
import com.endev.tutoring.exception.NotYourLessonException;
import com.endev.tutoring.mapper.LessonMapper;
import com.endev.tutoring.model.Account;
import com.endev.tutoring.model.Lesson;
import com.endev.tutoring.model.LessonStatus;
import com.endev.tutoring.repository.LessonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

// Status changes on a stored lesson. The lesson rules stay plain Java in rule/; this class only
// loads the row, checks it is the caller's lesson, asks the rules, and writes the new status.
//
// | Move     | From -> to              | Who                                   | Money          |
// | confirm  | REQUESTED -> CONFIRMED  | TUTOR (the locked action)             | hold placed    |
// | complete | CONFIRMED -> COMPLETED  | TUTOR                                 | hold released  |
// | cancel   | REQUESTED/CONFIRMED ->  | the lesson's TUTOR or its STUDENT     | refund if held |
//
// Every @PreAuthorize below is the same role as its URL rule in SecurityConfig: a second lock.
@Service
public class LessonWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(LessonWorkflowService.class);

    private final LessonRepository lessons;
    private final LessonService moves;
    private final AccountService accounts;

    public LessonWorkflowService(LessonRepository lessons, LessonService moves, AccountService accounts) {
        this.lessons = lessons;
        this.moves = moves;
        this.accounts = accounts;
    }

    // The actor is the token's subject, passed in by the controller, never a field of a request body.
    @Transactional
    @PreAuthorize("hasRole('TUTOR')")
    public Lesson confirm(UUID id, String actor) {
        return move(id, actor, LessonStatus.CONFIRMED, false);
    }

    @Transactional
    @PreAuthorize("hasRole('TUTOR')")
    public Lesson complete(UUID id, String actor) {
        return move(id, actor, LessonStatus.COMPLETED, false);
    }

    // a student may cancel their own booking too
    @Transactional
    @PreAuthorize("hasAnyRole('STUDENT', 'TUTOR')")
    public Lesson cancel(UUID id, String actor) {
        return move(id, actor, LessonStatus.CANCELLED, true);
    }

    private Lesson move(UUID id, String actor, LessonStatus to, boolean studentMayDoIt) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor must not be blank");
        }
        LessonEntity row = lessons.find(id).orElseThrow(() -> new LessonNotFoundException(id));
        checkItIsTheirLesson(row, actor, studentMayDoIt);
        Lesson lesson = LessonMapper.toLesson(row);
        try {
            // the rules, then the payment call: hold, release or refund
            moves.move(lesson.businessKey(), lesson.status(), to);
        } catch (IllegalStateException refused) {
            throw new LessonTransitionException(refused.getMessage());
        }
        row.setStatus(LessonMapper.toText(to));
        lessons.flush();
        log.info("lesson {} {} by {}", lesson.businessKey().value(), to.name().toLowerCase(Locale.ROOT), actor);
        return LessonMapper.toLesson(row);
    }

    // On top of the role: a lesson booked with a tutor belongs to that tutor (and, for cancel, to its
    // student). A lesson with no tutor yet, made by POST /api/v1/lessons, is decided by the role alone.
    private void checkItIsTheirLesson(LessonEntity row, String actor, boolean studentMayDoIt) {
        if (row.getTutorId() == null) {
            return;
        }
        Account me = accounts.find(actor).orElse(null);
        boolean myTeaching = me != null && row.getTutorId().equals(me.tutorId());
        boolean myBooking = studentMayDoIt && me != null && row.getStudentId() != null
                && row.getStudentId().equals(me.studentId());
        if (!myTeaching && !myBooking) {
            throw new NotYourLessonException();
        }
    }
}
