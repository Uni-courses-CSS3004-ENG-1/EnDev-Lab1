package com.endev.tutoring.model;

import java.math.BigDecimal;

// A lesson with the names a person wants to read on a card: who teaches, who learns, and the price.
// studentName and hourlyRate are null when the lesson has no student or tutor linked.
public record LessonView(Lesson lesson, String tutorName, String studentName, BigDecimal hourlyRate) {
}
