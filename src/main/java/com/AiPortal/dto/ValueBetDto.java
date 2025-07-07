package com.AiPortal.dto;

import java.time.Instant;

public class ValueBetDto {
    private Long fixtureId;
    private String homeTeamName;
    private String awayTeamName;
    private Instant fixtureDate;

    // Beste markedsodds vi fant
    private double marketHomeOdds;
    private double marketDrawOdds;
    private double marketAwayOdds;
    private String bookmakerName; // Navnet på bookmakeren med disse oddsene

    // Vår beregnede "sanne" odds
    private double aracanixHomeOdds;
    private double aracanixDrawOdds;
    private double aracanixAwayOdds;

    // Vår beregnede "value"
    private double valueHome;
    private double valueDraw;
    private double valueAway;

    // Getters and Setters


    public Long getFixtureId() {
        return fixtureId;
    }

    public void setFixtureId(Long fixtureId) {
        this.fixtureId = fixtureId;
    }

    public String getHomeTeamName() {
        return homeTeamName;
    }

    public void setHomeTeamName(String homeTeamName) {
        this.homeTeamName = homeTeamName;
    }

    public String getAwayTeamName() {
        return awayTeamName;
    }

    public void setAwayTeamName(String awayTeamName) {
        this.awayTeamName = awayTeamName;
    }

    public Instant getFixtureDate() {
        return fixtureDate;
    }

    public void setFixtureDate(Instant fixtureDate) {
        this.fixtureDate = fixtureDate;
    }

    public double getMarketHomeOdds() {
        return marketHomeOdds;
    }

    public void setMarketHomeOdds(double marketHomeOdds) {
        this.marketHomeOdds = marketHomeOdds;
    }

    public double getMarketDrawOdds() {
        return marketDrawOdds;
    }

    public void setMarketDrawOdds(double marketDrawOdds) {
        this.marketDrawOdds = marketDrawOdds;
    }

    public double getMarketAwayOdds() {
        return marketAwayOdds;
    }

    public void setMarketAwayOdds(double marketAwayOdds) {
        this.marketAwayOdds = marketAwayOdds;
    }

    public String getBookmakerName() {
        return bookmakerName;
    }

    public void setBookmakerName(String bookmakerName) {
        this.bookmakerName = bookmakerName;
    }

    public double getAracanixHomeOdds() {
        return aracanixHomeOdds;
    }

    public void setAracanixHomeOdds(double aracanixHomeOdds) {
        this.aracanixHomeOdds = aracanixHomeOdds;
    }

    public double getAracanixDrawOdds() {
        return aracanixDrawOdds;
    }

    public void setAracanixDrawOdds(double aracanixDrawOdds) {
        this.aracanixDrawOdds = aracanixDrawOdds;
    }

    public double getAracanixAwayOdds() {
        return aracanixAwayOdds;
    }

    public void setAracanixAwayOdds(double aracanixAwayOdds) {
        this.aracanixAwayOdds = aracanixAwayOdds;
    }

    public double getValueHome() {
        return valueHome;
    }

    public void setValueHome(double valueHome) {
        this.valueHome = valueHome;
    }

    public double getValueDraw() {
        return valueDraw;
    }

    public void setValueDraw(double valueDraw) {
        this.valueDraw = valueDraw;
    }

    public double getValueAway() {
        return valueAway;
    }

    public void setValueAway(double valueAway) {
        this.valueAway = valueAway;
    }
}