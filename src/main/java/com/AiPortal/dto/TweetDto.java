package com.AiPortal.dto;

import java.time.Instant;

public class TweetDto {
    private Long id; // ID fra vår database, nyttig for key prop i React
    private String authorUsername;
    private String content;
    private Instant tweetedAt;
    private String sourceBotName; // Navnet på boten som hentet den

    public TweetDto(Long id, String authorUsername, String content, Instant tweetedAt, String sourceBotName) {
        this.id = id;
        this.authorUsername = authorUsername;
        this.content = content;
        this.tweetedAt = tweetedAt;
        this.sourceBotName = sourceBotName;
    }

    // Getters
    public Long getId() { return id; }
    public String getAuthorUsername() { return authorUsername; }
    public String getContent() { return content; }
    public Instant getTweetedAt() { return tweetedAt; }
    public String getSourceBotName() { return sourceBotName; }
}