// src/main/java/com/AiPortal/controller/AdminController.java
package com.AiPortal.controller;

import com.AiPortal.service.BetSettlementRunner;
import com.AiPortal.service.BettingSimulationRunner;
import com.AiPortal.service.ScheduledBotRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final ScheduledBotRunner scheduledBotRunner;
    private final BettingSimulationRunner bettingSimulationRunner;
    private final BetSettlementRunner betSettlementRunner;

    @Autowired
    public AdminController(ScheduledBotRunner scheduledBotRunner,
                           BettingSimulationRunner bettingSimulationRunner,
                           BetSettlementRunner betSettlementRunner) {
        this.scheduledBotRunner = scheduledBotRunner;
        this.bettingSimulationRunner = bettingSimulationRunner;
        this.betSettlementRunner = betSettlementRunner;
    }



    @PostMapping("/run-betting-simulation")
    public ResponseEntity<String> runBettingSimulation() {
        bettingSimulationRunner.findAndPlaceBets();
        return ResponseEntity.ok("Betting-simulering manuelt utløst. Sjekk logger for detaljer.");
    }

    @PostMapping("/run-bet-settlement")
    public ResponseEntity<String> runBetSettlement() {
        betSettlementRunner.settleBets();
        return ResponseEntity.ok("Bet-avgjøringsjobb manuelt utløst. Sjekk logger for detaljer.");
    }

    @PostMapping("/run-twitter-bot")
    public ResponseEntity<String> runTwitterBot() {
        scheduledBotRunner.runTwitterSearchBot();
        return ResponseEntity.ok("Twitter-bot kjøring manuelt utløst.");
    }

    @PostMapping("/run-sport-stats-bot")
    public ResponseEntity<String> runSportStatsBot() {
        scheduledBotRunner.runSportDataBots();
        return ResponseEntity.ok("Sport-statistikk-bot (enkelt-lag) kjøring manuelt utløst.");
    }

    @PostMapping("/run-metadata-bot")
    public ResponseEntity<String> runMetadataBot() {
        scheduledBotRunner.updateFootballMetadata();
        return ResponseEntity.ok("Fotball-metadata-bot kjøring manuelt utløst.");
    }

    @PostMapping("/cleanup-incomplete-fixtures")
    public ResponseEntity<String> cleanupIncompleteFixtures() {
        int count = scheduledBotRunner.cleanupIncompleteFixtures();
        return ResponseEntity.ok("Opprydding fullført. Slettet " + count + " ufullstendige kamper.");
    }

    // --- OPPDATERING: Tunge jobber som nå returnerer 202 Accepted ---

    @PostMapping("/run-league-stats-collector")
    public ResponseEntity<String> runLeagueStatsCollector() {
        scheduledBotRunner.runLeagueStatsCollector();
        // OPPDATERING: Returnerer umiddelbart
        return ResponseEntity.accepted().body("Liga-statistikk-innsamler er startet og kjører nå i bakgrunnen.");
    }

    @PostMapping("/run-odds-bot")
    public ResponseEntity<String> runOddsBot() {
        scheduledBotRunner.fetchDailyOdds();
        // OPPDATERING: Returnerer umiddelbart
        return ResponseEntity.accepted().body("Api-Sports Odds-bot er startet og kjører nå i bakgrunnen.");
    }

    @PostMapping("/run-pinnacle-odds-bot")
    public ResponseEntity<String> runPinnacleOddsBot() {
        scheduledBotRunner.fetchPinnacleOdds();
        // OPPDATERING: Returnerer umiddelbart
        return ResponseEntity.accepted().body("Pinnacle Odds-bot er startet og kjører nå i bakgrunnen.");
    }

    @PostMapping("/run-historical-collector")
    public ResponseEntity<String> runHistoricalCollector() {
        scheduledBotRunner.runHistoricalDataCollector();
        // OPPDATERING: Returnerer umiddelbart
        return ResponseEntity.accepted().body("Forberedelse for historisk datainnsamler er startet i bakgrunnen.");
    }
}