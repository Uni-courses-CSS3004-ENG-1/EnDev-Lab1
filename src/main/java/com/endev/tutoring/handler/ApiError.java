package com.endev.tutoring.handler;

public record ApiError(String code, String message, String correlationId) {
}
