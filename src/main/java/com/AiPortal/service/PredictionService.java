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

/**
 * En dedikert tjeneste for å kommunisere med Python-baserte prediksjonsmodeller.
 */
@Service
public class PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public PredictionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:5001") // Pek mot Python-appen
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * DTO for å representere sannsynlighetene returnert fra ML-modellen.
     */
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

    /**
     * Kaller prediksjons-endepunktet for å få sannsynligheter for kampresultat.
     * @param features Et kart med alle features for kampen.
     * @return En Optional som inneholder MLProbabilities hvis kallet var vellykket.
     */
    public Optional<MLProbabilities> getMatchOutcomeProbabilities(Map<String, Object> features) {
        try {
            String jsonResponse = webClient.post()
                    .uri("/predict/match_outcome")
                    .bodyValue(features)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // block() er ok her, da dette vil bli kalt innenfor en større, asynkron jobb.

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
            log.error("Kunne ikke hente ML-prediksjon. Feil: {}", e.getMessage());
            return Optional.empty();
        }
    }
}