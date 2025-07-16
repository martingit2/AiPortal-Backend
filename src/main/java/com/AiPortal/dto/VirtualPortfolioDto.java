// src/main/java/com/AiPortal/dto/VirtualPortfolioDto.java
package com.AiPortal.dto;

import com.AiPortal.entity.AnalysisModel;
import com.AiPortal.entity.VirtualPortfolio;

import java.time.Instant;

public class VirtualPortfolioDto {

    private Long id;
    private String name;
    private double startingBalance;
    private double currentBalance;
    private boolean isActive;
    private int totalBets;
    private int wins;
    private int losses;
    private int pushes;
    private String discordWebhookUrl;
    private Instant createdAt;
    private AnalysisModel model;

    // Tom konstruktør for Jackson
    public VirtualPortfolioDto() {}

    // Konstruktør for enkel mapping fra Entitet
    public VirtualPortfolioDto(VirtualPortfolio portfolio) {
        this.id = portfolio.getId();
        this.name = portfolio.getName();
        this.startingBalance = portfolio.getStartingBalance();
        this.currentBalance = portfolio.getCurrentBalance();
        this.isActive = portfolio.isActive();
        this.totalBets = portfolio.getTotalBets();
        this.wins = portfolio.getWins();
        this.losses = portfolio.getLosses();
        this.pushes = portfolio.getPushes();
        this.discordWebhookUrl = portfolio.getDiscordWebhookUrl();
        this.createdAt = portfolio.getCreatedAt();
        this.model = portfolio.getModel(); // Dette er trygt fordi vi henter den med JOIN FETCH
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getStartingBalance() {
        return startingBalance;
    }

    public void setStartingBalance(double startingBalance) {
        this.startingBalance = startingBalance;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(double currentBalance) {
        this.currentBalance = currentBalance;
    }

    public boolean getIsActive() { // Følger standard bean-konvensjon for boolean
        return isActive;
    }

    public void setIsActive(boolean active) {
        isActive = active;
    }

    public int getTotalBets() {
        return totalBets;
    }

    public void setTotalBets(int totalBets) {
        this.totalBets = totalBets;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public int getPushes() {
        return pushes;
    }

    public void setPushes(int pushes) {
        this.pushes = pushes;
    }

    public String getDiscordWebhookUrl() {
        return discordWebhookUrl;
    }

    public void setDiscordWebhookUrl(String discordWebhookUrl) {
        this.discordWebhookUrl = discordWebhookUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public AnalysisModel getModel() {
        return model;
    }

    public void setModel(AnalysisModel model) {
        this.model = model;
    }
}