package com.endev.tutoring.service;

import com.endev.tutoring.entity.LessonEntity;
import com.endev.tutoring.exception.LessonNotFoundException;
import com.endev.tutoring.exception.LessonTransitionException;
import com.endev.tutoring.mapper.LessonMapper;
import com.endev.tutoring.model.Lesson;
import com.endev.tutoring.model.LessonStatus;
import com.endev.tutoring.repository.LessonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// Status changes on a stored lesson. The lesson rules stay plain Java in rule/; this class only
// loads the row, asks the rules, and writes the new status.
@Service
public class LessonWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(LessonWorkflowService.class);

    private final LessonRepository lessons;
    private final LessonService moves;

    public LessonWorkflowService(LessonRepository lessons, LessonService moves) {
        this.lessons = lessons;
        this.moves = moves;
    }

    // REQUESTED -> CONFIRMED and the payment hold. Only a tutor: the same role as the URL rule.
    // The actor is the token's subject, passed in by the controller, never a field of a request body.
    @Transactional
    @PreAuthorize("hasRole('TUTOR')")
    public Lesson confirm(UUID id, String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor must not be blank");
        }
        LessonEntity row = lessons.find(id).orElseThrow(() -> new LessonNotFoundException(id));
        Lesson lesson = LessonMapper.toLesson(row);
        try {
            moves.move(lesson.businessKey(), lesson.status(), LessonStatus.CONFIRMED);
        } catch (IllegalStateException refused) {
            throw new LessonTransitionException(refused.getMessage());
        }
        row.setStatus(LessonMapper.toText(LessonStatus.CONFIRMED));
        lessons.flush();
        log.info("lesson {} confirmed by {}", lesson.businessKey().value(), actor);
        return LessonMapper.toLesson(row);
    }
}
