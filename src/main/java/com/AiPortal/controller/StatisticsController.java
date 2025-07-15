// src/main/java/com/AiPortal/controller/StatisticsController.java
package com.AiPortal.controller;

import com.AiPortal.dto.HeadToHeadStatsDto;
import com.AiPortal.dto.LeagueStatsGroupDto;
import com.AiPortal.dto.MatchStatisticsDto;
import com.AiPortal.dto.PlayerMatchStatisticsDto;
import com.AiPortal.entity.MatchStatistics;
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

    @GetMapping("/teams/grouped")
    public ResponseEntity<List<LeagueStatsGroupDto>> getGroupedTeamStatistics() {
        List<LeagueStatsGroupDto> groupedStats = statisticsService.getGroupedTeamStatistics();
        return ResponseEntity.ok(groupedStats);
    }

    @GetMapping("/fixture/{fixtureId}")
    public ResponseEntity<List<MatchStatisticsDto>> getFixtureStats(@PathVariable Long fixtureId) {
        List<MatchStatisticsDto> fixtureStats = statisticsService.getStatisticsForFixture(fixtureId);
        if (fixtureStats.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(fixtureStats);
    }

    @GetMapping("/form/team/{teamId}/season/{season}")
    public ResponseEntity<List<MatchStatistics>> getTeamFormStats(
            @PathVariable Integer teamId,
            @PathVariable Integer season,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<MatchStatistics> formStats = statisticsService.getFormStatsForTeam(teamId, season, limit);
        return ResponseEntity.ok(formStats);
    }

    @GetMapping("/players/fixture/{fixtureId}")
    public ResponseEntity<List<PlayerMatchStatisticsDto>> getPlayerStatsForFixture(@PathVariable Long fixtureId) {
        List<PlayerMatchStatisticsDto> playerStats = statisticsService.getPlayerStatisticsForFixture(fixtureId);
        if (playerStats.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(playerStats);
    }

    /**
     * NYTT ENDEPUNKT: Henter Head-to-Head (H2H) statistikk for en gitt kamp.
     * @param fixtureId ID-en til kampen.
     * @return En DTO med H2H-statistikk, eller 404 Not Found hvis ingen data finnes.
     */
    @GetMapping("/h2h/{fixtureId}")
    public ResponseEntity<HeadToHeadStatsDto> getHeadToHeadStats(@PathVariable Long fixtureId) {
        return statisticsService.getHeadToHeadStats(fixtureId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}