package com.endev.tutoring.exception;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

// One way to read X-Correlation-Id for every error body: the advice and the security filters
// both use it. A caller's value is echoed; a missing or blank one becomes a new UUID.
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";

    private CorrelationId() {
    }

    public static String of(HttpServletRequest request) {
        String value = request.getHeader(HEADER);
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value;
    }
}
