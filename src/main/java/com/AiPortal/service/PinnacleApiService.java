// src/main/java/com/AiPortal/service/PinnacleApiService.java
package com.AiPortal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class PinnacleApiService {

    private final WebClient webClient;
    private static final Duration API_TIMEOUT = Duration.ofSeconds(90);

    public PinnacleApiService(
            @Value("${rapidapi.key.pinnacle}") String pinnacleApiKey
    ) {
        final int bufferSize = 16 * 1024 * 1024;
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(bufferSize))
                .build();

        this.webClient = WebClient.builder()
                .exchangeStrategies(strategies)
                .baseUrl("https://pinnacle-odds.p.rapidapi.com/kit/v1")
                .defaultHeader("x-rapidapi-key", pinnacleApiKey)
                .defaultHeader("x-rapidapi-host", "pinnacle-odds.p.rapidapi.com")
                .build();
    }

    public Mono<ResponseEntity<String>> getMarkets(String sportId, Long sinceTimestamp) {
        return this.webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/markets")
                            .queryParam("sport_id", sportId)
                            .queryParam("event_type", "prematch")
                            .queryParam("is_have_odds", "true");
                    if (sinceTimestamp != null && sinceTimestamp > 0) {
                        uriBuilder.queryParam("since", String.valueOf(sinceTimestamp));
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .toEntity(String.class)
                .timeout(API_TIMEOUT);
    }

    /**
     * NY METODE: Henter spesialmarkeder (f.eks. BTTS) fra Pinnacle.
     */
    public Mono<ResponseEntity<String>> getSpecialMarkets(String sportId, Long sinceTimestamp) {
        return this.webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/special-markets")
                            .queryParam("sport_id", sportId)
                            .queryParam("event_type", "prematch")
                            .queryParam("is_have_odds", "true");
                    if (sinceTimestamp != null && sinceTimestamp > 0) {
                        uriBuilder.queryParam("since", String.valueOf(sinceTimestamp));
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .toEntity(String.class)
                .timeout(API_TIMEOUT);
    }
}