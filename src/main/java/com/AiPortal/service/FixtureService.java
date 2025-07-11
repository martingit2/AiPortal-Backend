// src/main/java/com/AiPortal/service/FixtureService.java
package com.AiPortal.service;

import com.AiPortal.dto.TeamDetailsDto;
import com.AiPortal.entity.Fixture;
import com.AiPortal.repository.FixtureRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page; // Importer Page
import org.springframework.data.domain.Pageable; // Importer Pageable
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant; // Importer Instant
import java.util.Arrays; // Importer Arrays
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class FixtureService {

    private final FixtureRepository fixtureRepository;
    private final FootballApiService footballApiService; // Beholdes for getTeamDetails
    private final ObjectMapper objectMapper; // Beholdes for getTeamDetails

    // Listen over statuser som vi anser som "ferdigspilt"
    private static final List<String> FINISHED_STATUSES = Arrays.asList("FT", "AET", "PEN");

    public FixtureService(FixtureRepository fixtureRepository, FootballApiService footballApiService, ObjectMapper objectMapper) {
        this.fixtureRepository = fixtureRepository;
        this.footballApiService = footballApiService;
        this.objectMapper = objectMapper;
    }

    /**
     * Henter en paginert liste over kommende kamper.
     * @param pageable Pagineringinformasjon fra controlleren.
     * @return En Page med Fixture-objekter.
     */
    public Page<Fixture> getUpcomingFixtures(Pageable pageable) {
        return fixtureRepository.findByDateAfterOrderByDateAsc(Instant.now(), pageable);
    }

    /**
     * Henter en paginert liste over spilte kamper (resultater).
     * @param pageable Pagineringinformasjon fra controlleren.
     * @return En Page med Fixture-objekter.
     */
    public Page<Fixture> getResultFixtures(Pageable pageable) {
        return fixtureRepository.findByDateBeforeAndStatusInOrderByDateDesc(Instant.now(), FINISHED_STATUSES, pageable);
    }


    /**
     * Eksisterende metode for å hente detaljer for ett enkelt lag.
     */
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