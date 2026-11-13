package com.endev.tutoring.model;

import java.util.UUID;

// A student as the rest of the code sees it.
public record Student(UUID id, String name, String email) {
}
