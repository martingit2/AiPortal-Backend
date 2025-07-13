// src/main/java/com/AiPortal/entity/PlayerMatchStatistics.java
package com.AiPortal.entity;

import jakarta.persistence.*;

/**
 * Lagrer den detaljerte statistikken for en enkelt spiller i en enkelt kamp.
 * Dette er kjernen i den nye, rike dataen for ML-modellen.
 */
@Entity
@Table(name = "player_match_statistics")
public class PlayerMatchStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Koblinger for kontekst
    @Column(nullable = false)
    private Long fixtureId;

    @Column(nullable = false)
    private Integer teamId;

    @Column(nullable = false)
    private Integer playerId;

    // Generell kampinformasjon
    private Integer minutesPlayed;
    private String rating; // Kan være f.eks. "7.5", lagres som streng for fleksibilitet
    private boolean captain;
    private boolean substitute;

    // Angrepsstatistikk
    private Integer shotsTotal;
    private Integer shotsOnGoal;
    private Integer goalsTotal;
    private Integer goalsConceded;
    private Integer assists;
    private Integer saves; // For keepere

    // Pasningsstatistikk
    private Integer passesTotal;
    private Integer passesKey; // Nøkkelpasninger
    private String passesAccuracy; // Lagres som streng, f.eks. "85%"

    // Forsvarsstatistikk
    private Integer tacklesTotal;
    private Integer tacklesBlocks;
    private Integer tacklesInterceptions;

    // Driblinger
    private Integer dribblesAttempts;
    private Integer dribblesSuccess;

    // Dueller
    private Integer duelsTotal;
    private Integer duelsWon;

    // Feil og kort
    private Integer foulsDrawn; // Frispark forårsaket
    private Integer foulsCommitted; // Frispark begått
    private Integer cardsYellow;
    private Integer cardsRed;

    // Straffer
    private Integer penaltyWon;
    private Integer penaltyCommitted;
    private Integer penaltyScored;
    private Integer penaltyMissed;
    private Integer penaltySaved; // For keepere


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

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Integer playerId) {
        this.playerId = playerId;
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

    public Integer getGoalsConceded() {
        return goalsConceded;
    }

    public void setGoalsConceded(Integer goalsConceded) {
        this.goalsConceded = goalsConceded;
    }

    public Integer getAssists() {
        return assists;
    }

    public void setAssists(Integer assists) {
        this.assists = assists;
    }


    public Integer getSaves() {
        return saves;
    }

    public void setSaves(Integer saves) {
        this.saves = saves;
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

    public String getPassesAccuracy() {
        return passesAccuracy;
    }

    public void setPassesAccuracy(String passesAccuracy) {
        this.passesAccuracy = passesAccuracy;
    }

    public Integer getTacklesTotal() {
        return tacklesTotal;
    }

    public void setTacklesTotal(Integer tacklesTotal) {
        this.tacklesTotal = tacklesTotal;
    }

    public Integer getTacklesBlocks() {
        return tacklesBlocks;
    }

    public void setTacklesBlocks(Integer tacklesBlocks) {
        this.tacklesBlocks = tacklesBlocks;
    }

    public Integer getTacklesInterceptions() {
        return tacklesInterceptions;
    }

    public void setTacklesInterceptions(Integer tacklesInterceptions) {
        this.tacklesInterceptions = tacklesInterceptions;
    }

    public Integer getDribblesAttempts() {
        return dribblesAttempts;
    }

    public void setDribblesAttempts(Integer dribblesAttempts) {
        this.dribblesAttempts = dribblesAttempts;
    }

    public Integer getDribblesSuccess() {
        return dribblesSuccess;
    }

    public void setDribblesSuccess(Integer dribblesSuccess) {
        this.dribblesSuccess = dribblesSuccess;
    }

    public Integer getDuelsTotal() {
        return duelsTotal;
    }

    public void setDuelsTotal(Integer duelsTotal) {
        this.duelsTotal = duelsTotal;
    }

    public Integer getDuelsWon() {
        return duelsWon;
    }

    public void setDuelsWon(Integer duelsWon) {
        this.duelsWon = duelsWon;
    }

    public Integer getFoulsDrawn() {
        return foulsDrawn;
    }

    public void setFoulsDrawn(Integer foulsDrawn) {
        this.foulsDrawn = foulsDrawn;
    }

    public Integer getFoulsCommitted() {
        return foulsCommitted;
    }

    public void setFoulsCommitted(Integer foulsCommitted) {
        this.foulsCommitted = foulsCommitted;
    }

    public Integer getCardsYellow() {
        return cardsYellow;
    }

    public void setCardsYellow(Integer cardsYellow) {
        this.cardsYellow = cardsYellow;
    }

    public Integer getCardsRed() {
        return cardsRed;
    }

    public void setCardsRed(Integer cardsRed) {
        this.cardsRed = cardsRed;
    }

    public Integer getPenaltyWon() {
        return penaltyWon;
    }

    public void setPenaltyWon(Integer penaltyWon) {
        this.penaltyWon = penaltyWon;
    }

    public Integer getPenaltyCommitted() {
        return penaltyCommitted;
    }

    public void setPenaltyCommitted(Integer penaltyCommitted) {
        this.penaltyCommitted = penaltyCommitted;
    }

    public Integer getPenaltyScored() {
        return penaltyScored;
    }

    public void setPenaltyScored(Integer penaltyScored) {
        this.penaltyScored = penaltyScored;
    }

    public Integer getPenaltyMissed() {
        return penaltyMissed;
    }

    public void setPenaltyMissed(Integer penaltyMissed) {
        this.penaltyMissed = penaltyMissed;
    }

    public Integer getPenaltySaved() {
        return penaltySaved;
    }

    public void setPenaltySaved(Integer penaltySaved) {
        this.penaltySaved = penaltySaved;
    }
}