package com.AiPortal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "fixtures")
public class Fixture {

    @Id // Vi bruker API-ets ID, ikke en autogenerert en, for å unngå duplikater.
    private Long id;

    @Column(nullable = false)
    private int leagueId;

    @Column(nullable = false)
    private int season;

    @Column(nullable = false)
    private Instant date;

    private String status;

    // Hjemmelag
    @Column(nullable = false)
    private int homeTeamId;

    @Column(nullable = false)
    private String homeTeamName;

    // Bortelag
    @Column(nullable = false)
    private int awayTeamId;

    @Column(nullable = false)
    private String awayTeamName;

    // Getters and Setters (generert i IDE eller med Lombok)

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getLeagueId() {
        return leagueId;
    }

    public void setLeagueId(int leagueId) {
        this.leagueId = leagueId;
    }

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public Instant getDate() {
        return date;
    }

    public void setDate(Instant date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getHomeTeamId() {
        return homeTeamId;
    }

    public void setHomeTeamId(int homeTeamId) {
        this.homeTeamId = homeTeamId;
    }

    public String getHomeTeamName() {
        return homeTeamName;
    }

    public void setHomeTeamName(String homeTeamName) {
        this.homeTeamName = homeTeamName;
    }

    public int getAwayTeamId() {
        return awayTeamId;
    }

    public void setAwayTeamId(int awayTeamId) {
        this.awayTeamId = awayTeamId;
    }

    public String getAwayTeamName() {
        return awayTeamName;
    }

    public void setAwayTeamName(String awayTeamName) {
        this.awayTeamName = awayTeamName;
    }
}