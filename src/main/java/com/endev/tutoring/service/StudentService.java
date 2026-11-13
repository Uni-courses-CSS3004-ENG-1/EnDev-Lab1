package com.endev.tutoring.service;

import com.endev.tutoring.exception.DuplicateEmailException;
import com.endev.tutoring.exception.StudentNotFoundException;
import com.endev.tutoring.mapper.StudentMapper;
import com.endev.tutoring.model.Student;
import com.endev.tutoring.repository.StudentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class StudentService {

    private final StudentRepository students;

    public StudentService(StudentRepository students) {
        this.students = students;
    }

    @Transactional
    public Student create(String name, String email) {
        Student student = new Student(UUID.randomUUID(), name, email);
        try {
            students.insert(StudentMapper.toEntity(student));
            // flush inside the try, so a taken email is caught here and not at commit
            students.flush();
        } catch (DataIntegrityViolationException e) {
            if (UniqueViolation.is(e)) {
                throw new DuplicateEmailException(email);
            }
            throw e;
        }
        return student;
    }

    @Transactional(readOnly = true)
    public Student find(UUID id) {
        return students.find(id)
                .map(StudentMapper::toStudent)
                .orElseThrow(() -> new StudentNotFoundException(id));
    }
}
