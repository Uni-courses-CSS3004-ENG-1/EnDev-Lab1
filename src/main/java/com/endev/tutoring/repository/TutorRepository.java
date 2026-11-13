package com.endev.tutoring.repository;

import com.endev.tutoring.entity.TutorEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class TutorRepository {

    private final EntityManager em;

    public TutorRepository(EntityManager em) {
        this.em = em;
    }

    public void insert(TutorEntity tutor) {
        em.persist(tutor);
    }

    public Optional<TutorEntity> find(UUID id) {
        return Optional.ofNullable(em.find(TutorEntity.class, id));
    }

    public void flush() {
        em.flush();
    }
}
