package com.endev.tutoring.persistence;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

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

    public void flush() {
        em.flush();
    }
}
