// src/main/java/com/AiPortal/controller/TrainingDataController.java
package com.AiPortal.controller;

import com.AiPortal.dto.TrainingDataDto;
import com.AiPortal.service.TrainingDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for å eksponere maskinlærings-relatert data.
 * Denne er ment å bli kalt av interne tjenester (som Python-treningsskriptet),
 * og kan derfor ha enklere sikkerhet eller IP-basert tilgangskontroll i fremtiden.
 */
@RestController
@RequestMapping("/api/v1/ml-data")
public class TrainingDataController {

    private final TrainingDataService trainingDataService;

    public TrainingDataController(TrainingDataService trainingDataService) {
        this.trainingDataService = trainingDataService;
    }

    /**
     * Endepunkt for å hente det komplette, prosesserte treningssettet.
     * @return En liste med DTOer, klar for å bli konvertert til en Pandas DataFrame.
     */
    @GetMapping("/training-set")
    public ResponseEntity<List<TrainingDataDto>> getTrainingSet() {
        List<TrainingDataDto> trainingSet = trainingDataService.buildTrainingSet();
        return ResponseEntity.ok(trainingSet);
    }
}