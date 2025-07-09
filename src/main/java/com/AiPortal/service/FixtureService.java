// src/main/java/com/AiPortal/service/FixtureService.java (NY FIL)
package com.AiPortal.service;

import com.AiPortal.dto.TeamDetailsDto;
import com.AiPortal.entity.Fixture;
import com.AiPortal.repository.FixtureRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class FixtureService {

    private final FixtureRepository fixtureRepository;
    private final FootballApiService footballApiService;
    private final ObjectMapper objectMapper;

    public FixtureService(FixtureRepository fixtureRepository, FootballApiService footballApiService, ObjectMapper objectMapper) {
        this.fixtureRepository = fixtureRepository;
        this.footballApiService = footballApiService;
        this.objectMapper = objectMapper;
    }

    public Optional<TeamDetailsDto> getTeamDetails(Integer teamId, Integer season) {
        // Hent kamplisten fra vår database
        List<Fixture> fixtures = fixtureRepository.findFixturesByTeamAndSeason(teamId, season);

        // Hvis vi ikke finner noen kamper, er det ikke noe å vise
        if (fixtures.isEmpty()) {
            return Optional.empty();
        }

        // Hent lagnavnet fra en pålitelig kilde: /teams endepunktet
        String teamName = fetchTeamName(teamId).orElse("Lag ID: " + teamId);

        // Lag og returner den komplette DTO-en
        return Optional.of(new TeamDetailsDto(teamName, season, fixtures));
    }

    private Optional<String> fetchTeamName(Integer teamId) {
        try {
            ResponseEntity<String> response = footballApiService.getTeamById(teamId).block(); // Ny metode i ApiService
            if (response != null && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode responseArray = root.path("response");
                if (responseArray.isArray() && !responseArray.isEmpty()) {
                    return Optional.ofNullable(responseArray.get(0).path("team").path("name").asText(null));
                }
            }
        } catch (Exception e) {
            // Ignorer feil, vi har en fallback
        }
        return Optional.empty();
    }
}