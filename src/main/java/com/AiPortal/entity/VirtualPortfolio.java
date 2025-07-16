// src/main/java/com/AiPortal/entity/VirtualPortfolio.java
package com.AiPortal.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "virtual_portfolios")
public class VirtualPortfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hvilken modell denne porteføljen tilhører
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false, unique = true)
    private AnalysisModel model;

    @Column(nullable = false)
    private String name; // F.eks. "XGBoost v5 H2H - Kelly Criterion"

    @Column(nullable = false)
    private double startingBalance;

    @Column(nullable = false)
    private double currentBalance;

    @Column(nullable = false)
    private boolean isActive = false; // Om porteføljen aktivt skal plassere bets

    private int totalBets = 0;
    private int wins = 0;
    private int losses = 0;
    private int pushes = 0; // For "void" bets

    private String discordWebhookUrl;

    private Instant createdAt = Instant.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AnalysisModel getModel() { return model; }
    public void setModel(AnalysisModel model) { this.model = model; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getStartingBalance() { return startingBalance; }
    public void setStartingBalance(double startingBalance) { this.startingBalance = startingBalance; }
    public double getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(double currentBalance) { this.currentBalance = currentBalance; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public int getTotalBets() { return totalBets; }
    public void setTotalBets(int totalBets) { this.totalBets = totalBets; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
    public int getPushes() { return pushes; }
    public void setPushes(int pushes) { this.pushes = pushes; }
    public String getDiscordWebhookUrl() { return discordWebhookUrl; }
    public void setDiscordWebhookUrl(String discordWebhookUrl) { this.discordWebhookUrl = discordWebhookUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}