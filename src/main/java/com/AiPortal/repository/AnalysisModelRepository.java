// src/main/java/com/AiPortal/repository/AnalysisModelRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.AnalysisModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisModelRepository extends JpaRepository<AnalysisModel, Long> {

    // Sorterer modellene med den nyeste først som standard
    List<AnalysisModel> findAllByOrderByTrainingTimestampDesc();
}