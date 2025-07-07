package com.AiPortal.controller;

import com.AiPortal.dto.TeamStatisticsDto;
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

    private final StatisticsService statisticsService; // Bruker den nye servicen

    @Autowired
    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * Henter all lagret lagstatistikk som DTOs.
     * @return En liste med TeamStatisticsDto.
     */
    @GetMapping("/teams")
    public ResponseEntity<List<TeamStatisticsDto>> getAllTeamStatistics() {
        List<TeamStatisticsDto> allStatsDto = statisticsService.getAllTeamStatistics();
        return ResponseEntity.ok(allStatsDto);
    }
}