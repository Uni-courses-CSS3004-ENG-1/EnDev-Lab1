package com.endev.tutoring.persistence;

import com.endev.tutoring.domain.Lesson;
import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonStatus;

import java.util.UUID;

public final class LessonMapper {

    private LessonMapper() {
    }

    public static LessonEntity toEntity(UUID id, String businessKey, LessonStatus status, String subject,
                                        String tutor) {
        LessonEntity lesson = new LessonEntity();
        lesson.setId(id);
        lesson.setBusinessKey(businessKey);
        lesson.setStatus(toText(status));
        lesson.setSubject(subject);
        lesson.setTutor(tutor);
        return lesson;
    }

    // the entity never leaves persistence; callers get the domain view
    public static Lesson toLesson(LessonEntity lesson) {
        return new Lesson(lesson.getId(), new LessonId(lesson.getBusinessKey()), toStatus(lesson.getStatus()),
                lesson.getSubject());
    }

    public static String toText(LessonStatus status) {
        return switch (status) {
            case REQUESTED -> "REQUESTED";
            case CONFIRMED -> "CONFIRMED";
            case COMPLETED -> "COMPLETED";
            case CANCELLED -> "CANCELLED";
        };
    }

    public static LessonStatus toStatus(String text) {
        if (text == null) {
            throw unknownStatus(text);
        }
        return switch (text) {
            case "REQUESTED" -> LessonStatus.REQUESTED;
            case "CONFIRMED" -> LessonStatus.CONFIRMED;
            case "COMPLETED" -> LessonStatus.COMPLETED;
            case "CANCELLED" -> LessonStatus.CANCELLED;
            default -> throw unknownStatus(text);
        };
    }

    private static IllegalStateException unknownStatus(String text) {
        return new IllegalStateException("Unknown lesson status in the database: " + text);
    }
}
