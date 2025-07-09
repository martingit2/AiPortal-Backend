// src/main/java/com/AiPortal/controller/FixtureController.java
package com.AiPortal.controller;

import com.AiPortal.entity.Fixture;
import com.AiPortal.repository.FixtureRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fixtures")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class FixtureController {

    private final FixtureRepository fixtureRepository;

    public FixtureController(FixtureRepository fixtureRepository) {
        this.fixtureRepository = fixtureRepository;
    }

    /**
     * Henter alle kamper for et spesifikt lag i en gitt sesong.
     * @param teamId Lagets ID.
     * @param season Sesongens årstall.
     * @return En liste med kamper.
     */
    @GetMapping("/team/{teamId}/season/{season}")
    public ResponseEntity<List<Fixture>> getFixturesForTeam(
            @PathVariable Integer teamId,
            @PathVariable Integer season) {

        List<Fixture> fixtures = fixtureRepository.findFixturesByTeamAndSeason(teamId, season);
        return ResponseEntity.ok(fixtures);
    }
}