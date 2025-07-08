// src/main/java/com/AiPortal/controller/AdminController.java

package com.AiPortal.controller;

import com.AiPortal.service.ScheduledBotRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * En sikret controller for administrative oppgaver, som å manuelt trigge planlagte jobber.
 * I en ekte app ville du sikret denne til kun admin-brukere.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final ScheduledBotRunner scheduledBotRunner;

    @Autowired
    public AdminController(ScheduledBotRunner scheduledBotRunner) {
        this.scheduledBotRunner = scheduledBotRunner;
    }

    @PostMapping("/run-twitter-bot")
    public ResponseEntity<String> runTwitterBot() {
        scheduledBotRunner.runTwitterSearchBot();
        return ResponseEntity.ok("Twitter-bot kjøring manuelt utløst.");
    }

    @PostMapping("/run-sport-stats-bot")
    public ResponseEntity<String> runSportStatsBot() {
        scheduledBotRunner.runSportDataBots();
        return ResponseEntity.ok("Sport-statistikk-bot kjøring manuelt utløst.");
    }

    @PostMapping("/run-metadata-bot")
    public ResponseEntity<String> runMetadataBot() {
        scheduledBotRunner.updateFootballMetadata();
        return ResponseEntity.ok("Fotball-metadata-bot kjøring manuelt utløst.");
    }

    @PostMapping("/run-odds-bot")
    public ResponseEntity<String> runOddsBot() {
        scheduledBotRunner.fetchDailyOdds();
        return ResponseEntity.ok("Odds-bot kjøring manuelt utløst.");
    }

    /**
     * NYTT ENDEPUNKT: Trigger den nye liga-statistikk-innsamleren.
     * @return En bekreftelse på at jobben er startet.
     */
    @PostMapping("/run-league-stats-collector")
    public ResponseEntity<String> runLeagueStatsCollector() {
        scheduledBotRunner.runLeagueStatsCollector();
        return ResponseEntity.ok("Liga-statistikk-innsamler kjøring manuelt utløst.");
    }
}