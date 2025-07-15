// src/main/java/com/AiPortal/service/PredictionService.java
package com.AiPortal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@Service
public class PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public PredictionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:5001")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public static class MLProbabilities {
        public final double homeWin;
        public final double draw;
        public final double awayWin;

        public MLProbabilities(double homeWin, double draw, double awayWin) {
            this.homeWin = homeWin;
            this.draw = draw;
            this.awayWin = awayWin;
        }
    }

    // --- NY DTO for Over/Under-sannsynligheter ---
    public static class OverUnderProbabilities {
        public final double under;
        public final double over;

        public OverUnderProbabilities(double under, double over) {
            this.under = under;
            this.over = over;
        }
    }
    // ---------------------------------------------


    public Optional<MLProbabilities> getMatchOutcomeProbabilities(Map<String, Object> features) {
        try {
            String jsonResponse = webClient.post()
                    .uri("/predict/match_outcome")
                    .bodyValue(features)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (jsonResponse == null) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode probsNode = root.path("probabilities");

            double homeWin = probsNode.path("home_win").asDouble();
            double draw = probsNode.path("draw").asDouble();
            double awayWin = probsNode.path("away_win").asDouble();

            return Optional.of(new MLProbabilities(homeWin, draw, awayWin));

        } catch (Exception e) {
            log.error("Kunne ikke hente kampvinner-prediksjon. Feil: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // --- NY METODE for å hente Over/Under-prediksjoner ---
    public Optional<OverUnderProbabilities> getOverUnderProbabilities(Map<String, Object> features) {
        try {
            String jsonResponse = webClient.post()
                    .uri("/predict/over_under") // Kaller det nye endepunktet
                    .bodyValue(features)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (jsonResponse == null) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode probsNode = root.path("probabilities");

            double under = probsNode.path("under_2_5").asDouble();
            double over = probsNode.path("over_2_5").asDouble();

            return Optional.of(new OverUnderProbabilities(under, over));

        } catch (Exception e) {
            log.error("Kunne ikke hente Over/Under-prediksjon. Feil: {}", e.getMessage());
            return Optional.empty();
        }
    }
    // ----------------------------------------------------
}