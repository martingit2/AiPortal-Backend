// src/main/java/com/AiPortal/entity/BotConfiguration.java
package com.AiPortal.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "bot_configurations")
public class BotConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType sourceType;

    @Column(nullable = false)
    private String sourceIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BotStatus status;

    private Instant lastRun;

    /**
     * NYTT FELT: Lagrer 'since'-timestampen for API-er som støtter det (f.eks. Pinnacle).
     * Dette lar oss kun hente data som er endret siden forrige kjøring,
     * noe som er ekstremt effektivt.
     */
    private Long sinceTimestamp;

    @Column(nullable = false, updatable = false)
    private String userId;

    public enum SourceType {
        TWITTER,
        SPORT_API,
        LEAGUE_STATS,
        HISTORICAL_FIXTURE_DATA,
        PINNACLE_ODDS,
        STOCK_API,
        CRYPTO_API
    }

    public enum BotStatus {
        ACTIVE,
        PAUSED,
        ERROR
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

    public Long getSinceTimestamp() {
        return sinceTimestamp;
    }

    public void setSinceTimestamp(Long sinceTimestamp) {
        this.sinceTimestamp = sinceTimestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}