package com.endev.tutoring.repository;

import com.endev.tutoring.entity.AccountEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AccountRepository {

    private final EntityManager em;

    public AccountRepository(EntityManager em) {
        this.em = em;
    }

    public void insert(AccountEntity account) {
        em.persist(account);
    }

    public Optional<AccountEntity> findByUsername(String username) {
        return em.createQuery("select a from AccountEntity a where a.username = :username", AccountEntity.class)
                .setParameter("username", username)
                // a list, not a stream: login calls this outside a transaction, where a stream's
                // result set would already be closed
                .getResultList()
                .stream()
                .findFirst();
    }

    // Login: people type their email. Older accounts may still type a username; both find the row.
    public Optional<AccountEntity> findByLogin(String login) {
        return em.createQuery("""
                        select a from AccountEntity a
                        where a.username = :login
                           or a.studentId in (select s.id from StudentEntity s where lower(s.email) = lower(:login))
                           or a.tutorId in (select t.id from TutorEntity t where lower(t.email) = lower(:login))""",
                        AccountEntity.class)
                .setParameter("login", login)
                // a list, not a stream: login runs outside a transaction
                .getResultList()
                .stream()
                .findFirst();
    }

    public void flush() {
        em.flush();
    }
}
