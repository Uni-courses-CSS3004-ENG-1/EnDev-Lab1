package com.endev.tutoring.repository;

import com.endev.tutoring.entity.StudentEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class StudentRepository {

    private final EntityManager em;

    public StudentRepository(EntityManager em) {
        this.em = em;
    }

    public void insert(StudentEntity student) {
        em.persist(student);
    }

    public Optional<StudentEntity> find(UUID id) {
        return Optional.ofNullable(em.find(StudentEntity.class, id));
    }

    public void flush() {
        em.flush();
    }
}
