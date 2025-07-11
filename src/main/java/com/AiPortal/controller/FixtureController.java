// src/main/java/com/AiPortal/controller/FixtureController.java
package com.AiPortal.controller;

import com.AiPortal.dto.TeamDetailsDto;
import com.AiPortal.entity.Fixture; // Importer Fixture
import com.AiPortal.service.FixtureService;
import org.springframework.data.domain.Page; // Importer Page
import org.springframework.data.domain.PageRequest; // Importer PageRequest
import org.springframework.data.domain.Pageable; // Importer Pageable
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fixtures")
// MERK: Jfjernet den spesifikke @CrossOrigin her, siden vi har en global konfigurasjon i SecurityConfig.
// Hvis vi trenger den tilbake, kan vu fjerne kommentaren.
// @CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class FixtureController {

    private final FixtureService fixtureService;

    public FixtureController(FixtureService fixtureService) {
        this.fixtureService = fixtureService;
    }

    /**
     * NYTT ENDEPUNKT: Henter en paginert liste over kommende kamper.
     * Frontenden kaller denne for "Kommende"-fanen.
     * @param page Sidenummer (0-indeksert), default 0.
     * @param size Antall elementer per side, default 25.
     * @return En ResponseEntity som inneholder en Page med Fixture-objekter.
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
     * NYTT ENDEPUNKT: Henter en paginert liste over spilte kamper (resultater).
     * Frontenden kaller denne for "Resultater"-fanen.
     * @param page Sidenummer (0-indeksert), default 0.
     * @param size Antall elementer per side, default 25.
     * @return En ResponseEntity som inneholder en Page med Fixture-objekter.
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
     * Eksisterende endepunkt for å hente detaljer for ett enkelt lag.
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