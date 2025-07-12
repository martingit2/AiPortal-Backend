// src/main/java/com/AiPortal/entity/Injury.java
package com.AiPortal.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "injuries")
public class Injury {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Koblinger for å vite hvilken kamp og hvilket lag skaden tilhører
    @Column(nullable = false)
    private Long fixtureId;

    @Column(nullable = false)
    private Integer leagueId;

    @Column(nullable = false)
    private Integer season;

    @Column(nullable = false)
    private Integer teamId;

    // Spillerinformasjon
    @Column(nullable = false)
    private Integer playerId;

    @Column(nullable = false)
    private String playerName;

    // Skadeinformasjon
    @Column(nullable = false)
    private String type; // F.eks. "Kneeskade", "Muskelstrekk"

    @Column(nullable = false)
    private String reason; // F.eks. "Meniscus tear", "Hamstring"


    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFixtureId() {
        return fixtureId;
    }

    public void setFixtureId(Long fixtureId) {
        this.fixtureId = fixtureId;
    }

    public Integer getLeagueId() {
        return leagueId;
    }

    public void setLeagueId(Integer leagueId) {
        this.leagueId = leagueId;
    }

    public Integer getSeason() {
        return season;
    }

    public void setSeason(Integer season) {
        this.season = season;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Integer playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}