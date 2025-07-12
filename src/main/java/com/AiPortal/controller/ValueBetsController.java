// src/main/java/com/AiPortal/controller/ValueBetsController.java

package com.AiPortal.controller;

import com.AiPortal.dto.ValueBetDto;
import com.AiPortal.entity.Fixture;
import com.AiPortal.repository.FixtureRepository;
import com.AiPortal.service.OddsCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest; // Importer
import org.springframework.data.domain.Sort; // Importer
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/value-bets")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ValueBetsController {

    private static final Logger log = LoggerFactory.getLogger(ValueBetsController.class);

    private final FixtureRepository fixtureRepository;
    private final OddsCalculationService oddsCalculationService;

    public ValueBetsController(FixtureRepository fixtureRepository, OddsCalculationService oddsCalculationService) {
        this.fixtureRepository = fixtureRepository;
        this.oddsCalculationService = oddsCalculationService;
    }

    @GetMapping
    public ResponseEntity<List<ValueBetDto>> findValueBets() {
        // Hent de 50 neste kampene
        PageRequest pageRequest = PageRequest.of(0, 50, Sort.by("date").ascending());
        List<Fixture> upcomingFixtures = fixtureRepository.findAll(pageRequest).getContent();

        log.info("Fant {} kamp(er) fra databasen for analyse.", upcomingFixtures.size());

        // NY LOGIKK: Bruk flatMap til å håndtere listen av lister
        List<ValueBetDto> valueBets = upcomingFixtures.stream()
                .peek(fixture -> log.info("--- Prosesserer kamp ID: {} ({} vs {}) ---",
                        fixture.getId(), fixture.getHomeTeamName(), fixture.getAwayTeamName()))
                .flatMap(fixture -> oddsCalculationService.calculateValue(fixture).stream()) // Flater ut List<List<ValueBetDto>> til Stream<ValueBetDto>
                .collect(Collectors.toList());

        log.info("Returnerer {} value bet-signaler etter prosessering.", valueBets.size());

        return ResponseEntity.ok(valueBets);
    }
}