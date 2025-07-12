// src/main/java/com/AiPortal/service/twitter/TwttrApi241Service.java
package com.AiPortal.service.twitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class TwttrApi241Service implements TwitterServiceProvider {

    private static final Logger log = LoggerFactory.getLogger(TwttrApi241Service.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public TwttrApi241Service(
            ObjectMapper objectMapper,
            @Value("${rapidapi.twitter241.key}") String apiKey,
            @Value("${rapidapi.twitter241.host}") String apiHost
    ) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl("https://" + apiHost)
                .defaultHeader("x-rapidapi-key", apiKey)
                .defaultHeader("x-rapidapi-host", apiHost)
                .build();
    }

    @Override
    public String getProviderName() {
        return "TwttrAPI241";
    }

    @Override
    public Mono<String> searchRecentTweets(String query, String sinceId) {
        // Vi bruker /search-endepunktet som vi vet fungerer.
        // Den forventer en 'query' som "from:username", noe som passer perfekt.
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("query", query)
                        .queryParam("count", 20)
                        .queryParam("type", "Top") // Eller "Latest"
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }

    @Override
    public List<JsonNode> parseTweetsFromResponse(String responseBody) {
        // NÅ: Korrekt parsing for den komplekse strukturen.
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            List<JsonNode> tweets = new ArrayList<>();

            // Navigerer ned til 'entries'-listen
            JsonNode entries = root.path("result").path("timeline").path("instructions").get(0).path("entries");

            if (entries.isArray()) {
                for (JsonNode entry : entries) {
                    // Vi leter etter 'content' som inneholder et tweet.
                    // Vi antar at tweet-innhold er under "itemContent" og har "tweet_results".
                    JsonNode tweetResults = entry.path("content").path("itemContent").path("tweet_results").path("result");

                    // Sjekk om 'tweetResults' finnes og ikke er et tomt objekt.
                    if (tweetResults != null && !tweetResults.isMissingNode() && tweetResults.has("rest_id")) {
                        // Vi har funnet et tweet-objekt. Legg det til i listen.
                        // Vi må justere objektet slik at det ligner på de andre API-ene.
                        // For nå legger vi bare til det vi fant.
                        tweets.add(tweetResults);
                    }
                }
            }
            return tweets;
        } catch (Exception e) {
            log.error("Kunne ikke parse JSON fra {}: {}", getProviderName(), responseBody, e);
            throw new RuntimeException("Kunne ikke parse tweets fra " + getProviderName(), e);
        }
    }

    @Override
    public String parseNewestTweetId(String responseBody) {
        try {
            List<JsonNode> tweets = parseTweetsFromResponse(responseBody);
            if (!tweets.isEmpty()) {
                // Feltet heter 'rest_id' i denne responsen.
                return tweets.get(0).path("rest_id").asText(null);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String findUsernameFromIncludes(String responseBody, String authorId) {
        // Ikke relevant.
        return "N/A";
    }
}