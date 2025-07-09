// src/main/java/com/AiPortal/controller/AdminController.java

package com.AiPortal.controller;

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
        return ResponseEntity.ok("Sport-statistikk-bot (enkelt-lag) kjøring manuelt utløst.");
    }

    @PostMapping("/run-league-stats-collector")
    public ResponseEntity<String> runLeagueStatsCollector() {
        scheduledBotRunner.runLeagueStatsCollector();
        return ResponseEntity.ok("Liga-statistikk-innsamler kjøring manuelt utløst. Jobben kjører i bakgrunnen.");
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
     * NYTT ENDEPUNKT: Trigger innsamling av detaljert historisk kampdata.
     * @return En bekreftelse på at den langvarige jobben er startet i bakgrunnen.
     */
    @PostMapping("/run-historical-collector")
    public ResponseEntity<String> runHistoricalCollector() {
        scheduledBotRunner.runHistoricalDataCollector();
        return ResponseEntity.ok("Historisk datainnsamler kjøring manuelt utløst. Dette vil kjøre i bakgrunnen.");
    }
}