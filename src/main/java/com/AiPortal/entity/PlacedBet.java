// src/main/java/com/AiPortal/entity/PlacedBet.java
package com.AiPortal.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "placed_bets")
public class PlacedBet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private VirtualPortfolio portfolio;

    @Column(nullable = false)
    private Long fixtureId;

    private String market; // F.eks. "MATCH_WINNER", "OVER_UNDER_2.5"
    private String selection; // F.eks. "HOME_WIN", "OVER"
    private double stake; // Innsats
    private double odds; // Markedsodds
    private double modelProbability;
    private double value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BetStatus status;

    private Double profit; // Kan være null før avgjort

    private Instant placedAt = Instant.now();
    private Instant settledAt;

    public enum BetStatus {
        PENDING,
        WON,
        LOST,
        PUSH // For ugyldige/void bets
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public VirtualPortfolio getPortfolio() { return portfolio; }
    public void setPortfolio(VirtualPortfolio portfolio) { this.portfolio = portfolio; }
    public Long getFixtureId() { return fixtureId; }
    public void setFixtureId(Long fixtureId) { this.fixtureId = fixtureId; }
    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }
    public String getSelection() { return selection; }
    public void setSelection(String selection) { this.selection = selection; }
    public double getStake() { return stake; }
    public void setStake(double stake) { this.stake = stake; }
    public double getOdds() { return odds; }
    public void setOdds(double odds) { this.odds = odds; }
    public double getModelProbability() { return modelProbability; }
    public void setModelProbability(double modelProbability) { this.modelProbability = modelProbability; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public BetStatus getStatus() { return status; }
    public void setStatus(BetStatus status) { this.status = status; }
    public Double getProfit() { return profit; }
    public void setProfit(Double profit) { this.profit = profit; }
    public Instant getPlacedAt() { return placedAt; }
    public void setPlacedAt(Instant placedAt) { this.placedAt = placedAt; }
    public Instant getSettledAt() { return settledAt; }
    public void setSettledAt(Instant settledAt) { this.settledAt = settledAt; }
}