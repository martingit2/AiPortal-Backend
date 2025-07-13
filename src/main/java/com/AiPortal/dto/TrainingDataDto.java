// src/main/java/com/AiPortal/dto/TrainingDataDto.java
package com.AiPortal.dto;

/**
 * Data Transfer Object for å sende et komplett, flatt datasett for én kamp
 * fra Java-backenden til Python-tjenesten for maskinlæring.
 */
public class TrainingDataDto {

    // Identifikasjon
    private Long fixtureId;
    private Integer season;
    private Integer leagueId;

    // Features - Hjemmelag
    private double homeAvgShotsOnGoal;
    private double homeAvgShotsOffGoal;
    private double homeAvgCorners;
    private int homeInjuries;

    // Features - Bortelag
    private double awayAvgShotsOnGoal;
    private double awayAvgShotsOffGoal;
    private double awayAvgCorners;
    private int awayInjuries;

    // Label (Målet vi skal predikere)
    private String result; // "HOME_WIN", "DRAW", "AWAY_WIN"


    // Getters and Setters
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
}