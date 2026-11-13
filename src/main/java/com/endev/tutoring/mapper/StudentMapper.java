package com.endev.tutoring.mapper;

import com.endev.tutoring.entity.StudentEntity;
import com.endev.tutoring.model.Student;

public final class StudentMapper {

    private StudentMapper() {
    }

    public static StudentEntity toEntity(Student student) {
        StudentEntity entity = new StudentEntity();
        entity.setId(student.id());
        entity.setName(student.name());
        entity.setEmail(student.email());
        return entity;
    }

    public static Student toStudent(StudentEntity entity) {
        return new Student(entity.getId(), entity.getName(), entity.getEmail());
    }
}
