// src/main/java/com/AiPortal/service/AnalysisService.java
package com.AiPortal.service;

import com.AiPortal.entity.Analysis;
import com.AiPortal.entity.RawTweetData;
import com.AiPortal.repository.AnalysisRepository;
import com.AiPortal.repository.RawTweetDataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final AnalysisRepository analysisRepository;
    private final RawTweetDataRepository tweetRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public AnalysisService(
            AnalysisRepository analysisRepository,
            RawTweetDataRepository tweetRepository,
            ObjectMapper objectMapper) {
        this.analysisRepository = analysisRepository;
        this.tweetRepository = tweetRepository;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:5001")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Transactional(readOnly = true)
    public List<Analysis> getAnalysesForUser(String userId) {
        return analysisRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Analysis startSentimentAnalysis(Long tweetId, String userId) {
        RawTweetData tweet = tweetRepository.findById(tweetId)
                .orElseThrow(() -> new IllegalArgumentException("Tweet ikke funnet med ID: " + tweetId));

        // Sjekk om en analyse allerede er i gang for denne tweeten for å unngå duplikater
        // Dette er en forenklet sjekk. I et større system kan man ha en relasjon.
        String analysisName = "Innsiktsanalyse for tweet fra @" + tweet.getAuthorUsername();
        if (analysisRepository.findByName(analysisName).stream().anyMatch(a -> a.getStatus() != Analysis.AnalysisStatus.FAILED)) {
            log.warn("Analyse for tweet {} er allerede startet eller fullført. Starter ikke ny jobb.", tweet.getId());
            // Returner den eksisterende analysen eller en feilmelding
            return analysisRepository.findByName(analysisName).get(0);
        }

        Analysis analysis = new Analysis();
        analysis.setName(analysisName);
        analysis.setUserId(userId);
        analysis.setStatus(Analysis.AnalysisStatus.QUEUED);

        Analysis savedAnalysis = analysisRepository.save(analysis);

        processInsightExtraction(savedAnalysis.getId(), tweet.getContent());

        return savedAnalysis;
    }

    @Transactional(readOnly = true)
    public double getAggregatedSentimentForMarket(String keyword, String marketType, int hoursBack) {
        log.info("Beregner aggregert sentiment for nøkkelord: '{}' og marked: '{}'", keyword, marketType);

        Instant afterDate = Instant.now().minus(hoursBack, ChronoUnit.HOURS);
        List<RawTweetData> relevantTweets = tweetRepository.findByContentContainingIgnoreCaseAndTweetedAtAfter(keyword, afterDate);

        if (relevantTweets.isEmpty()) {
            return 0.0;
        }

        double totalScore = 0;
        int analyzedCount = 0;

        List<Analysis> allAnalyses = analysisRepository.findAll();

        for (RawTweetData tweet : relevantTweets) {
            String analysisName = "Innsiktsanalyse for tweet fra @" + tweet.getAuthorUsername();

            Optional<Analysis> existingAnalysis = allAnalyses.stream()
                    .filter(a -> a.getName().equals(analysisName) && a.getStatus() == Analysis.AnalysisStatus.COMPLETED)
                    .findFirst();

            if (existingAnalysis.isPresent()) {
                Analysis analysis = existingAnalysis.get();
                try {
                    JsonNode root = objectMapper.readTree(analysis.getResult());
                    JsonNode entities = root.path("entities_found");

                    boolean marketMentioned = StreamSupport.stream(entities.spliterator(), false)
                            .anyMatch(entity -> entity.path("entity").asText().equals(marketType));

                    if (marketMentioned) {
                        totalScore += root.path("general_sentiment").path("score").asDouble();
                        analyzedCount++;
                    }
                } catch (JsonProcessingException e) {
                    log.error("Kunne ikke parse analyseresultat for analyse-ID {}", analysis.getId(), e);
                }
            } else {
                boolean analysisInProgress = allAnalyses.stream()
                        .anyMatch(a -> a.getName().equals(analysisName) && (
                                a.getStatus() == Analysis.AnalysisStatus.QUEUED ||
                                        a.getStatus() == Analysis.AnalysisStatus.RUNNING
                        ));
                if (!analysisInProgress) {
                    log.info("Ingen analyse funnet for tweet {}. Starter ny innsiktsanalyse.", tweet.getId());
                    startSentimentAnalysis(tweet.getId(), "system");
                }
            }
        }

        if (analyzedCount == 0) {
            log.info("Ingen fullførte analyser funnet som nevner marked '{}' for nøkkelord '{}'.", marketType, keyword);
            return 0.0;
        }

        double averageScore = totalScore / analyzedCount;
        log.info("Aggregert sentiment for marked '{}' / nøkkelord '{}' er {:.4f} basert på {} tweets.", marketType, keyword, averageScore, analyzedCount);
        return averageScore;
    }


    @Async("taskExecutor")
    @Transactional
    public void processInsightExtraction(Long analysisId, String textToAnalyze) {
        log.info("Starter innsikts-ekstraksjon for analyse-ID: {}", analysisId);

        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new IllegalStateException("Kan ikke finne analyse med ID: " + analysisId));

        analysis.setStatus(Analysis.AnalysisStatus.RUNNING);
        analysisRepository.saveAndFlush(analysis);

        try {
            Map<String, String> requestBody = Map.of("text", textToAnalyze);

            String insightResultJson = webClient.post()
                    .uri("/extract-insights")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            analysis.setResult(insightResultJson);
            analysis.setStatus(Analysis.AnalysisStatus.COMPLETED);

        } catch (Exception e) {
            log.error("Innsiktsanalyse feilet for ID: {}", analysisId, e);
            analysis.setStatus(Analysis.AnalysisStatus.FAILED);
            analysis.setResult("Feil under kall til AI-tjeneste: " + e.getMessage());
        }

        analysis.setCompletedAt(Instant.now());
        analysisRepository.save(analysis);
        log.info("Innsikts-ekstraksjon ferdig for analyse-ID: {}", analysisId);
    }
}