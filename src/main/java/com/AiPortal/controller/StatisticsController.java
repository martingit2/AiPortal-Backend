// src/main/java/com/AiPortal/controller/StatisticsController.java
package com.AiPortal.controller;

import com.AiPortal.dto.LeagueStatsGroupDto;
import com.AiPortal.dto.MatchStatisticsDto;
import com.AiPortal.entity.MatchStatistics; // Importer MatchStatistics
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
     */
    @GetMapping("/teams/grouped")
    public ResponseEntity<List<LeagueStatsGroupDto>> getGroupedTeamStatistics() {
        List<LeagueStatsGroupDto> groupedStats = statisticsService.getGroupedTeamStatistics();
        return ResponseEntity.ok(groupedStats);
    }

    /**
     * Henter detaljert statistikk for en enkelt kamp.
     */
    @GetMapping("/fixture/{fixtureId}")
    public ResponseEntity<List<MatchStatisticsDto>> getFixtureStats(@PathVariable Long fixtureId) {
        List<MatchStatisticsDto> fixtureStats = statisticsService.getStatisticsForFixture(fixtureId);
        if (fixtureStats.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(fixtureStats);
    }

    /**
     * NYTT ENDEPUNKT: Henter form-statistikk for et lag.
     * @param teamId ID-en til laget.
     * @param season Sesongen.
     * @param limit Antall kamper å returnere statistikk for (default 10).
     * @return En liste med MatchStatistics-entiteter for de siste kampene.
     */
    @GetMapping("/form/team/{teamId}/season/{season}")
    public ResponseEntity<List<MatchStatistics>> getTeamFormStats(
            @PathVariable Integer teamId,
            @PathVariable Integer season,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<MatchStatistics> formStats = statisticsService.getFormStatsForTeam(teamId, season, limit);
        return ResponseEntity.ok(formStats);
    }
}