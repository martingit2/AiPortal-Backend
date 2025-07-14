// src/main/java/com/AiPortal/controller/FixtureController.java
package com.AiPortal.controller;

import com.AiPortal.dto.TeamDetailsDto;
import com.AiPortal.dto.UpcomingFixtureDto;
import com.AiPortal.entity.Fixture;
import com.AiPortal.service.FixtureService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fixtures")
public class FixtureController {

    private final FixtureService fixtureService;

    public FixtureController(FixtureService fixtureService) {
        this.fixtureService = fixtureService;
    }

    /**
     * Henter en paginert liste over kommende kamper (kun grunnleggende info).
     * Brukes på "Kampoversikt"-siden.
     */
    @GetMapping("/upcoming")
    public ResponseEntity<Page<Fixture>> getUpcomingFixtures(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Fixture> upcomingFixtures = fixtureService.getUpcomingFixtures(pageable);
        return ResponseEntity.ok(upcomingFixtures);
    }

    /**
     * NYTT ENDEPUNKT: Henter en komplett liste over alle kommende kamper
     * med tilhørende odds-informasjon. Brukes på den nye "Kommende Odds"-siden.
     * @return En liste med DTOer som gir en full oversikt.
     */
    @GetMapping("/upcoming-with-odds")
    public ResponseEntity<List<UpcomingFixtureDto>> getUpcomingFixturesWithOdds() {
        List<UpcomingFixtureDto> fixtures = fixtureService.getUpcomingFixturesWithOdds();
        return ResponseEntity.ok(fixtures);
    }

    /**
     * Henter en paginert liste over spilte kamper (resultater).
     * Brukes på "Kampoversikt"-siden.
     */
    @GetMapping("/results")
    public ResponseEntity<Page<Fixture>> getResultFixtures(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Fixture> resultFixtures = fixtureService.getResultFixtures(pageable);
        return ResponseEntity.ok(resultFixtures);
    }

    /**
     * Henter detaljer for ett enkelt lag. Brukes på "Lagdetaljer"-siden.
     */
    @GetMapping("/team-details/team/{teamId}/season/{season}")
    public ResponseEntity<TeamDetailsDto> getTeamDetails(
            @PathVariable Integer teamId,
            @PathVariable Integer season) {

        return fixtureService.getTeamDetails(teamId, season)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}