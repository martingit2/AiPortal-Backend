// src/main/java/com/AiPortal/dto/HeadToHeadStatsDto.java
package com.AiPortal.dto;

public class HeadToHeadStatsDto {
    private int matchesPlayed;
    private int team1Wins;
    private int team2Wins;
    private int draws;
    private double avgTotalGoals;

    // Konstruktør, Getters og Setters
    public HeadToHeadStatsDto(int matchesPlayed, int team1Wins, int team2Wins, int draws, double avgTotalGoals) {
        this.matchesPlayed = matchesPlayed;
        this.team1Wins = team1Wins;
        this.team2Wins = team2Wins;
        this.draws = draws;
        this.avgTotalGoals = avgTotalGoals;
    }

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