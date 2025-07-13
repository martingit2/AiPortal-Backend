// src/main/java/com/AiPortal/entity/PendingFixtureChunk.java
package com.AiPortal.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Representerer en "chunk" (en bunt) av kamp-IDer som venter på å bli prosessert
 * av den historiske datainnsamleren. Dette er en del av en robust "producer-consumer"-
 * arkitektur for å håndtere lange bakgrunnsjobber.
 */
@Entity
@Table(name = "pending_fixture_chunks")
public class PendingFixtureChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * En streng som inneholder kamp-ID-ene for denne chunken,
     * separert med bindestrek (f.eks. "123-124-125").
     */
    @Column(nullable = false, length = 1024)
    private String fixtureIds;

    /**
     * Hvilken liga og sesong denne chunken tilhører.
     * Nyttig for sporing og feilsøking.
     */
    @Column(nullable = false)
    private String sourceIdentifier; // F.eks. "39:2023"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChunkStatus status;

    private Instant createdAt = Instant.now();

    private Instant processedAt;

    /**
     * Antall ganger vi har forsøkt å prosessere denne chunken.
     * Kan brukes til å unngå evige forsøk på en chunk som alltid feiler.
     */
    private int attemptCount = 0;

    @Column(columnDefinition = "TEXT")
    private String lastErrorMessage;

    public enum ChunkStatus {
        PENDING,
        PROCESSING,
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

    public String getFixtureIds() {
        return fixtureIds;
    }

    public void setFixtureIds(String fixtureIds) {
        this.fixtureIds = fixtureIds;
    }

    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    public ChunkStatus getStatus() {
        return status;
    }

    public void setStatus(ChunkStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }
}