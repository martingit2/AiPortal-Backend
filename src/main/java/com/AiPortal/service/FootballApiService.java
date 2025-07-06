package com.AiPortal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class FootballApiService {

    private static final Logger log = LoggerFactory.getLogger(FootballApiService.class);
    private final WebClient webClient;

    public FootballApiService(@Value("${rapidapi.key}") String apiKey,
                              @Value("${rapidapi.host.football}") String apiHost) {
        this.webClient = WebClient.builder()
                .baseUrl("https://" + apiHost + "/v3")
                .defaultHeader("x-rapidapi-key", apiKey)
                .defaultHeader("x-rapidapi-host", apiHost)
                .build();
    }

    public Mono<String> getTeamStatistics(String leagueId, String season, String teamId) {
        // Logg informasjonen vi skal sende for å være sikker
        log.info("Kaller API-Football med league={}, season={}, team={}", leagueId, season, teamId);

        return this.webClient.get()
                // ENDRING HER: Bygger URI-en litt annerledes for å være mer eksplisitt
                .uri("/teams/statistics?league={leagueId}&season={season}&team={teamId}",
                        leagueId, season, teamId)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> log.error("WebClient feil: {}", error.getMessage())) // Legg til feillogging
                .doOnSuccess(response -> log.info("WebClient suksess: Mottok svar.")); // Legg til suksesslogging
    }
}