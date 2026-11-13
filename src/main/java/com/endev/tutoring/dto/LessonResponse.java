package com.endev.tutoring.dto;

import com.endev.tutoring.model.Lesson;
import com.endev.tutoring.mapper.LessonMapper;

import java.util.UUID;

// What a client sees of a lesson. Built from the model Lesson, never from the JPA entity,
// so created_at stays off the wire and the table shape stays private.
public record LessonResponse(UUID id, String businessKey, String status, String subject,
                             UUID tutorId, UUID studentId) {

    public static LessonResponse from(Lesson lesson) {
        // the status is the same token the CHECK constraint stores
        return new LessonResponse(lesson.id(), lesson.businessKey().value(),
                LessonMapper.toText(lesson.status()), lesson.subject(), lesson.tutorId(), lesson.studentId());
    }
}
