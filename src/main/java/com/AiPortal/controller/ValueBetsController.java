// src/main/java/com/AiPortal/controller/ValueBetsController.java
package com.AiPortal.controller;

import com.AiPortal.dto.ValueBetDto;
import com.AiPortal.entity.Fixture;
import com.AiPortal.repository.FixtureRepository;
import com.AiPortal.repository.MatchOddsRepository;
import com.AiPortal.service.OddsCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/value-bets")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ValueBetsController {

    private static final Logger log = LoggerFactory.getLogger(ValueBetsController.class);

    private final FixtureRepository fixtureRepository;
    private final OddsCalculationService oddsCalculationService;
    private final MatchOddsRepository matchOddsRepository;

    public ValueBetsController(FixtureRepository fixtureRepository, OddsCalculationService oddsCalculationService, MatchOddsRepository matchOddsRepository) {
        this.fixtureRepository = fixtureRepository;
        this.oddsCalculationService = oddsCalculationService;
        this.matchOddsRepository = matchOddsRepository;
    }

    @GetMapping
    public ResponseEntity<List<ValueBetDto>> findValueBets() {

        // --- NY, SMARTERE STRATEGI ---
        // 1. Finn ID-ene til alle kommende kamper som FAKTISK HAR ODDS.
        List<Long> fixtureIdsWithOdds = matchOddsRepository.findDistinctFixtureIdsWithUpcomingOdds(Instant.now());

        if (fixtureIdsWithOdds.isEmpty()) {
            log.info("Fant ingen kommende kamper med odds i databasen. Returnerer tom liste.");
            return ResponseEntity.ok(List.of());
        }

        // 2. Hent de fulle Fixture-objektene for disse ID-ene.
        List<Fixture> fixturesToAnalyze = fixtureRepository.findAllById(fixtureIdsWithOdds);

        // 3. Sorter listen etter dato og ta de første 50 for å begrense analysen.
        fixturesToAnalyze.sort(Comparator.comparing(Fixture::getDate));
        List<Fixture> upcomingFixtures = fixturesToAnalyze.stream().limit(50).collect(Collectors.toList());
        // --- SLUTT PÅ NY STRATEGI ---

        log.info("Fant {} kamp(er) med odds for parallell analyse.", upcomingFixtures.size());

        List<ValueBetDto> valueBets = Flux.fromIterable(upcomingFixtures)
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .flatMap(fixture -> {
                    log.info("Prosessering startet for kamp ID: {}", fixture.getId());
                    return Flux.fromIterable(oddsCalculationService.calculateValue(fixture));
                })
                .sequential()
                .collectList()
                .block(Duration.ofSeconds(60));

        log.info("Returnerer {} value bet-signaler etter parallell prosessering.", valueBets != null ? valueBets.size() : 0);

        return ResponseEntity.ok(valueBets);
    }
}