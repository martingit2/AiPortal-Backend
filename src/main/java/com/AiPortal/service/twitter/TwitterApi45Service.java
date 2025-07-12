// src/main/java/com/AiPortal/service/twitter/TwitterApi45Service.java
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
public class TwitterApi45Service implements TwitterServiceProvider {

    private static final Logger log = LoggerFactory.getLogger(TwitterApi45Service.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public TwitterApi45Service(
            ObjectMapper objectMapper,
            @Value("${rapidapi.twitter45.key}") String apiKey,
            @Value("${rapidapi.twitter45.host}") String apiHost
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
        return "TwitterAPI45";
    }

    @Override
    public Mono<String> searchRecentTweets(String query, String sinceId) {
        // Denne API-en bruker /search.php og ignorerer sinceId.
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search.php") // Korrekt endepunkt
                        .queryParam("query", query)
                        .queryParam("search_type", "Top") // Eller "Latest"
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }

    @Override
    public List<JsonNode> parseTweetsFromResponse(String responseBody) {
        // VIKTIG: Strukturen på JSON-svaret fra /search.php må inspiseres.
        // Dette er et KVALIFISERT GJETT.
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            // Kanskje responsen er pakket inn i et "timeline" eller "tweets" objekt
            JsonNode data = root.path("timeline");
            List<JsonNode> tweets = new ArrayList<>();
            if (data.isArray()) {
                data.forEach(tweets::add);
            } else {
                log.warn("TwitterAPI45 respons var ikke en liste som forventet under 'timeline'. Body: {}", responseBody);
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
                // Hva heter ID-feltet? "tweet_id"? "id"?
                return tweets.get(0).path("tweet_id").asText(null);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String findUsernameFromIncludes(String responseBody, String authorId) {
        // Ikke relevant for denne API-en. Overlates til ScheduledBotRunner.
        return "N/A";
    }
}