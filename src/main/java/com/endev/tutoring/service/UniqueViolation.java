package com.endev.tutoring.service;

import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

// PostgreSQL's unique-key SQLState. Other integrity errors (CHECK, NOT NULL, foreign key) are not duplicates.
final class UniqueViolation {

    private UniqueViolation() {
    }

    static boolean is(DataIntegrityViolationException e) {
        return e.getMostSpecificCause() instanceof SQLException sql && "23505".equals(sql.getSQLState());
    }
}
