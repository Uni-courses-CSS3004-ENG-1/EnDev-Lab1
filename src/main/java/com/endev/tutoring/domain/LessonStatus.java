package com.endev.tutoring.domain;

/** Where a lesson stands, from the student asking for a slot to the tutor being paid. */
public enum LessonStatus {

    /** The student asked a tutor for a time slot. No money has moved yet. */
    REQUESTED,

    /** The tutor accepted the slot and the platform holds the student's payment. */
    CONFIRMED,

    /** The lesson took place: the tutor is paid and the platform keeps its commission. Final. */
    COMPLETED,

    /** The lesson was called off before it took place; any held payment went back to the student. Final. */
    CANCELLED
}
