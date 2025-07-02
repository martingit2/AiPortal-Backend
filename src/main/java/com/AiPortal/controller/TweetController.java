package com.AiPortal.controller;

import com.AiPortal.dto.TweetDto;
import com.AiPortal.service.TweetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tweets")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class TweetController {

    private final TweetService tweetService;

    @Autowired
    public TweetController(TweetService tweetService) {
        this.tweetService = tweetService;
    }

    @GetMapping
    public ResponseEntity<Page<TweetDto>> getLatestTweets( // Endret returtype til Page<TweetDto>
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size
    ) {
        Page<TweetDto> tweetPage = tweetService.getLatestTweets(page, size);
        return ResponseEntity.ok(tweetPage);
    }
}