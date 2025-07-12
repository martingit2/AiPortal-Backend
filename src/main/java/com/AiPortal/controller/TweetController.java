// src/main/java/com/AiPortal/controller/TweetController.java
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
    public ResponseEntity<Page<TweetDto>> getLatestTweets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<TweetDto> tweetPage = tweetService.getLatestTweets(page, size);
        return ResponseEntity.ok(tweetPage);
    }

    /**
     * NYTT ENDEPUNKT: Sletter en spesifikk tweet basert på dens database-ID.
     * @param id ID-en til tweeten som skal slettes, fra URL-stien.
     * @return 204 No Content hvis slettingen var vellykket, 404 Not Found hvis ikke.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTweet(@PathVariable Long id) {
        boolean deleted = tweetService.deleteTweetById(id);
        if (deleted) {
            return ResponseEntity.noContent().build(); // Standard og korrekt respons for vellykket sletting
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}