package com.endev.tutoring.rule;

import com.endev.tutoring.model.LessonStatus;

public interface Rule {

    // throws IllegalStateException if the move is not allowed
    void check(LessonStatus from, LessonStatus to);
}
