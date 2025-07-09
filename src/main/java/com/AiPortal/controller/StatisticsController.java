// src/main/java/com/AiPortal/controller/StatisticsController.java
package com.AiPortal.controller;

import com.AiPortal.dto.LeagueStatsGroupDto;
import com.AiPortal.dto.MatchStatisticsDto;
import com.AiPortal.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/statistics")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Autowired
    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * Henter all lagret lagstatistikk gruppert etter liga og sesong.
     * @return En liste med LeagueStatsGroupDto-objekter for å bygge tabeller i UI.
     */
    @GetMapping("/teams/grouped")
    public ResponseEntity<List<LeagueStatsGroupDto>> getGroupedTeamStatistics() {
        List<LeagueStatsGroupDto> groupedStats = statisticsService.getGroupedTeamStatistics();
        return ResponseEntity.ok(groupedStats);
    }

    /**
     * NYTT ENDEPUNKT: Henter detaljert statistikk for en enkelt kamp.
     * @param fixtureId ID-en til kampen fra URL-stien.
     * @return En liste med DTO-er for kampstatistikk, typisk to (hjemme og borte).
     */
    @GetMapping("/fixture/{fixtureId}")
    public ResponseEntity<List<MatchStatisticsDto>> getFixtureStats(@PathVariable Long fixtureId) {
        List<MatchStatisticsDto> fixtureStats = statisticsService.getStatisticsForFixture(fixtureId);
        if (fixtureStats.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(fixtureStats);
    }
}