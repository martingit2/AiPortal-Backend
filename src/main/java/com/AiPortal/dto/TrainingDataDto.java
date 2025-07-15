// src/main/java/com/AiPortal/dto/TrainingDataDto.java
package com.AiPortal.dto;

/**
 * Data Transfer Object for å sende et komplett, flatt datasett for én kamp
 * fra Java-backenden til Python-tjenesten for maskinlæring.
 *
 * Denne versjonen er utvidet med H2H (Head-to-Head) statistikk.
 */
public class TrainingDataDto {

    // Identifikasjon
    private Long fixtureId;
    private Integer season;
    private Integer leagueId;

    // Eksisterende features
    private double homeAvgShotsOnGoal;
    private double homeAvgShotsOffGoal;
    private double homeAvgCorners;
    private int homeInjuries;
    private double homePlayersAvgRating;
    private double homePlayersAvgGoals;

    private double awayAvgShotsOnGoal;
    private double awayAvgShotsOffGoal;
    private double awayAvgCorners;
    private int awayInjuries;
    private double awayPlayersAvgRating;
    private double awayPlayersAvgGoals;

    // --- NYE H2H FEATURES ---
    private double h2hHomeWinPercentage;
    private double h2hAwayWinPercentage;
    private double h2hDrawPercentage;
    private double h2hAvgGoals;

    // Label (Målet vi skal predikere)
    private String result;
    private Integer goalsHome;
    private Integer goalsAway;


    // --- Getters and Setters ---

    public Long getFixtureId() { return fixtureId; }
    public void setFixtureId(Long fixtureId) { this.fixtureId = fixtureId; }
    public Integer getSeason() { return season; }
    public void setSeason(Integer season) { this.season = season; }
    public Integer getLeagueId() { return leagueId; }
    public void setLeagueId(Integer leagueId) { this.leagueId = leagueId; }
    public double getHomeAvgShotsOnGoal() { return homeAvgShotsOnGoal; }
    public void setHomeAvgShotsOnGoal(double homeAvgShotsOnGoal) { this.homeAvgShotsOnGoal = homeAvgShotsOnGoal; }
    public double getHomeAvgShotsOffGoal() { return homeAvgShotsOffGoal; }
    public void setHomeAvgShotsOffGoal(double homeAvgShotsOffGoal) { this.homeAvgShotsOffGoal = homeAvgShotsOffGoal; }
    public double getHomeAvgCorners() { return homeAvgCorners; }
    public void setHomeAvgCorners(double homeAvgCorners) { this.homeAvgCorners = homeAvgCorners; }
    public int getHomeInjuries() { return homeInjuries; }
    public void setHomeInjuries(int homeInjuries) { this.homeInjuries = homeInjuries; }
    public double getAwayAvgShotsOnGoal() { return awayAvgShotsOnGoal; }
    public void setAwayAvgShotsOnGoal(double awayAvgShotsOnGoal) { this.awayAvgShotsOnGoal = awayAvgShotsOnGoal; }
    public double getAwayAvgShotsOffGoal() { return awayAvgShotsOffGoal; }
    public void setAwayAvgShotsOffGoal(double awayAvgShotsOffGoal) { this.awayAvgShotsOffGoal = awayAvgShotsOffGoal; }
    public double getAwayAvgCorners() { return awayAvgCorners; }
    public void setAwayAvgCorners(double awayAvgCorners) { this.awayAvgCorners = awayAvgCorners; }
    public int getAwayInjuries() { return awayInjuries; }
    public void setAwayInjuries(int awayInjuries) { this.awayInjuries = awayInjuries; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public double getHomePlayersAvgRating() { return homePlayersAvgRating; }
    public void setHomePlayersAvgRating(double homePlayersAvgRating) { this.homePlayersAvgRating = homePlayersAvgRating; }
    public double getHomePlayersAvgGoals() { return homePlayersAvgGoals; }
    public void setHomePlayersAvgGoals(double homePlayersAvgGoals) { this.homePlayersAvgGoals = homePlayersAvgGoals; }
    public double getAwayPlayersAvgRating() { return awayPlayersAvgRating; }
    public void setAwayPlayersAvgRating(double awayPlayersAvgRating) { this.awayPlayersAvgRating = awayPlayersAvgRating; }
    public double getAwayPlayersAvgGoals() { return awayPlayersAvgGoals; }
    public void setAwayPlayersAvgGoals(double awayPlayersAvgGoals) { this.awayPlayersAvgGoals = awayPlayersAvgGoals; }
    public Integer getGoalsHome() { return goalsHome; }
    public void setGoalsHome(Integer goalsHome) { this.goalsHome = goalsHome; }
    public Integer getGoalsAway() { return goalsAway; }
    public void setGoalsAway(Integer goalsAway) { this.goalsAway = goalsAway; }

    // --- NYE GETTERS OG SETTERS for H2H ---
    public double getH2hHomeWinPercentage() { return h2hHomeWinPercentage; }
    public void setH2hHomeWinPercentage(double h2hHomeWinPercentage) { this.h2hHomeWinPercentage = h2hHomeWinPercentage; }
    public double getH2hAwayWinPercentage() { return h2hAwayWinPercentage; }
    public void setH2hAwayWinPercentage(double h2hAwayWinPercentage) { this.h2hAwayWinPercentage = h2hAwayWinPercentage; }
    public double getH2hDrawPercentage() { return h2hDrawPercentage; }
    public void setH2hDrawPercentage(double h2hDrawPercentage) { this.h2hDrawPercentage = h2hDrawPercentage; }
    public double getH2hAvgGoals() { return h2hAvgGoals; }
    public void setH2hAvgGoals(double h2hAvgGoals) { this.h2hAvgGoals = h2hAvgGoals; }
}