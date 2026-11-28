package com.endev.tutoring.exception;

// The token names a user that has no account row. 404.
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String username) {
        super("account not found: " + username);
    }
}
