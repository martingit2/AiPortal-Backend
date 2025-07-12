// src/main/java/com/AiPortal/service/twitter/OfficialTwitterService.java
package com.AiPortal.service.twitter; // <--- OPPDATERT PAKKENAVN

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service // Spring vil nå kjenne igjen denne som en gyldig "TwitterServiceProvider"-bønne
public class OfficialTwitterService implements TwitterServiceProvider {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // Bearer token hentes fra application.properties
    public OfficialTwitterService(ObjectMapper objectMapper, @Value("${twitter.api.bearer-token}") String bearerToken) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.twitter.com/2")
                .defaultHeader("Authorization", "Bearer " + bearerToken)
                .build();
    }

    @Override
    public String getProviderName() {
        return "OfficialTwitterAPI";
    }

    @Override
    public Mono<String> searchRecentTweets(String query, String sinceId) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/tweets/search/recent")
                            .queryParam("query", query)
                            .queryParam("tweet.fields", "created_at,author_id")
                            .queryParam("expansions", "author_id")
                            .queryParam("max_results", "100");

                    if (sinceId != null && !sinceId.isEmpty()) {
                        uriBuilder.queryParam("since_id", sinceId);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(String.class);
    }

    @Override
    public List<JsonNode> parseTweetsFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.path("data");
            List<JsonNode> tweets = new ArrayList<>();
            if (data.isArray()) {
                data.forEach(tweets::add);
            }
            return tweets;
        } catch (Exception e) {
            // I en produksjonsapp ville vi hatt bedre logging her
            throw new RuntimeException("Kunne ikke parse tweets fra Official Twitter API", e);
        }
    }

    @Override
    public String parseNewestTweetId(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("meta").path("newest_id").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
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