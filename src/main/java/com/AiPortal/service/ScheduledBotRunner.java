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

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ScheduledBotRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledBotRunner.class);

    private final BotConfigurationRepository botConfigRepository;
    private final TwitterService twitterService;
    private final RawTweetDataRepository tweetRepository;
    private final TwitterQueryStateRepository queryStateRepository;

    public ScheduledBotRunner(BotConfigurationRepository botConfigRepository,
                              TwitterService twitterService,
                              RawTweetDataRepository tweetRepository,
                              TwitterQueryStateRepository queryStateRepository) {
        this.botConfigRepository = botConfigRepository;
        this.twitterService = twitterService;
        this.tweetRepository = tweetRepository;
        this.queryStateRepository = queryStateRepository;
    }

    // Kjører hvert 3. minutt (180 000 ms) for å være trygt innenfor 1 kall/15 min.
    // I produksjon kan du øke denne for å spare API-kvote.
    @Scheduled(fixedRate = 960000)
    public void runTwitterSearchBot() {
        log.info("Starter planlagt Twitter-søk...");

        // 1. Hent alle aktive Twitter-boter
        List<BotConfiguration> activeTwitterBots = botConfigRepository.findAll().stream()
                .filter(bot -> bot.getStatus() == BotConfiguration.BotStatus.ACTIVE &&
                        bot.getSourceType() == BotConfiguration.SourceType.TWITTER)
                .collect(Collectors.toList());

        if (activeTwitterBots.isEmpty()) {
            log.info("Ingen aktive Twitter-boter funnet. Avslutter kjøring.");
            return;
        }

        // 2. Bygg søkestrengen
        String query = activeTwitterBots.stream()
                .map(bot -> "from:" + bot.getSourceIdentifier())
                .collect(Collectors.joining(" OR "));
        query += " -is:retweet"; // Ekskluderer retweets
        log.info("Bygget Twitter-spørring: {}", query);

        // 3. Hent sist sette tweet-ID fra databasen
        String sinceId = queryStateRepository.findById("recent_search_all_bots")
                .map(TwitterQueryState::getLastSeenTweetId)
                .orElse(null);
        log.info("Henter tweets siden ID: {}", sinceId == null ? "N/A" : sinceId);


        // 4. Utfør API-kallet
        twitterService.searchRecentTweets(query, sinceId)
                .subscribe(responseBody -> {
                    // Parse svaret
                    List<JsonNode> tweets = twitterService.parseTweetsFromResponse(responseBody);
                    String newestId = twitterService.parseNewestTweetId(responseBody);

                    if (tweets.isEmpty()) {
                        log.info("Ingen nye tweets funnet.");
                        return;
                    }

                    // 5. Lagre nye tweets i databasen
                    for (JsonNode tweet : tweets) {
                        String tweetId = tweet.path("id").asText();
                        if (!tweetRepository.existsByTweetId(tweetId)) {
                            String authorId = tweet.path("author_id").asText();
                            String authorUsername = twitterService.findUsernameFromIncludes(responseBody, authorId);

                            RawTweetData newTweetData = new RawTweetData();
                            newTweetData.setTweetId(tweetId);
                            newTweetData.setAuthorUsername(authorUsername);
                            newTweetData.setContent(tweet.path("text").asText());
                            newTweetData.setTweetedAt(Instant.parse(tweet.path("created_at").asText()));
                            tweetRepository.save(newTweetData);
                        }
                    }
                    log.info("Lagret {} nye tweets.", tweets.size());

                    // 6. Oppdater sist sette tweet-ID
                    if (newestId != null) {
                        TwitterQueryState state = new TwitterQueryState();
                        state.setLastSeenTweetId(newestId);
                        queryStateRepository.save(state);
                        log.info("Oppdatert lastSeenTweetId til: {}", newestId);
                    }

                }, error -> {
                    log.error("Feil ved kjøring av Twitter-søk: {}", error.getMessage());
                });
    }
}