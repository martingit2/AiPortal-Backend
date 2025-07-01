package com.AiPortal.controller;

import com.AiPortal.entity.RawTweetData;
import com.AiPortal.service.TweetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tweets") // Base-sti for alle tweet-relaterte endepunkter
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class TweetController {

    private final TweetService tweetService;

    @Autowired
    public TweetController(TweetService tweetService) {
        this.tweetService = tweetService;
    }

    /**
     * Henter en paginert liste av de sist lagrede tweets.
     * Dette endepunktet vil være beskyttet av SecurityConfig.
     * @param page Sidenummer (standard 0).
     * @param size Antall per side (standard 20).
     * @return En paginert respons med tweet-data.
     */
    @GetMapping
    public ResponseEntity<Page<RawTweetData>> getLatestTweets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<RawTweetData> tweetPage = tweetService.getLatestTweets(page, size);
        return ResponseEntity.ok(tweetPage);
    }
}