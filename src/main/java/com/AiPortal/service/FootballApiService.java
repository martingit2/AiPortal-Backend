package com.AiPortal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies; // <-- NY IMPORT
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class FootballApiService {

    private final WebClient webClient;
    private static final Duration API_TIMEOUT = Duration.ofSeconds(30);

    public FootballApiService(@Value("${rapidapi.key}") String apiKey,
                              @Value("${rapidapi.host.football}") String apiHost) {

        // Definer en strategi for å øke bufferstørrelsen
        final int bufferSize = 16 * 1024 * 1024; // 16 MB, en veldig generøs grense

        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs
                        .defaultCodecs()
                        .maxInMemorySize(bufferSize))
                .build();

        // Bygg WebClient med den nye strategien
        this.webClient = WebClient.builder()
                .exchangeStrategies(strategies) // <-- LEGG TIL DENNE
                .baseUrl("https://" + apiHost + "/v3")
                .defaultHeader("x-rapidapi-key", apiKey)
                .defaultHeader("x-rapidapi-host", apiHost)
                .build();
    }

    // ... resten av metodene (getTeamStatistics, getOddsByDate, etc.) forblir helt uendret ...
    public Mono<ResponseEntity<String>> getTeamStatistics(String leagueId, String season, String teamId) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams/statistics")
                        .queryParam("league", leagueId)
                        .queryParam("season", season)
                        .queryParam("team", teamId)
                        .build())
                .retrieve()
                .toEntity(String.class)
                .timeout(API_TIMEOUT);
    }

    public Mono<ResponseEntity<String>> getOddsByDate(String date) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/odds")
                        .queryParam("date", date)
                        .build())
                .retrieve()
                .toEntity(String.class)
                .timeout(API_TIMEOUT);
    }

    public Mono<ResponseEntity<String>> getBookmakers() {
        return this.webClient.get()
                .uri("/odds/bookmakers")
                .retrieve()
                .toEntity(String.class)
                .timeout(API_TIMEOUT);
    }

    public Mono<ResponseEntity<String>> getBetTypes() {
        return this.webClient.get()
                .uri("/odds/bets")
                .retrieve()
                .toEntity(String.class)
                .timeout(API_TIMEOUT);
    }
}