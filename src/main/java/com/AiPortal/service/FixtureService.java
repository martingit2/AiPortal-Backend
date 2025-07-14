// src/main/java/com/AiPortal/service/FixtureService.java
package com.AiPortal.service;

import com.AiPortal.dto.TeamDetailsDto;
import com.AiPortal.dto.UpcomingFixtureDto;
import com.AiPortal.entity.Fixture;
import com.AiPortal.entity.League;
import com.AiPortal.entity.MatchOdds;
import com.AiPortal.repository.FixtureRepository;
import com.AiPortal.repository.LeagueRepository;
import com.AiPortal.repository.MatchOddsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger; // NY IMPORT
import org.slf4j.LoggerFactory; // NY IMPORT
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects; // NY IMPORT
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FixtureService {

    private static final Logger log = LoggerFactory.getLogger(FixtureService.class); // NYTT LOGGER-OBJEKT

    private final FixtureRepository fixtureRepository;
    private final FootballApiService footballApiService;
    private final ObjectMapper objectMapper;
    private final MatchOddsRepository matchOddsRepository;
    private final LeagueRepository leagueRepository;

    private static final List<String> FINISHED_STATUSES = Arrays.asList("FT", "AET", "PEN");

    public FixtureService(
            FixtureRepository fixtureRepository,
            FootballApiService footballApiService,
            ObjectMapper objectMapper,
            MatchOddsRepository matchOddsRepository,
            LeagueRepository leagueRepository
    ) {
        this.fixtureRepository = fixtureRepository;
        this.footballApiService = footballApiService;
        this.objectMapper = objectMapper;
        this.matchOddsRepository = matchOddsRepository;
        this.leagueRepository = leagueRepository;
    }

    public List<UpcomingFixtureDto> getUpcomingFixturesWithOdds() {
        List<Fixture> upcomingFixtures = fixtureRepository.findAllByDateAfterOrderByDateAsc(Instant.now());
        if (upcomingFixtures.isEmpty()) {
            return List.of();
        }

        List<Long> fixtureIds = upcomingFixtures.stream().map(Fixture::getId).collect(Collectors.toList());

        Map<Long, List<MatchOdds>> oddsByFixtureId = matchOddsRepository.findAllByFixtureIdIn(fixtureIds)
                .stream().collect(Collectors.groupingBy(mo -> mo.getFixture().getId()));

        Set<Integer> leagueIds = upcomingFixtures.stream()
                .map(Fixture::getLeagueId)
                .filter(Objects::nonNull) // KORRIGERING: Håndterer kamper uten leagueId
                .collect(Collectors.toSet());

        Map<Integer, String> leagueNames = leagueRepository.findAllById(leagueIds).stream()
                .collect(Collectors.toMap(League::getId, League::getName));

        return upcomingFixtures.stream()
                .map(fixture -> new UpcomingFixtureDto(
                        fixture,
                        oddsByFixtureId.getOrDefault(fixture.getId(), List.of()),
                        leagueNames.getOrDefault(fixture.getLeagueId(), "Ukjent Liga"),
                        objectMapper // KORRIGERING: Send med riktig antall argumenter
                ))
                .collect(Collectors.toList());
    }

    public Page<Fixture> getUpcomingFixtures(Pageable pageable) {
        return fixtureRepository.findByDateAfterOrderByDateAsc(Instant.now(), pageable);
    }

    public Page<Fixture> getResultFixtures(Pageable pageable) {
        return fixtureRepository.findByDateBeforeAndStatusInOrderByDateDesc(Instant.now(), FINISHED_STATUSES, pageable);
    }

    public Optional<TeamDetailsDto> getTeamDetails(Integer teamId, Integer season) {
        List<Fixture> fixtures = fixtureRepository.findFixturesByTeamAndSeason(teamId, season);
        if (fixtures.isEmpty()) {
            return Optional.empty();
        }
        String teamName = fetchTeamName(teamId).orElse("Lag ID: " + teamId);
        return Optional.of(new TeamDetailsDto(teamName, season, fixtures));
    }

    private Optional<String> fetchTeamName(Integer teamId) {
        try {
            ResponseEntity<String> response = footballApiService.getTeamById(teamId).block();
            if (response != null && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode responseArray = root.path("response");
                if (responseArray.isArray() && !responseArray.isEmpty()) {
                    return Optional.ofNullable(responseArray.get(0).path("team").path("name").asText(null));
                }
            }
        } catch (Exception e) {
            log.error("Kunne ikke hente lagnavn for ID {}: {}", teamId, e.getMessage());
        }
        return Optional.empty();
    }
}