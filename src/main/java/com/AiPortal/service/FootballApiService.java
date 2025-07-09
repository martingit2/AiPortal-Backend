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
    private static final Duration API_TIMEOUT = Duration.ofSeconds(30);

    public FootballApiService(@Value("${rapidapi.key}") String apiKey,
                              @Value("${rapidapi.host.football}") String apiHost) {

        final int bufferSize = 16 * 1024 * 1024;
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

    public Mono<ResponseEntity<String>> getFixtureById(long fixtureId) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures")
                        .queryParam("id", String.valueOf(fixtureId))
                        .build())
                .retrieve()
                .toEntity(String.class)
                .timeout(API_TIMEOUT);
    }

    public Mono<ResponseEntity<String>> getTeamsInLeague(String leagueId, String season) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("league", leagueId)
                        .queryParam("season", season)
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

    public Mono<ResponseEntity<String>> getStatisticsForFixture(Long fixtureId) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures/statistics")
                        .queryParam("fixture", String.valueOf(fixtureId))
                        .build())
                .retrieve()
                .toEntity(String.class)
                .timeout(API_TIMEOUT);
    }

    /**
     * NY METODE: Henter basisinformasjon for ett enkelt lag, basert på ID.
     * Brukes for å slå opp lagnavn på en robust måte.
     * @param teamId ID-en til laget.
     * @return Et Mono med JSON-svaret.
     */
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
}