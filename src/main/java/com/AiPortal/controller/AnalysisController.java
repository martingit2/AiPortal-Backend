package com.AiPortal.controller;

import com.AiPortal.entity.Analysis;
import com.AiPortal.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analyses")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AnalysisController {

    private final AnalysisService analysisService;

    @Autowired
    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * Henter alle analyser for den innloggede brukeren.
     */
    @GetMapping
    public ResponseEntity<List<Analysis>> getAnalysesForUser(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        List<Analysis> analyses = analysisService.getAnalysesForUser(userId);
        return ResponseEntity.ok(analyses);
    }

    /**
     * Starter en ny sentimentanalyse på en gitt tweet.
     * Body forventes å være på formatet: { "tweetId": 123 }
     */
    @PostMapping("/sentiment")
    public ResponseEntity<Analysis> startSentimentAnalysis(@RequestBody Map<String, Long> payload, @AuthenticationPrincipal Jwt jwt) {
        Long tweetId = payload.get("tweetId");
        if (tweetId == null) {
            return ResponseEntity.badRequest().build();
        }
        String userId = jwt.getSubject();

        try {
            Analysis createdAnalysis = analysisService.startSentimentAnalysis(tweetId, userId);
            // 202 Accepted er en god statuskode for å indikere at en bakgrunnsjobb er startet.
            return new ResponseEntity<>(createdAnalysis, HttpStatus.ACCEPTED);
        } catch (IllegalArgumentException e) {
            // Hvis tweetId ikke ble funnet i databasen
            return ResponseEntity.notFound().build();
        }
    }
}