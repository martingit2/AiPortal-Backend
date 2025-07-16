// src/main/java/com/AiPortal/entity/AnalysisModel.java
package com.AiPortal.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "analysis_models")
public class AnalysisModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String modelName;

    @Column(nullable = false)
    private String marketType;

    private double accuracy;

    private double logLoss;

    @Column(columnDefinition = "TEXT")
    private String classificationReport;

    @Column(columnDefinition = "TEXT")
    private String featureImportances; // Lagres som JSON-streng

    private Instant trainingTimestamp;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getMarketType() { return marketType; }
    public void setMarketType(String marketType) { this.marketType = marketType; }
    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
    public double getLogLoss() { return logLoss; }
    public void setLogLoss(double logLoss) { this.logLoss = logLoss; }
    public String getClassificationReport() { return classificationReport; }
    public void setClassificationReport(String classificationReport) { this.classificationReport = classificationReport; }
    public String getFeatureImportances() { return featureImportances; }
    public void setFeatureImportances(String featureImportances) { this.featureImportances = featureImportances; }
    public Instant getTrainingTimestamp() { return trainingTimestamp; }
    public void setTrainingTimestamp(Instant trainingTimestamp) { this.trainingTimestamp = trainingTimestamp; }
}