// src/main/java/com/AiPortal/dto/MatchOddsDto.java
package com.AiPortal.dto;

import com.AiPortal.entity.MatchOdds;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode; // Importer ObjectNode

import java.util.ArrayList;
import java.util.List;

public class MatchOddsDto {
    private String bookmakerName;
    private String betName;
    private List<OddDetailDto> odds;

    // Tom konstruktør for Jackson
    public MatchOddsDto() {}

    /**
     * KORRIGERT: Konstruktøren aksepterer nå ObjectMapper som det andre argumentet.
     */
    public MatchOddsDto(MatchOdds matchOdds, ObjectMapper objectMapper) {
        this.bookmakerName = matchOdds.getBookmaker() != null ? matchOdds.getBookmaker().getName() : "Ukjent";
        this.betName = matchOdds.getBetName();
        this.odds = new ArrayList<>();

        try {
            // Sjekker om oddsData-strengen faktisk har innhold
            String oddsDataString = matchOdds.getOddsData();
            if (oddsDataString != null && !oddsDataString.isEmpty()) {
                JsonNode oddsData = objectMapper.readTree(oddsDataString);
                if (oddsData.isArray()) {
                    for (JsonNode valueNode : oddsData) {
                        this.odds.add(new OddDetailDto(valueNode));
                    }
                }
            }
        } catch (JsonProcessingException e) {
            // Håndter feil, f.eks. ved å la listen være tom eller logge en feil
            // For nå lar vi listen være tom, slik at appen ikke krasjer.
        }
    }

    // Getters and Setters
    public String getBookmakerName() { return bookmakerName; }
    public void setBookmakerName(String bookmakerName) { this.bookmakerName = bookmakerName; }
    public String getBetName() { return betName; }
    public void setBetName(String betName) { this.betName = betName; }
    public List<OddDetailDto> getOdds() { return odds; }
    public void setOdds(List<OddDetailDto> odds) { this.odds = odds; }
}

// En indre klasse for å representere ett enkelt odds-valg
class OddDetailDto {
    private String name;
    private String handicap;
    private String points;
    private double odds;

    // Tom konstruktør for Jackson
    public OddDetailDto() {}

    public OddDetailDto(JsonNode valueNode) {
        this.name = valueNode.path("name").asText(null);
        this.handicap = valueNode.path("handicap").asText(null);
        this.points = valueNode.path("points").asText(null);
        this.odds = valueNode.path("odds").asDouble();
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHandicap() { return handicap; }
    public void setHandicap(String handicap) { this.handicap = handicap; }
    public String getPoints() { return points; }
    public void setPoints(String points) { this.points = points; }
    public double getOdds() { return odds; }
    public void setOdds(double odds) { this.odds = odds; }
}