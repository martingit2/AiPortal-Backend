// src/main/java/com/AiPortal/dto/TeamStatisticsDto.java
package com.AiPortal.dto;

public class TeamStatisticsDto {

    private Long id;
    private Integer teamId;
    private String teamName;
    private String leagueName;
    private int season;
    private int playedTotal;
    private int winsTotal;
    private int drawsTotal;
    private int lossesTotal;
    private int goalsForTotal;
    private int goalsAgainstTotal;
    private String sourceBotName;

    // Tom konstruktør
    public TeamStatisticsDto() {
    }

    // Konstruktør for enkel mapping
    public TeamStatisticsDto(Long id, Integer teamId, String teamName, String leagueName, int season, int playedTotal, int winsTotal, int drawsTotal, int lossesTotal, int goalsForTotal, int goalsAgainstTotal, String sourceBotName) {
        this.id = id;
        this.teamId = teamId;
        this.teamName = teamName;
        this.leagueName = leagueName;
        this.season = season;
        this.playedTotal = playedTotal;
        this.winsTotal = winsTotal;
        this.drawsTotal = drawsTotal;
        this.lossesTotal = lossesTotal;
        this.goalsForTotal = goalsForTotal;
        this.goalsAgainstTotal = goalsAgainstTotal;
        this.sourceBotName = sourceBotName;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getTeamId() { return teamId; }
    public void setTeamId(Integer teamId) { this.teamId = teamId; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getLeagueName() { return leagueName; }
    public void setLeagueName(String leagueName) { this.leagueName = leagueName; }
    public int getSeason() { return season; }
    public void setSeason(int season) { this.season = season; }
    public int getPlayedTotal() { return playedTotal; }
    public void setPlayedTotal(int playedTotal) { this.playedTotal = playedTotal; }
    public int getWinsTotal() { return winsTotal; }
    public void setWinsTotal(int winsTotal) { this.winsTotal = winsTotal; }
    public int getDrawsTotal() { return drawsTotal; }
    public void setDrawsTotal(int drawsTotal) { this.drawsTotal = drawsTotal; }
    public int getLossesTotal() { return lossesTotal; }
    public void setLossesTotal(int lossesTotal) { this.lossesTotal = lossesTotal; }
    public int getGoalsForTotal() { return goalsForTotal; }
    public void setGoalsForTotal(int goalsForTotal) { this.goalsForTotal = goalsForTotal; }
    public int getGoalsAgainstTotal() { return goalsAgainstTotal; }
    public void setGoalsAgainstTotal(int goalsAgainstTotal) { this.goalsAgainstTotal = goalsAgainstTotal; }
    public String getSourceBotName() { return sourceBotName; }
    public void setSourceBotName(String sourceBotName) { this.sourceBotName = sourceBotName; }
}