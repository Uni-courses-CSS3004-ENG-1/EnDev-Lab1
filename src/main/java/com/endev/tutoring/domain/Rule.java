package com.endev.tutoring.domain;

/** One business rule about a status change. It throws IllegalStateException when the change is forbidden. */
public interface Rule {

    void check(LessonStatus from, LessonStatus to);
}
