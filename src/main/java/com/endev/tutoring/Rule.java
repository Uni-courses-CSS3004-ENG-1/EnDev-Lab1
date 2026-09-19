package com.endev.tutoring;

public interface Rule {

    void check(LessonStatus from, LessonStatus to);
}
