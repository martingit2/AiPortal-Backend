// src/main/java/com/AiPortal/dto/PlayerMatchStatisticsDto.java
package com.AiPortal.dto;

/**
 * DTO for å sende detaljert spillerstatistikk for en kamp til frontenden.
 * Denne kombinerer data fra PlayerMatchStatistics og Player-entitetene.
 */
public class PlayerMatchStatisticsDto {

    // Spillerinfo
    private Integer playerId;
    private String playerName;
    private Integer teamId;

    // Generell kampinfo
    private Integer minutesPlayed;
    private String rating;
    private boolean captain;
    private boolean substitute;

    // Angrepsstatistikk
    private Integer shotsTotal;
    private Integer shotsOnGoal;
    private Integer goalsTotal;
    private Integer assists;

    // Pasningsstatistikk
    private Integer passesTotal;
    private Integer passesKey;

    // Nødvendig tom konstruktør for mapping
    public PlayerMatchStatisticsDto() {}

    // Getters and Setters
    public Integer getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Integer playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public Integer getMinutesPlayed() {
        return minutesPlayed;
    }

    public void setMinutesPlayed(Integer minutesPlayed) {
        this.minutesPlayed = minutesPlayed;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public boolean isCaptain() {
        return captain;
    }

    public void setCaptain(boolean captain) {
        this.captain = captain;
    }

    public boolean isSubstitute() {
        return substitute;
    }

    public void setSubstitute(boolean substitute) {
        this.substitute = substitute;
    }

    public Integer getShotsTotal() {
        return shotsTotal;
    }

    public void setShotsTotal(Integer shotsTotal) {
        this.shotsTotal = shotsTotal;
    }

    public Integer getShotsOnGoal() {
        return shotsOnGoal;
    }

    public void setShotsOnGoal(Integer shotsOnGoal) {
        this.shotsOnGoal = shotsOnGoal;
    }

    public Integer getGoalsTotal() {
        return goalsTotal;
    }

    public void setGoalsTotal(Integer goalsTotal) {
        this.goalsTotal = goalsTotal;
    }

    public Integer getAssists() {
        return assists;
    }

    public void setAssists(Integer assists) {
        this.assists = assists;
    }

    public Integer getPassesTotal() {
        return passesTotal;
    }

    public void setPassesTotal(Integer passesTotal) {
        this.passesTotal = passesTotal;
    }

    public Integer getPassesKey() {
        return passesKey;
    }

    public void setPassesKey(Integer passesKey) {
        this.passesKey = passesKey;
    }
}