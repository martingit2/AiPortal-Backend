package com.AiPortal.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "analyses")
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status;

    // @Lob-annotasjonen er fjernet for å unngå problemer med PostgreSQL-driveren.
    // @Column(columnDefinition = "TEXT") er nok til å sikre at det blir en tekstkolonne
    // som kan lagre store mengder data (opptil 1 GB i PostgreSQL).
    @Column(columnDefinition = "TEXT")
    private String result; // Lagrer resultatet som en JSON-streng

    private Instant createdAt = Instant.now();

    private Instant completedAt;

    @Column(nullable = false)
    private String userId;

    public enum AnalysisStatus {
        QUEUED,
        RUNNING,
        COMPLETED,
        FAILED
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AnalysisStatus getStatus() {
        return status;
    }

    public void setStatus(AnalysisStatus status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}