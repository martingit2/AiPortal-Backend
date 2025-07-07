package com.AiPortal.controller;

import com.AiPortal.dto.ValueBetDto;
import com.AiPortal.entity.Fixture;
import com.AiPortal.repository.FixtureRepository;
import com.AiPortal.service.OddsCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
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
        // Hent f.eks. de 50 første kommende kampene for analyse
        List<Fixture> upcomingFixtures = fixtureRepository.findAll(PageRequest.of(0, 50)).getContent();
        log.info("Fant {} kommende kamper fra databasen for analyse.", upcomingFixtures.size());

        if (upcomingFixtures.isEmpty()) {
            log.warn("Ingen kommende kamper funnet i 'fixtures'-tabellen. Har odds-boten kjørt og hentet data for morgendagen?");
        }

        List<ValueBetDto> valueBets = upcomingFixtures.stream()
                .peek(fixture -> log.info("--- Prosesserer kamp ID: {} ({} vs {}) ---",
                        fixture.getId(), fixture.getHomeTeamName(), fixture.getAwayTeamName()))
                .map(oddsCalculationService::calculateValue)
                .filter(Optional::isPresent)
                .map(Optional::get)
                // Filteret for å kun vise positive value bets er fjernet for feilsøking.
                // .filter(bet -> bet.getValueHome() > 0.05 || bet.getValueDraw() > 0.05 || bet.getValueAway() > 0.05)
                .collect(Collectors.toList());

        log.info("Returnerer {} value bets etter prosessering.", valueBets.size());
        return ResponseEntity.ok(valueBets);
    }
}