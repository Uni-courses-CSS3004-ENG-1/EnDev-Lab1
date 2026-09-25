package com.endev.tutoring.domain;

public interface Rule {

    void check(LessonStatus from, LessonStatus to);
}
