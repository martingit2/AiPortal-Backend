package com.AiPortal.service;

import com.AiPortal.entity.Analysis;
import com.AiPortal.entity.RawTweetData;
import com.AiPortal.repository.AnalysisRepository;
import com.AiPortal.repository.RawTweetDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient; // Viktig import
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final AnalysisRepository analysisRepository;
    private final RawTweetDataRepository tweetRepository;
    private final WebClient webClient; // WebClient for å kalle Python-tjenesten

    @Autowired
    public AnalysisService(AnalysisRepository analysisRepository,
                           RawTweetDataRepository tweetRepository) {
        this.analysisRepository = analysisRepository;
        this.tweetRepository = tweetRepository;
        // Initialiserer WebClient til å peke på din Flask API
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:5001") // Porten Flask-appen din kjører på
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Henter alle analyser for en gitt bruker.
     * @param userId Brukerens ID fra Clerk.
     * @return En liste av analyser, sortert etter opprettelsestidspunkt.
     */
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysesForUser(String userId) {
        return analysisRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Starter en analysejobb. Oppretter en analyse-rad i databasen
     * og starter den asynkrone prosesseringen.
     * @param tweetId ID-en til den lagrede tweeten som skal analyseres.
     * @param userId Brukerens ID.
     * @return Den nyopprettede Analysis-entiteten med status QUEUED.
     */
    @Transactional
    public Analysis startSentimentAnalysis(Long tweetId, String userId) {
        RawTweetData tweet = tweetRepository.findById(tweetId)
                .orElseThrow(() -> new IllegalArgumentException("Tweet ikke funnet med ID: " + tweetId));

        Analysis analysis = new Analysis();
        analysis.setName("Sentimentanalyse for tweet fra @" + tweet.getAuthorUsername());
        analysis.setUserId(userId);
        analysis.setStatus(Analysis.AnalysisStatus.QUEUED);

        Analysis savedAnalysis = analysisRepository.save(analysis);

        // Start selve den tidkrevende analysen i en egen tråd
        processAnalysis(savedAnalysis.getId(), tweet.getContent()); // Send med tweet-teksten

        return savedAnalysis;
    }

    /**
     * Asynkron metode som utfører selve analysen ved å kalle Python-tjenesten.
     * @param analysisId ID-en til analysen som skal oppdateres.
     * @param textToAnalyze Teksten som skal analyseres.
     */
    @Async
    @Transactional
    public void processAnalysis(Long analysisId, String textToAnalyze) {
        log.info("Starter prosessering for analyse-ID: {}", analysisId);

        // Hent analysen fra DB for å oppdatere den
        Analysis analysis = analysisRepository.findById(analysisId).orElse(null);
        if (analysis == null) {
            log.error("Kunne ikke finne analyse med ID: {} for prosessering.", analysisId);
            return;
        }

        analysis.setStatus(Analysis.AnalysisStatus.RUNNING);
        analysisRepository.saveAndFlush(analysis); // Lagre statusendringen umiddelbart

        try {
            // Lag request body for Python API-et
            Map<String, String> requestBody = Map.of("text", textToAnalyze);

            // Kall Python/Flask AI-tjenesten og vent på svar
            String sentimentResultJson = webClient.post()
                    .uri("/analyze-sentiment")
                    .bodyValue(requestBody)
                    .retrieve() // Hent responsen
                    .bodyToMono(String.class) // Konverter body til en String
                    .block(); // .block() gjør kallet synkront. Enkelt og greit for en bakgrunnsjobb.

            // Suksess! Oppdater analysen med resultatet.
            analysis.setResult(sentimentResultJson);
            analysis.setStatus(Analysis.AnalysisStatus.COMPLETED);

        } catch (Exception e) {
            log.error("Analyse feilet for ID: {}", analysisId, e);
            analysis.setStatus(Analysis.AnalysisStatus.FAILED);
            analysis.setResult("Feil under kall til AI-tjeneste: " + e.getMessage());
        }

        analysis.setCompletedAt(Instant.now());
        analysisRepository.save(analysis);
        log.info("Prosessering ferdig for analyse-ID: {}", analysisId);
    }
}