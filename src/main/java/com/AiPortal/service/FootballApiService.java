// src/main/java/com/AiPortal/service/FootballApiService.java
package com.AiPortal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class FootballApiService {

    private final WebClient webClient;
    private static final Duration API_TIMEOUT = Duration.ofSeconds(60); // Økt timeout for potensielt store responser

    public FootballApiService(@Value("${rapidapi.key}") String apiKey,
                              @Value("${rapidapi.host.football}") String apiHost) {

        final int bufferSize = 16 * 1024 * 1024; // 16MB
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs
                        .defaultCodecs()
                        .maxInMemorySize(bufferSize))
                .build();

        this.webClient = WebClient.builder()
                .exchangeStrategies(strategies)
                .baseUrl("https://" + apiHost + "/v3")
                .defaultHeader("x-rapidapi-key", apiKey)
                .defaultHeader("x-rapidapi-host", apiHost)
                .build();
    }

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

    public Mono<ResponseEntity<String>> getFixturesByIds(String fixtureIds) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures")
                        .queryParam("ids", fixtureIds)
                        .build())
                .retrieve()
                .toEntity(String.class)
                .timeout(API_TIMEOUT);
    }

    public Mono<ResponseEntity<String>> getFixturesByLeagueAndSeason(String leagueId, String season) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures")
                        .queryParam("league", leagueId)
                        .queryParam("season", season)
                        .build())
                .retrieve()
                .toEntity(String.class)
                .timeout(API_TIMEOUT);
    }

    public Mono<ResponseEntity<String>> getTeamById(Integer teamId) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("id", String.valueOf(teamId))
                        .build())
                .retrieve()
                .toEntity(String.class)
                .timeout(API_TIMEOUT);
    }

    public Mono<ResponseEntity<String>> getInjuriesByIds(String fixtureIds) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/injuries")
                        .queryParam("ids", fixtureIds)
                        .build())
                .retrieve()
                .toEntity(String.class)
                .timeout(API_TIMEOUT);
    }

    /**
     * NY METODE: Henter tabell (standings) for en hel liga/sesong.
     * @param leagueId ID-en til ligaen.
     * @param season Årstall for sesongen.
     * @return Et Mono med JSON-svaret.
     */
    public Mono<ResponseEntity<String>> getStandings(String leagueId, String season) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/standings")
                        .queryParam("league", leagueId)
                        .queryParam("season", season)
                        .build())
                .retrieve()
                .toEntity(String.class)
                .timeout(API_TIMEOUT);
    }
}