// src/main/java/com/AiPortal/dto/UpcomingFixtureDto.java
package com.AiPortal.dto;

import com.AiPortal.entity.Fixture;
import com.AiPortal.entity.MatchOdds;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class UpcomingFixtureDto {

    private Long fixtureId;
    private Instant date;
    private String homeTeamName;
    private String awayTeamName;
    private String leagueName;
    private boolean hasOdds;
    private List<MatchOddsDto> odds;

    /**
     * KORRIGERT: Konstruktøren aksepterer nå ObjectMapper som det fjerde argumentet.
     */
    public UpcomingFixtureDto(Fixture fixture, List<MatchOdds> oddsList, String leagueName, ObjectMapper objectMapper) {
        this.fixtureId = fixture.getId();
        this.date = fixture.getDate();
        this.homeTeamName = fixture.getHomeTeamName();
        this.awayTeamName = fixture.getAwayTeamName();
        this.leagueName = leagueName;
        this.hasOdds = oddsList != null && !oddsList.isEmpty();
        this.odds = oddsList != null
                ? oddsList.stream().map(o -> new MatchOddsDto(o, objectMapper)).collect(Collectors.toList())
                : List.of();
    }

    // Getters
    public Long getFixtureId() { return fixtureId; }
    public Instant getDate() { return date; }
    public String getHomeTeamName() { return homeTeamName; }
    public String getAwayTeamName() { return awayTeamName; }
    public String getLeagueName() { return leagueName; }
    public boolean isHasOdds() { return hasOdds; }
    public List<MatchOddsDto> getOdds() { return odds; }
}