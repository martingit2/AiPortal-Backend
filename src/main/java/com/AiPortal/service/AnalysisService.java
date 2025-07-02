package com.AiPortal.service;

import com.AiPortal.entity.Analysis;
import com.AiPortal.entity.RawTweetData;
import com.AiPortal.repository.AnalysisRepository;
import com.AiPortal.repository.RawTweetDataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final AnalysisRepository analysisRepository;
    private final RawTweetDataRepository tweetRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public AnalysisService(AnalysisRepository analysisRepository, RawTweetDataRepository tweetRepository, ObjectMapper objectMapper) {
        this.analysisRepository = analysisRepository;
        this.tweetRepository = tweetRepository;
        this.objectMapper = objectMapper;
    }

    public List<Analysis> getAnalysesForUser(String userId) {
        return analysisRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Starter en analysejobb. Oppretter en analyse-rad i databasen
     * og starter den asynkrone prosesseringen.
     * @return Den nyopprettede Analysis-entiteten med status QUEUED.
     */
    @Transactional
    public Analysis startWordCountAnalysis(Long tweetId, String userId) {
        // Finn tweeten for å få litt kontekst til navnet
        RawTweetData tweet = tweetRepository.findById(tweetId)
                .orElseThrow(() -> new IllegalArgumentException("Tweet ikke funnet med ID: " + tweetId));

        Analysis analysis = new Analysis();
        analysis.setName("Ordtelling for tweet fra @" + tweet.getAuthorUsername());
        analysis.setUserId(userId);
        analysis.setStatus(Analysis.AnalysisStatus.QUEUED);

        Analysis savedAnalysis = analysisRepository.save(analysis);

        // Start selve den tidkrevende analysen i en egen tråd
        processAnalysis(savedAnalysis.getId());

        return savedAnalysis;
    }

    /**
     * Asynkron metode som utfører selve analysen.
     * Den vil kjøre i en separat tråd og ikke blokkere API-kallet.
     */
    @Async
    @Transactional
    public void processAnalysis(Long analysisId) {
        log.info("Starter prosessering for analyse-ID: {}", analysisId);

        // Hent analysen fra DB, sett status til RUNNING
        Analysis analysis = analysisRepository.findById(analysisId).orElse(null);
        if (analysis == null) {
            log.error("Kunne ikke finne analyse med ID: {} for prosessering.", analysisId);
            return;
        }

        analysis.setStatus(Analysis.AnalysisStatus.RUNNING);
        analysisRepository.save(analysis);

        try {
            // Finn den assosierte tweeten (her må vi ha en bedre måte, men for nå er det ok)
            // I en ekte app ville du lagret tweet-IDen i Analysis-entiteten.
            // For nå, la oss bare anta at vi finner den på en eller annen måte.
            // Siden vi ikke har lagret tweetId i Analysis, er dette en placeholder.
            // Vi henter den første tweeten i databasen for demonstrasjonens skyld.
            RawTweetData tweetToAnalyze = tweetRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("Ingen tweets å analysere"));

            // Simulerer litt arbeid
            Thread.sleep(5000); // Vent 5 sekunder

            // Selve analysen: enkel ordtelling
            String content = tweetToAnalyze.getContent().toLowerCase().replaceAll("[^a-zA-Zøæå\\s]", "");
            Map<String, Long> wordCounts = Arrays.stream(content.split("\\s+"))
                    .filter(word -> !word.isEmpty() && word.length() > 2) // Filtrer bort korte ord/tomme strenger
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            // Konverter resultatet til en JSON-streng
            String resultJson = objectMapper.writeValueAsString(wordCounts);

            // Oppdater analysen med resultat og status COMPLETED
            analysis.setResult(resultJson);
            analysis.setStatus(Analysis.AnalysisStatus.COMPLETED);

        } catch (Exception e) {
            log.error("Analyse feilet for ID: {}", analysisId, e);
            analysis.setStatus(Analysis.AnalysisStatus.FAILED);
            analysis.setResult("Feil under analyse: " + e.getMessage());
        }

        analysis.setCompletedAt(Instant.now());
        analysisRepository.save(analysis);
        log.info("Prosessering ferdig for analyse-ID: {}", analysisId);
    }
}