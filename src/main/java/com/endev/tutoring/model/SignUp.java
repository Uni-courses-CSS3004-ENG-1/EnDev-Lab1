package com.endev.tutoring.model;

import java.math.BigDecimal;

// What a new person types on the sign-up form. subject, hourlyRate and bio are for tutors only.
public record SignUp(String username, String password, String role, String name, String email,
                     String subject, BigDecimal hourlyRate, String bio) {

    // the record's default toString would print the password into a log line
    @Override
    public String toString() {
        return "SignUp[username=" + username + ", role=" + role + ", password=****]";
    }
}
