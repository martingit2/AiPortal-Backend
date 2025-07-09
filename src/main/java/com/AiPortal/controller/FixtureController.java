// src/main/java/com/AiPortal/controller/FixtureController.java
package com.AiPortal.controller;

import com.AiPortal.dto.TeamDetailsDto;
import com.AiPortal.service.FixtureService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fixtures")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class FixtureController {

    private final FixtureService fixtureService;

    public FixtureController(FixtureService fixtureService) {
        this.fixtureService = fixtureService;
    }

    @GetMapping("/team-details/team/{teamId}/season/{season}")
    public ResponseEntity<TeamDetailsDto> getTeamDetails(
            @PathVariable Integer teamId,
            @PathVariable Integer season) {

        return fixtureService.getTeamDetails(teamId, season)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}