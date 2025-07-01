package com.AiPortal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "twitter_query_states")
public class TwitterQueryState {

    // Vi bruker en fast ID, siden vi bare har én "recent search"-spørring for alle boter.
    @Id
    private String queryName = "recent_search_all_bots";

    @Column(nullable = false)
    private String lastSeenTweetId;

    // Getters and Setters
    public String getQueryName() {
        return queryName;
    }

    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }

    public String getLastSeenTweetId() {
        return lastSeenTweetId;
    }

    public void setLastSeenTweetId(String lastSeenTweetId) {
        this.lastSeenTweetId = lastSeenTweetId;
    }
}