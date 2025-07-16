// src/main/java/com/AiPortal/dto/AnalysisModelDto.java
package com.AiPortal.dto;

public class AnalysisModelDto {

    private String modelName;
    private String marketType;
    private double accuracy;
    private double logLoss;
    private String classificationReport;
    private String featureImportances;

    // Getters and Setters
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
}