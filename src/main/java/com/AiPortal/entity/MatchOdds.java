// src/main/java/com/AiPortal/entity/MatchOdds.java
package com.AiPortal.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Representerer et spesifikt odds-marked (f.eks. Match Winner, Over/Under)
 * for en gitt kamp fra en spesifikk bookmaker.
 * Denne versjonen er generalisert for å håndtere alle typer oddsmarkeder.
 */
@Entity
@Table(name = "match_odds")
public class MatchOdds {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixture_id", nullable = false)
    private Fixture fixture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookmaker_id", nullable = false)
    private Bookmaker bookmaker;

    /**
     * Navnet på markedet, f.eks. "Match Winner", "Asian Handicap", "Total Goals".
     */
    @Column(nullable = false)
    private String betName;

    /**
     * En JSON-kolonne som lagrer en liste av alle tilgjengelige valg og deres odds.
     * Eksempel for "Total Goals": [{"name": "Over", "points": "2.5", "odds": 1.90}, {"name": "Under", "points": "2.5", "odds": 1.90}]
     * Eksempel for "Match Winner": [{"name": "Home", "odds": 1.50}, {"name": "Draw", "odds": 3.50}, {"name": "Away", "odds": 4.00}]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String oddsData;

    @Column(nullable = false)
    private Instant lastUpdated;

    // --- GETTERS AND SETTERS ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Fixture getFixture() {
        return fixture;
    }

    public void setFixture(Fixture fixture) {
        this.fixture = fixture;
    }

    public Bookmaker getBookmaker() {
        return bookmaker;
    }

    public void setBookmaker(Bookmaker bookmaker) {
        this.bookmaker = bookmaker;
    }

    public String getBetName() {
        return betName;
    }

    public void setBetName(String betName) {
        this.betName = betName;
    }

    public String getOddsData() {
        return oddsData;
    }

    public void setOddsData(String oddsData) {
        this.oddsData = oddsData;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}