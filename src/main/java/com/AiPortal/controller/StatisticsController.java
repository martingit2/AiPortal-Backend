// src/main/java/com/AiPortal/controller/StatisticsController.java
package com.AiPortal.controller;

import com.AiPortal.dto.LeagueStatsGroupDto; // <-- Ny import
import com.AiPortal.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * @return En liste med LeagueStatsGroupDto.
     */
    @GetMapping("/teams/grouped") // Endret URL for å ikke brekke gammel funksjonalitet
    public ResponseEntity<List<LeagueStatsGroupDto>> getGroupedTeamStatistics() {
        List<LeagueStatsGroupDto> groupedStats = statisticsService.getGroupedTeamStatistics();
        return ResponseEntity.ok(groupedStats);
    }
}