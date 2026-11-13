package com.endev.tutoring.dto;

public record ApiError(String code, String message, String correlationId) {
}
