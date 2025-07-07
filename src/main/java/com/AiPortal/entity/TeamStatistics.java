package com.AiPortal.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "team_statistics")
public class TeamStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identifikasjon av statistikken
    private int leagueId;
    private String leagueName;
    private int teamId;
    private String teamName;
    private int season;

    // Kampstatistikk (Fixtures)
    private int playedTotal;
    private int winsTotal;
    private int drawsTotal;
    private int lossesTotal;

    // Målstatistikk (Goals)
    private int goalsForTotal;
    private int goalsAgainstTotal;

    // Annen nyttig statistikk
    private int cleanSheetTotal;
    private int failedToScoreTotal;

    private Instant lastUpdated = Instant.now();

    // Kobling til boten som hentet dataen
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_bot_id")
    private BotConfiguration sourceBot;

    // Nødvendig med en tom konstruktør for JPA
    public TeamStatistics() {
    }

    // Getters and Setters

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

    public String getLeagueName() {
        return leagueName;
    }

    public void setLeagueName(String leagueName) {
        this.leagueName = leagueName;
    }

    public int getTeamId() {
        return teamId;
    }

    public void setTeamId(int teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public int getPlayedTotal() {
        return playedTotal;
    }

    public void setPlayedTotal(int playedTotal) {
        this.playedTotal = playedTotal;
    }

    public int getWinsTotal() {
        return winsTotal;
    }

    public void setWinsTotal(int winsTotal) {
        this.winsTotal = winsTotal;
    }

    public int getDrawsTotal() {
        return drawsTotal;
    }

    public void setDrawsTotal(int drawsTotal) {
        this.drawsTotal = drawsTotal;
    }

    public int getLossesTotal() {
        return lossesTotal;
    }

    public void setLossesTotal(int lossesTotal) {
        this.lossesTotal = lossesTotal;
    }

    public int getGoalsForTotal() {
        return goalsForTotal;
    }

    public void setGoalsForTotal(int goalsForTotal) {
        this.goalsForTotal = goalsForTotal;
    }

    public int getGoalsAgainstTotal() {
        return goalsAgainstTotal;
    }

    public void setGoalsAgainstTotal(int goalsAgainstTotal) {
        this.goalsAgainstTotal = goalsAgainstTotal;
    }

    public int getCleanSheetTotal() {
        return cleanSheetTotal;
    }

    public void setCleanSheetTotal(int cleanSheetTotal) {
        this.cleanSheetTotal = cleanSheetTotal;
    }

    public int getFailedToScoreTotal() {
        return failedToScoreTotal;
    }

    public void setFailedToScoreTotal(int failedToScoreTotal) {
        this.failedToScoreTotal = failedToScoreTotal;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public BotConfiguration getSourceBot() {
        return sourceBot;
    }

    public void setSourceBot(BotConfiguration sourceBot) {
        this.sourceBot = sourceBot;
    }
}