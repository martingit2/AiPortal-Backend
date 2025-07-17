// src/main/java/com/AiPortal/service/PredictionService.java (REFAKTORERT)
package com.AiPortal.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
public class PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);
    private final WebClient webClient;

    public PredictionService() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:5001") // Python service
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * NY GENERELL METODE: Kaller det nye dynamiske /predict endepunktet.
     * @param modelName Filnavnet på modellen som skal brukes (f.eks. "football_predictor_v5_h2h.joblib").
     * @param features Et map med feature-navn og verdier.
     * @return En Optional som inneholder JSON-responsen fra ML-tjenesten.
     */
    public Optional<JsonNode> getPredictions(String modelName, Map<String, Object> features) {
        try {
            // Bygg den nye request-bodyen
            Map<String, Object> requestBody = Map.of(
                    "modelName", modelName,
                    "features", features
            );

            JsonNode jsonResponse = webClient.post()
                    .uri("/predict") // Kaller det nye, generelle endepunktet
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            return Optional.ofNullable(jsonResponse);

        } catch (Exception e) {
            log.error("Kunne ikke hente prediksjon for modell '{}'. Feil: {}", modelName, e.getMessage());
            return Optional.empty();
        }
    }
}