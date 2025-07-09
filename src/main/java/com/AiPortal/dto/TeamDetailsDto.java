// src/main/java/com/AiPortal/dto/TeamDetailsDto.java (NY FIL)
package com.AiPortal.dto;

import com.AiPortal.entity.Fixture;
import java.util.List;

public class TeamDetailsDto {
    private String teamName;
    private int season;
    private List<Fixture> fixtures;

    public TeamDetailsDto(String teamName, int season, List<Fixture> fixtures) {
        this.teamName = teamName;
        this.season = season;
        this.fixtures = fixtures;
    }

    // Getters
    public String getTeamName() { return teamName; }
    public int getSeason() { return season; }
    public List<Fixture> getFixtures() { return fixtures; }
}