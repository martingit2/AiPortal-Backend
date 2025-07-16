// src/main/java/com/AiPortal/dto/PlacedBetDto.java
package com.AiPortal.dto;

import com.AiPortal.entity.Fixture;
import com.AiPortal.entity.PlacedBet;

import java.time.Instant;

public class PlacedBetDto {

    private Long id;
    private Long fixtureId;
    private String homeTeamName;
    private String awayTeamName;
    private String market;
    private String selection;
    private double stake;
    private double odds;
    private PlacedBet.BetStatus status;
    private Double profit;
    private Instant placedAt;

    // Konstruktør som tar inn et Bet og en Fixture
    public PlacedBetDto(PlacedBet bet, Fixture fixture) {
        this.id = bet.getId();
        this.fixtureId = bet.getFixtureId();
        this.homeTeamName = fixture.getHomeTeamName();
        this.awayTeamName = fixture.getAwayTeamName();
        this.market = bet.getMarket();
        this.selection = bet.getSelection();
        this.stake = bet.getStake();
        this.odds = bet.getOdds();
        this.status = bet.getStatus();
        this.profit = bet.getProfit();
        this.placedAt = bet.getPlacedAt();
    }

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

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getSelection() {
        return selection;
    }

    public void setSelection(String selection) {
        this.selection = selection;
    }

    public double getStake() {
        return stake;
    }

    public void setStake(double stake) {
        this.stake = stake;
    }

    public double getOdds() {
        return odds;
    }

    public void setOdds(double odds) {
        this.odds = odds;
    }

    public PlacedBet.BetStatus getStatus() {
        return status;
    }

    public void setStatus(PlacedBet.BetStatus status) {
        this.status = status;
    }

    public Double getProfit() {
        return profit;
    }

    public void setProfit(Double profit) {
        this.profit = profit;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(Instant placedAt) {
        this.placedAt = placedAt;
    }
}