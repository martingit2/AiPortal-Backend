// src/main/java/com/AiPortal/dto/ParsedTweetResponse.java
package com.AiPortal.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public class ParsedTweetResponse {
    private final List<JsonNode> tweets;
    private final String newestId;
    private final String responseBody; // Vi tar med hele bodyen for å kunne hente brukernavn

    public ParsedTweetResponse(List<JsonNode> tweets, String newestId, String responseBody) {
        this.tweets = tweets;
        this.newestId = newestId;
        this.responseBody = responseBody;
    }

    // Getters
    public List<JsonNode> getTweets() {
        return tweets;
    }

    public String getNewestId() {
        return newestId;
    }

    public String getResponseBody() {
        return responseBody;
    }
}