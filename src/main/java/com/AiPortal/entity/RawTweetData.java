package com.AiPortal.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "raw_tweets")
public class RawTweetData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false) // Sørger for at vi ikke lagrer samme tweet flere ganger
    private String tweetId;

    @Column(nullable = false)
    private String authorUsername;

    @Column(length = 1024, nullable = false) // Økt lengde for å håndtere lange tweets
    private String content;

    @Column(nullable = false)
    private Instant tweetedAt;

    private Instant createdAt = Instant.now(); // Tidspunkt for når vi lagret den

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTweetId() { return tweetId; }
    public void setTweetId(String tweetId) { this.tweetId = tweetId; }
    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getTweetedAt() { return tweetedAt; }
    public void setTweetedAt(Instant tweetedAt) { this.tweetedAt = tweetedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}