package com.endev.tutoring.model;

// The signed-in person: their account, plus the name and email from their student or tutor row.
// tutor is null for a student.
public record Profile(Account account, String name, String email, Tutor tutor) {
}
