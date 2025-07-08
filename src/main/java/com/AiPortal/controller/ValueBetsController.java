// src/main/java/com/AiPortal/controller/ValueBetsController.java

package com.AiPortal.controller;

import com.AiPortal.dto.ValueBetDto;
import com.AiPortal.entity.Fixture;
import com.AiPortal.repository.FixtureRepository;
import com.AiPortal.service.OddsCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Fjernet PageRequest og Sort da de ikke brukes i testmodus
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections; // Importert for å håndtere listen
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

        // ==================================================================
        // === START PÅ MIDLERTIDIG TESTKODE                             ===
        // ==================================================================
        log.warn("!!! KJØRER I TESTMODUS: Henter én spesifikk kamp !!!");

        // ERSTATT DETTE TALLET MED ID-EN TIL KAMPEN DU VIL TESTE.
        // ID for New England Revolution vs Inter Miami er 1326523.
        long testFixtureId = 1326523;
        log.info("Test-modus er satt til å lete etter fixture ID: {}", testFixtureId);

        // Hent kun den ene spesifikke kampen fra databasen.
        List<Fixture> upcomingFixtures = fixtureRepository.findById(testFixtureId)
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());

        // ==================================================================
        // === SLUTT PÅ MIDLERTIDIG TESTKODE                              ===
        // === Når du er ferdig med testing, erstatt blokken over med:   ===
        // === PageRequest pageRequest = PageRequest.of(0, 50, Sort.by("date").ascending());
        // === List<Fixture> upcomingFixtures = fixtureRepository.findAll(pageRequest).getContent();
        // ==================================================================

        log.info("Fant {} kamp(er) fra databasen for analyse.", upcomingFixtures.size());

        if (upcomingFixtures.isEmpty()) {
            log.warn("Test-kampen med ID {} ble ikke funnet i databasen.", testFixtureId);
        }

        List<ValueBetDto> valueBets = upcomingFixtures.stream()
                .peek(fixture -> log.info("--- Prosesserer kamp ID: {} ({} vs {}) ---",
                        fixture.getId(), fixture.getHomeTeamName(), fixture.getAwayTeamName()))
                .map(oddsCalculationService::calculateValue)
                .filter(Optional::isPresent)
                .map(Optional::get)
                // Filteret er fortsatt fjernet for å se ALLE resultater, selv negative.
                .collect(Collectors.toList());

        log.info("Returnerer {} value bets etter prosessering.", valueBets.size());

        if (valueBets.isEmpty() && !upcomingFixtures.isEmpty()) {
            log.warn("Kampen ble funnet, men `calculateValue` returnerte ikke et resultat. Sjekk OddsCalculationService-loggen for advarsler om manglende statistikk.");
        }

        return ResponseEntity.ok(valueBets);
    }
}