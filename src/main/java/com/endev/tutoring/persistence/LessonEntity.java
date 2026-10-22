package com.endev.tutoring.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

// One row of the lesson table. It names the columns that already exist; it never creates them.
// It stays in persistence and never leaves a service method.
@Entity
@Table(name = "lesson")
public class LessonEntity {

    // set in Java, so there is no @GeneratedValue
    @Id
    private UUID id;

    @Column(name = "business_key", nullable = false, unique = true)
    private String businessKey;

    // the same words as the CHECK, never the enum's own idea of a name
    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String subject;

    // filled by DEFAULT now(); if Java sent it, the null would replace the default
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
