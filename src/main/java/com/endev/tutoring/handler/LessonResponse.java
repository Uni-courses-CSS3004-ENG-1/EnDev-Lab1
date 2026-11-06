package com.endev.tutoring.handler;

import com.endev.tutoring.domain.Lesson;
import com.endev.tutoring.persistence.LessonMapper;

import java.util.UUID;

// What a client sees of a lesson. Built from the domain Lesson, never from the JPA entity,
// so tutor and created_at stay off the wire and the table shape stays private.
public record LessonResponse(UUID id, String businessKey, String status, String subject) {

    public static LessonResponse from(Lesson lesson) {
        // the status is the same token the CHECK constraint stores
        return new LessonResponse(lesson.id(), lesson.businessKey().value(),
                LessonMapper.toText(lesson.status()), lesson.subject());
    }
}
