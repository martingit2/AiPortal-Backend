// src/main/java/com/AiPortal/service/ModelManagementService.java
package com.AiPortal.service;

import com.AiPortal.dto.AnalysisModelDto;
import com.AiPortal.entity.AnalysisModel;
import com.AiPortal.repository.AnalysisModelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class ModelManagementService {

    private final AnalysisModelRepository modelRepository;

    public ModelManagementService(AnalysisModelRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    public List<AnalysisModel> getAllModels() {
        return modelRepository.findAllByOrderByTrainingTimestampDesc();
    }

    public AnalysisModel registerModel(AnalysisModelDto dto) {
        AnalysisModel model = new AnalysisModel();
        model.setModelName(dto.getModelName());
        model.setMarketType(dto.getMarketType());
        model.setAccuracy(dto.getAccuracy());
        model.setLogLoss(dto.getLogLoss());
        model.setClassificationReport(dto.getClassificationReport());
        model.setFeatureImportances(dto.getFeatureImportances());
        model.setTrainingTimestamp(Instant.now());

        return modelRepository.save(model);
    }

    public void deleteModel(Long id) {
        if (!modelRepository.existsById(id)) {
            // Kast en exception eller bare returner, avhengig av ønsket oppførsel
            // For enkelhetens skyld, gjør vi ingenting hvis den ikke finnes.
            return;
        }
        modelRepository.deleteById(id);
    }
}