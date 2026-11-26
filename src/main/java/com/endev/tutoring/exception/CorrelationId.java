package com.endev.tutoring.exception;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

// One way to read X-Correlation-Id for every error body: the advice and the security filters
// both use it. A caller's value is echoed; a missing or blank one becomes a new UUID.
// The id is kept on the request, so a log line and the error body of one call show the same id.
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";

    private static final String ATTRIBUTE = CorrelationId.class.getName();

    private CorrelationId() {
    }

    public static String of(HttpServletRequest request) {
        if (request.getAttribute(ATTRIBUTE) instanceof String known) {
            return known;
        }
        String value = request.getHeader(HEADER);
        String id = value == null || value.isBlank() ? UUID.randomUUID().toString() : value;
        request.setAttribute(ATTRIBUTE, id);
        return id;
    }
}
