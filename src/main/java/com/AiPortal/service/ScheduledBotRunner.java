package com.AiPortal.service;

import com.AiPortal.entity.BotConfiguration;
import com.AiPortal.entity.RawTweetData;
import com.AiPortal.entity.TwitterQueryState;
import com.AiPortal.repository.BotConfigurationRepository;
import com.AiPortal.repository.RawTweetDataRepository;
import com.AiPortal.repository.TwitterQueryStateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ScheduledBotRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledBotRunner.class);

    private final BotConfigurationService botConfigService;
    private final BotConfigurationRepository botConfigRepository;
    private final TwitterService twitterService;
    private final RawTweetDataRepository tweetRepository;
    private final TwitterQueryStateRepository queryStateRepository;

    public ScheduledBotRunner(BotConfigurationService botConfigService,
                              BotConfigurationRepository botConfigRepository,
                              TwitterService twitterService,
                              RawTweetDataRepository tweetRepository,
                              TwitterQueryStateRepository queryStateRepository) {
        this.botConfigService = botConfigService;
        this.botConfigRepository = botConfigRepository;
        this.twitterService = twitterService;
        this.tweetRepository = tweetRepository;
        this.queryStateRepository = queryStateRepository;
    }

    /**
     * Kjører periodisk for å hente nye tweets fra aktive Twitter-boter.
     * fixedRate er satt til 960 000 ms (16 minutter) for å være trygt innenfor Twitter API sin gratis-grense (1 kall / 15 min).
     * initialDelay venter 60 sekunder etter app-oppstart før første kjøring.
     */
    @Scheduled(fixedRate = 960000, initialDelay = 60000)
    @Transactional
    public void runTwitterSearchBot() {
        log.info("Starter planlagt Twitter-søk...");

        // 1. Hent alle aktive Twitter-boter
        List<BotConfiguration> activeTwitterBots = botConfigService.getAllBotsByStatusAndType(
                BotConfiguration.BotStatus.ACTIVE,
                BotConfiguration.SourceType.TWITTER
        );

        if (activeTwitterBots.isEmpty()) {
            log.info("Ingen aktive Twitter-boter funnet. Avslutter kjøring.");
            return;
        }

        // 2. Bygg en map og en spørring fra de aktive botene
        Map<String, BotConfiguration> botMap = activeTwitterBots.stream()
                .collect(Collectors.toMap(
                        bot -> bot.getSourceIdentifier().toLowerCase(),
                        Function.identity()
                ));

        String query = activeTwitterBots.stream()
                .map(bot -> "from:" + bot.getSourceIdentifier())
                .collect(Collectors.joining(" OR "));
        query += " -is:retweet";
        log.info("Bygget Twitter-spørring: {}", query);

        // 3. Hent sist sette tweet-ID for å unngå duplikater
        String sinceId = queryStateRepository.findById("recent_search_all_bots")
                .map(TwitterQueryState::getLastSeenTweetId)
                .orElse(null);
        log.info("Henter tweets siden ID: {}", sinceId == null ? "N/A (henter de nyeste)" : sinceId);


        // 4. Utfør API-kallet
        twitterService.searchRecentTweets(query, sinceId)
                .subscribe(responseBody -> { // Denne blokken kjøres ved et vellykket API-svar (status 200 OK)

                    // --- Logikken for å oppdatere lastRun kjøres nå alltid ved suksess ---
                    Instant now = Instant.now();
                    activeTwitterBots.forEach(bot -> {
                        bot.setLastRun(now);
                        botConfigRepository.save(bot); // Lagre endringen for hver bot
                    });
                    log.info("Oppdatert 'lastRun' for {} aktive bot(er).", activeTwitterBots.size());


                    // --- Fortsett med å behandle svaret ---
                    List<JsonNode> tweets = twitterService.parseTweetsFromResponse(responseBody);
                    String newestId = twitterService.parseNewestTweetId(responseBody);

                    if (tweets.isEmpty()) {
                        log.info("Ingen nye tweets funnet i dette intervallet.");
                    } else {
                        int newTweetsCount = 0;
                        for (JsonNode tweet : tweets) {
                            String tweetId = tweet.path("id").asText();
                            // Sjekk om tweeten allerede er lagret for å være helt sikker
                            if (!tweetRepository.existsByTweetId(tweetId)) {
                                String authorId = tweet.path("author_id").asText();
                                String authorUsername = twitterService.findUsernameFromIncludes(responseBody, authorId);

                                BotConfiguration sourceBot = botMap.get(authorUsername.toLowerCase());

                                if (sourceBot != null) {
                                    RawTweetData newTweetData = new RawTweetData();
                                    newTweetData.setTweetId(tweetId);
                                    newTweetData.setAuthorUsername(authorUsername);
                                    newTweetData.setContent(tweet.path("text").asText());
                                    newTweetData.setTweetedAt(Instant.parse(tweet.path("created_at").asText()));
                                    newTweetData.setSourceBot(sourceBot);
                                    tweetRepository.save(newTweetData);
                                    newTweetsCount++;
                                }
                            }
                        }
                        if (newTweetsCount > 0) {
                            log.info("Lagret {} nye tweets i databasen.", newTweetsCount);
                        }
                    }

                    // Oppdater 'since_id' for neste kjøring
                    if (newestId != null) {
                        TwitterQueryState state = new TwitterQueryState();
                        state.setLastSeenTweetId(newestId);
                        queryStateRepository.save(state);
                        log.info("Oppdatert 'lastSeenTweetId' til: {}", newestId);
                    }

                }, error -> { // Denne blokken kjøres ved en API-feil (f.eks. 429 Too Many Requests)
                    // Vi oppdaterer IKKE lastRun ved feil, slik at vi vet at siste kjøring feilet.
                    log.error("Feil ved kjøring av Twitter-søk: {}", error.getMessage());
                });
    }
}