// src/main/java/com/AiPortal/dto/MatchStatisticsDto.java
package com.AiPortal.dto;

public class MatchStatisticsDto {

    private String teamName;
    private Integer shotsOnGoal;
    private Integer shotsOffGoal;
    private Integer totalShots;
    private Integer blockedShots;
    private Integer shotsInsideBox;
    private Integer shotsOutsideBox;
    private Integer fouls;
    private Integer cornerKicks;
    private Integer offsides;
    private String ballPossession;
    private Integer yellowCards;
    private Integer redCards;
    private Integer goalkeeperSaves;
    private Integer totalPasses;
    private Integer passesAccurate;
    private String passesPercentage;

    // Konstruktør
    public MatchStatisticsDto(String teamName, Integer shotsOnGoal, Integer shotsOffGoal, Integer totalShots, Integer blockedShots, Integer shotsInsideBox, Integer shotsOutsideBox, Integer fouls, Integer cornerKicks, Integer offsides, String ballPossession, Integer yellowCards, Integer redCards, Integer goalkeeperSaves, Integer totalPasses, Integer passesAccurate, String passesPercentage) {
        this.teamName = teamName;
        this.shotsOnGoal = shotsOnGoal;
        this.shotsOffGoal = shotsOffGoal;
        this.totalShots = totalShots;
        this.blockedShots = blockedShots;
        this.shotsInsideBox = shotsInsideBox;
        this.shotsOutsideBox = shotsOutsideBox;
        this.fouls = fouls;
        this.cornerKicks = cornerKicks;
        this.offsides = offsides;
        this.ballPossession = ballPossession;
        this.yellowCards = yellowCards;
        this.redCards = redCards;
        this.goalkeeperSaves = goalkeeperSaves;
        this.totalPasses = totalPasses;
        this.passesAccurate = passesAccurate;
        this.passesPercentage = passesPercentage;
    }

    // Getters
    public String getTeamName() { return teamName; }
    public Integer getShotsOnGoal() { return shotsOnGoal; }
    public Integer getShotsOffGoal() { return shotsOffGoal; }
    public Integer getTotalShots() { return totalShots; }
    public Integer getBlockedShots() { return blockedShots; }
    public Integer getShotsInsideBox() { return shotsInsideBox; }
    public Integer getShotsOutsideBox() { return shotsOutsideBox; }
    public Integer getFouls() { return fouls; }
    public Integer getCornerKicks() { return cornerKicks; }
    public Integer getOffsides() { return offsides; }
    public String getBallPossession() { return ballPossession; }
    public Integer getYellowCards() { return yellowCards; }
    public Integer getRedCards() { return redCards; }
    public Integer getGoalkeeperSaves() { return goalkeeperSaves; }
    public Integer getTotalPasses() { return totalPasses; }
    public Integer getPassesAccurate() { return passesAccurate; }
    public String getPassesPercentage() { return passesPercentage; }
}