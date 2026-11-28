package com.endev.tutoring.dto;

import com.endev.tutoring.mapper.LessonMapper;
import com.endev.tutoring.model.LessonView;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// A lesson with names and price, for the booking pages. LessonResponse stays as Lab 4 defined it.
public record LessonDetailsResponse(UUID id, String businessKey, String status, String subject,
                                    OffsetDateTime startsAt, UUID tutorId, String tutorName,
                                    UUID studentId, String studentName, BigDecimal hourlyRate) {

    public static LessonDetailsResponse from(LessonView view) {
        var lesson = view.lesson();
        return new LessonDetailsResponse(lesson.id(), lesson.businessKey().value(),
                LessonMapper.toText(lesson.status()), lesson.subject(), lesson.startsAt(),
                lesson.tutorId(), view.tutorName(), lesson.studentId(), view.studentName(), view.hourlyRate());
    }
}
