package com.endev.tutoring.persistence;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class LessonJpa {

    private final EntityManager em;

    public LessonJpa(EntityManager em) {
        this.em = em;
    }

    public void insert(LessonEntity lesson) {
        // The UUID is already set, so persist avoids save() checking whether it exists first.
        em.persist(lesson);
    }

    public int count(String businessKey) {
        Long count = em.createQuery(
                        "select count(l) from LessonEntity l where l.businessKey = :businessKey",
                        Long.class)
                .setParameter("businessKey", businessKey)
                .getSingleResult();
        return count.intValue();
    }

    public String statusText(String businessKey) {
        return em.createQuery(
                        "select l.status from LessonEntity l where l.businessKey = :businessKey",
                        String.class)
                .setParameter("businessKey", businessKey)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    // a plain primary-key lookup: no flush, no write
    public Optional<LessonEntity> find(UUID id) {
        return Optional.ofNullable(em.find(LessonEntity.class, id));
    }

    public void flush() {
        em.flush();
    }
}
