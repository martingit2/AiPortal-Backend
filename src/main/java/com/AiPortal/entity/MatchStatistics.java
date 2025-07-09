// src/main/java/com/AiPortal/entity/MatchStatistics.java
package com.AiPortal.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "match_statistics")
public class MatchStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Kobling til kampen og laget det gjelder
    @Column(nullable = false)
    private Long fixtureId;

    @Column(nullable = false)
    private Integer teamId;

    // Statistikk-felter
    private Integer shotsOnGoal;
    private Integer shotsOffGoal;
    private Integer totalShots;
    private Integer blockedShots;
    private Integer shotsInsideBox;
    private Integer shotsOutsideBox;
    private Integer fouls;
    private Integer cornerKicks;
    private Integer offsides;
    private String ballPossession; // Lagres som streng, f.eks. "55%"
    private Integer yellowCards;
    private Integer redCards;
    private Integer goalkeeperSaves;
    private Integer totalPasses;
    private Integer passesAccurate;
    private String passesPercentage;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFixtureId() { return fixtureId; }
    public void setFixtureId(Long fixtureId) { this.fixtureId = fixtureId; }
    public Integer getTeamId() { return teamId; }
    public void setTeamId(Integer teamId) { this.teamId = teamId; }
    public Integer getShotsOnGoal() { return shotsOnGoal; }
    public void setShotsOnGoal(Integer shotsOnGoal) { this.shotsOnGoal = shotsOnGoal; }
    public Integer getShotsOffGoal() { return shotsOffGoal; }
    public void setShotsOffGoal(Integer shotsOffGoal) { this.shotsOffGoal = shotsOffGoal; }
    public Integer getTotalShots() { return totalShots; }
    public void setTotalShots(Integer totalShots) { this.totalShots = totalShots; }
    public Integer getBlockedShots() { return blockedShots; }
    public void setBlockedShots(Integer blockedShots) { this.blockedShots = blockedShots; }
    public Integer getShotsInsideBox() { return shotsInsideBox; }
    public void setShotsInsideBox(Integer shotsInsideBox) { this.shotsInsideBox = shotsInsideBox; }
    public Integer getShotsOutsideBox() { return shotsOutsideBox; }
    public void setShotsOutsideBox(Integer shotsOutsideBox) { this.shotsOutsideBox = shotsOutsideBox; }
    public Integer getFouls() { return fouls; }
    public void setFouls(Integer fouls) { this.fouls = fouls; }
    public Integer getCornerKicks() { return cornerKicks; }
    public void setCornerKicks(Integer cornerKicks) { this.cornerKicks = cornerKicks; }
    public Integer getOffsides() { return offsides; }
    public void setOffsides(Integer offsides) { this.offsides = offsides; }
    public String getBallPossession() { return ballPossession; }
    public void setBallPossession(String ballPossession) { this.ballPossession = ballPossession; }
    public Integer getYellowCards() { return yellowCards; }
    public void setYellowCards(Integer yellowCards) { this.yellowCards = yellowCards; }
    public Integer getRedCards() { return redCards; }
    public void setRedCards(Integer redCards) { this.redCards = redCards; }
    public Integer getGoalkeeperSaves() { return goalkeeperSaves; }
    public void setGoalkeeperSaves(Integer goalkeeperSaves) { this.goalkeeperSaves = goalkeeperSaves; }
    public Integer getTotalPasses() { return totalPasses; }
    public void setTotalPasses(Integer totalPasses) { this.totalPasses = totalPasses; }
    public Integer getPassesAccurate() { return passesAccurate; }
    public void setPassesAccurate(Integer passesAccurate) { this.passesAccurate = passesAccurate; }
    public String getPassesPercentage() { return passesPercentage; }
    public void setPassesPercentage(String passesPercentage) { this.passesPercentage = passesPercentage; }
}