package com.endev.tutoring.dto;

import com.endev.tutoring.model.Student;

import java.util.UUID;

public record StudentResponse(UUID id, String name, String email) {

    public static StudentResponse from(Student student) {
        return new StudentResponse(student.id(), student.name(), student.email());
    }
}
