// src/main/java/com/AiPortal/entity/HeadToHeadStats.java
package com.AiPortal.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "h2h_stats")
public class HeadToHeadStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Kobling til kampen denne statistikken ble generert for
    @OneToOne
    @JoinColumn(name = "fixture_id", nullable = false, unique = true)
    private Fixture fixture;

    // Lagene som ble sammenlignet
    private Integer team1Id;
    private Integer team2Id;

    // Selve statistikken
    private int matchesPlayed;
    private int team1Wins;
    private int team2Wins;
    private int draws;
    private double avgTotalGoals;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Fixture getFixture() { return fixture; }
    public void setFixture(Fixture fixture) { this.fixture = fixture; }
    public Integer getTeam1Id() { return team1Id; }
    public void setTeam1Id(Integer team1Id) { this.team1Id = team1Id; }
    public Integer getTeam2Id() { return team2Id; }
    public void setTeam2Id(Integer team2Id) { this.team2Id = team2Id; }
    public int getMatchesPlayed() { return matchesPlayed; }
    public void setMatchesPlayed(int matchesPlayed) { this.matchesPlayed = matchesPlayed; }
    public int getTeam1Wins() { return team1Wins; }
    public void setTeam1Wins(int team1Wins) { this.team1Wins = team1Wins; }
    public int getTeam2Wins() { return team2Wins; }
    public void setTeam2Wins(int team2Wins) { this.team2Wins = team2Wins; }
    public int getDraws() { return draws; }
    public void setDraws(int draws) { this.draws = draws; }
    public double getAvgTotalGoals() { return avgTotalGoals; }
    public void setAvgTotalGoals(double avgTotalGoals) { this.avgTotalGoals = avgTotalGoals; }
}