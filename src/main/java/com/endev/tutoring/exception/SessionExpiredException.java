package com.endev.tutoring.exception;

// POST /api/v1/auth/refresh without a valid refresh cookie. 401: the person logs in again.
public class SessionExpiredException extends RuntimeException {

    public SessionExpiredException() {
        super("session expired, log in again");
    }
}
