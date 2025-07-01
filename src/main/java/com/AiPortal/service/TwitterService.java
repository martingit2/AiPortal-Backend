package com.AiPortal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class TwitterService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // Bearer token hentes fra application.properties
    public TwitterService(ObjectMapper objectMapper, @Value("${twitter.api.bearer-token}") String bearerToken) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.twitter.com/2")
                .defaultHeader("Authorization", "Bearer " + bearerToken)
                .build();
    }

    /**
     * Henter nye tweets basert på en konstruert spørring og den sist sette tweet-IDen.
     * @param query Spørringen, f.eks. "(from:user1 OR from:user2) -is:retweet"
     * @param sinceId ID-en til den siste tweeten som ble hentet, for å unngå duplikater. Kan være null.
     * @return Et Mono som inneholder JSON-responsen fra Twitter som en streng.
     */
    public Mono<String> searchRecentTweets(String query, String sinceId) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/tweets/search/recent")
                            .queryParam("query", query)
                            .queryParam("tweet.fields", "created_at,author_id") // Hent med forfatter-ID og tidspunkt
                            .queryParam("expansions", "author_id") // Viktig for å få brukernavn
                            .queryParam("max_results", "100"); // Hent maks antall per kall

                    if (sinceId != null && !sinceId.isEmpty()) {
                        uriBuilder.queryParam("since_id", sinceId);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * Hjelpemetode for å parse JSON-svaret fra Twitter.
     * @param responseBody JSON-strengen fra API-et.
     * @return En liste av JsonNode-objekter, hvor hver representerer en tweet.
     */
    public List<JsonNode> parseTweetsFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.path("data");
            List<JsonNode> tweets = new ArrayList<>();
            if (data != null && data.isArray()) {
                data.forEach(tweets::add);
            }
            return tweets;
        } catch (Exception e) {
            throw new RuntimeException("Kunne ikke parse tweets fra Twitter API", e);
        }
    }

    /**
     * Hjelpemetode for å parse den nyeste tweet-IDen fra meta-dataen i svaret.
     * @param responseBody JSON-strengen fra API-et.
     * @return Den nyeste tweet-IDen, eller null hvis ikke funnet.
     */
    public String parseNewestTweetId(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("meta").path("newest_id").asText(null);
        } catch (Exception e) {
            // Ignorer feil her, vi returnerer bare null
            return null;
        }
    }

    /**
     * Hjelpemetode for å finne brukernavnet til en forfatter-ID fra "includes"-delen av svaret.
     * @param responseBody JSON-strengen fra API-et.
     * @param authorId ID-en til forfatteren.
     * @return Brukernavnet, eller "ukjent" hvis ikke funnet.
     */
    public String findUsernameFromIncludes(String responseBody, String authorId) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode users = root.path("includes").path("users");
            if (users.isArray()) {
                for (JsonNode user : users) {
                    if (user.path("id").asText().equals(authorId)) {
                        return user.path("username").asText();
                    }
                }
            }
            return "ukjent";
        } catch (Exception e) {
            return "ukjent";
        }
    }
}