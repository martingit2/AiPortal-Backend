package com.AiPortal.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "bot_configurations") // Navnet på databasetabellen
public class BotConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // F.eks. "Fabrizio Romano Fotballnyheter"

    @Enumerated(EnumType.STRING) // Lagrer enum-verdien som tekst (lesbart)
    @Column(nullable = false)
    private SourceType sourceType; // F.eks. TWITTER, STOCK_API

    @Column(nullable = false)
    private String sourceIdentifier; // F.eks. "FabrizioRomano" for Twitter, "TSLA" for aksje

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BotStatus status; // F.eks. ACTIVE, PAUSED

    private Instant lastRun; // Tidspunkt for siste kjøring

    @Column(nullable = false, updatable = false)
    private String userId; // Clerk User ID for eieren av denne boten

    // Enums for å definere typer og statuser
    public enum SourceType {
        TWITTER,
        SPORT_API,
        STOCK_API,
        CRYPTO_API
    }

    public enum BotStatus {
        ACTIVE,
        PAUSED,
        ERROR
    }

    // Getters and Setters (du kan også bruke Lombok @Data for å slippe dette)

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

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    public BotStatus getStatus() {
        return status;
    }

    public void setStatus(BotStatus status) {
        this.status = status;
    }

    public Instant getLastRun() {
        return lastRun;
    }

    public void setLastRun(Instant lastRun) {
        this.lastRun = lastRun;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}