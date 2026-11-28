package com.endev.tutoring.repository;

import com.endev.tutoring.entity.LessonEntity;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class LessonRepository {

    private final EntityManager em;

    public LessonRepository(EntityManager em) {
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

    // a tutor's lessons, or a student's lessons: soonest start first, unscheduled ones last
    public List<LessonEntity> findByTutor(UUID tutorId) {
        return em.createQuery("""
                        select l from LessonEntity l where l.tutorId = :id
                        order by l.startsAt asc nulls last, l.createdAt desc""", LessonEntity.class)
                .setParameter("id", tutorId)
                .getResultList();
    }

    public List<LessonEntity> findByStudent(UUID studentId) {
        return em.createQuery("""
                        select l from LessonEntity l where l.studentId = :id
                        order by l.startsAt asc nulls last, l.createdAt desc""", LessonEntity.class)
                .setParameter("id", studentId)
                .getResultList();
    }

    // lessons of this tutor that are still on (REQUESTED or CONFIRMED) and start between from and to
    public long countActiveBetween(UUID tutorId, OffsetDateTime from, OffsetDateTime to) {
        return em.createQuery("""
                        select count(l) from LessonEntity l
                        where l.tutorId = :id and l.status in ('REQUESTED', 'CONFIRMED')
                          and l.startsAt > :from and l.startsAt < :to""", Long.class)
                .setParameter("id", tutorId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    // tutor id -> how many of their lessons are COMPLETED
    public Map<UUID, Long> completedCountByTutor() {
        return em.createQuery("""
                        select l.tutorId, count(l) from LessonEntity l
                        where l.status = 'COMPLETED' and l.tutorId is not null
                        group by l.tutorId""", Object[].class)
                .getResultList()
                .stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
    }

    public void flush() {
        em.flush();
    }
}
