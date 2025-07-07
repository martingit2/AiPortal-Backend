package com.AiPortal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class FootballApiService {

    private final WebClient webClient;

    public FootballApiService(@Value("${rapidapi.key}") String apiKey,
                              @Value("${rapidapi.host.football}") String apiHost) {
        this.webClient = WebClient.builder()
                .baseUrl("https://" + apiHost + "/v3")
                .defaultHeader("x-rapidapi-key", apiKey)
                .defaultHeader("x-rapidapi-host", apiHost)
                .build();
    }

    /**
     * Henter team-statistikk for en gitt liga, sesong og lag.
     */
    public Mono<String> getTeamStatistics(String leagueId, String season, String teamId) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams/statistics")
                        .queryParam("league", leagueId)
                        .queryParam("season", season)
                        .queryParam("team", teamId)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * Henter odds for alle kamper på en gitt dato.
     */
    public Mono<String> getOddsByDate(String date) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/odds")
                        .queryParam("date", date)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * Henter listen over alle tilgjengelige bookmakere.
     */
    public Mono<String> getBookmakers() {
        return this.webClient.get()
                .uri("/odds/bookmakers")
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * Henter listen over alle tilgjengelige spilltyper (bets).
     */
    public Mono<String> getBetTypes() {
        return this.webClient.get()
                .uri("/odds/bets")
                .retrieve()
                .bodyToMono(String.class);
    }
}