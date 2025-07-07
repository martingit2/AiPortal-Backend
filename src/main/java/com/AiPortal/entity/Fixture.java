package com.AiPortal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "fixtures")
public class Fixture {

    @Id
    private Long id;

    // Endret til wrapper-klasser for å kunne håndtere 'null'
    @Column(nullable = false)
    private Integer leagueId;

    @Column(nullable = false)
    private Integer season;

    @Column(nullable = false)
    private Instant date;

    private String status;

    @Column(nullable = false)
    private Integer homeTeamId;

    @Column(nullable = false)
    private String homeTeamName;

    @Column(nullable = false)
    private Integer awayTeamId;

    @Column(nullable = false)
    private String awayTeamName;

    // Getters and Setters for de nye typene
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getLeagueId() { return leagueId; }
    public void setLeagueId(Integer leagueId) { this.leagueId = leagueId; }
    public Integer getSeason() { return season; }
    public void setSeason(Integer season) { this.season = season; }
    public Instant getDate() { return date; }
    public void setDate(Instant date) { this.date = date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getHomeTeamId() { return homeTeamId; }
    public void setHomeTeamId(Integer homeTeamId) { this.homeTeamId = homeTeamId; }
    public String getHomeTeamName() { return homeTeamName; }
    public void setHomeTeamName(String homeTeamName) { this.homeTeamName = homeTeamName; }
    public Integer getAwayTeamId() { return awayTeamId; }
    public void setAwayTeamId(Integer awayTeamId) { this.awayTeamId = awayTeamId; }
    public String getAwayTeamName() { return awayTeamName; }
    public void setAwayTeamName(String awayTeamName) { this.awayTeamName = awayTeamName; }
}