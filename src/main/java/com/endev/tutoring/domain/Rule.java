package com.endev.tutoring.domain;

public interface Rule {

    // throws IllegalStateException if the move is not allowed
    void check(LessonStatus from, LessonStatus to);
}
