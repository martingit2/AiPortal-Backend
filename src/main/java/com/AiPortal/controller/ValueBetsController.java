// src/main/java/com/AiPortal/controller/ValueBetsController.java
package com.AiPortal.controller;

import com.AiPortal.dto.ValueBetDto;
import com.AiPortal.entity.Fixture;
import com.AiPortal.repository.FixtureRepository;
import com.AiPortal.service.OddsCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux; // Importer Flux
import reactor.core.scheduler.Schedulers; // Importer Schedulers

import java.time.Duration; // Importer Duration
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

        log.info("Fant {} kamp(er) fra databasen for parallell analyse.", upcomingFixtures.size());

        // --- OPTIMALISERING: Parallell prosessering med Project Reactor ---
        List<ValueBetDto> valueBets = Flux.fromIterable(upcomingFixtures)
                .parallel() // Gjør strømmen parallell
                .runOn(Schedulers.boundedElastic()) // Bruk en trådpool egnet for I/O-bundne oppgaver (nettverkskall)
                .flatMap(fixture -> {
                    log.info("Prosessering startet for kamp ID: {}", fixture.getId());
                    return Flux.fromIterable(oddsCalculationService.calculateValue(fixture));
                })
                .sequential() // Gå tilbake til en sekvensiell strøm for å samle resultatene
                .collectList()
                .block(Duration.ofSeconds(30)); // Blokker og vent på at alt skal bli ferdig, med en timeout

        log.info("Returnerer {} value bet-signaler etter parallell prosessering.", valueBets != null ? valueBets.size() : 0);

        return ResponseEntity.ok(valueBets);
    }
}