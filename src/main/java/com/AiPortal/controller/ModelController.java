// src/main/java/com/AiPortal/controller/ModelController.java
package com.AiPortal.controller;

import com.AiPortal.dto.AnalysisModelDto;
import com.AiPortal.entity.AnalysisModel;
import com.AiPortal.service.ModelManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/models")
public class ModelController {

    private final ModelManagementService modelService;

    public ModelController(ModelManagementService modelService) {
        this.modelService = modelService;
    }

    // Endepunkt for frontend for å hente alle modeller
    @GetMapping
    public ResponseEntity<List<AnalysisModel>> getAllModels() {
        List<AnalysisModel> models = modelService.getAllModels();
        return ResponseEntity.ok(models);
    }

    // Endepunkt for Python-skriptet for å registrere en ny-trent modell
    // Dette endepunktet bør sikres i fremtiden, f.eks. med en API-nøkkel
    @PostMapping("/register")
    public ResponseEntity<AnalysisModel> registerModel(@RequestBody AnalysisModelDto modelDto) {
        AnalysisModel registeredModel = modelService.registerModel(modelDto);
        return new ResponseEntity<>(registeredModel, HttpStatus.CREATED);
    }

    // Endepunkt for frontend for å slette en modell
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteModel(@PathVariable Long id) {
        modelService.deleteModel(id);
        return ResponseEntity.noContent().build();
    }
}