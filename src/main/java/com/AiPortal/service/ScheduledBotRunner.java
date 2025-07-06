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
    private final FootballApiService footballApiService; // NY SERVICE INJISERT

    public ScheduledBotRunner(BotConfigurationService botConfigService,
                              BotConfigurationRepository botConfigRepository,
                              TwitterService twitterService,
                              RawTweetDataRepository tweetRepository,
                              TwitterQueryStateRepository queryStateRepository,
                              FootballApiService footballApiService) { // NY DEPENDENCY
        this.botConfigService = botConfigService;
        this.botConfigRepository = botConfigRepository;
        this.twitterService = twitterService;
        this.tweetRepository = tweetRepository;
        this.queryStateRepository = queryStateRepository;
        this.footballApiService = footballApiService; // NY
    }

    /**
     * Kjører periodisk for å hente nye tweets fra aktive Twitter-boter.
     * Rate er satt til 16 minutter for å være trygg med Twitter API sin gratis-grense.
     */
    @Scheduled(fixedRate = 960000, initialDelay = 60000)
    @Transactional
    public void runTwitterSearchBot() {
        log.info("--- Starter planlagt Twitter-søk ---");

        List<BotConfiguration> activeTwitterBots = botConfigService.getAllBotsByStatusAndType(
                BotConfiguration.BotStatus.ACTIVE,
                BotConfiguration.SourceType.TWITTER
        );

        if (activeTwitterBots.isEmpty()) {
            log.info("Ingen aktive Twitter-boter funnet. Avslutter Twitter-kjøring.");
            return;
        }

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

        String sinceId = queryStateRepository.findById("recent_search_all_bots")
                .map(TwitterQueryState::getLastSeenTweetId)
                .orElse(null);
        log.info("Henter tweets siden ID: {}", sinceId == null ? "N/A" : sinceId);

        twitterService.searchRecentTweets(query, sinceId)
                .subscribe(responseBody -> {
                    Instant now = Instant.now();
                    activeTwitterBots.forEach(bot -> {
                        bot.setLastRun(now);
                        botConfigRepository.save(bot);
                    });
                    log.info("Oppdatert 'lastRun' for {} aktive Twitter-bot(er).", activeTwitterBots.size());

                    List<JsonNode> tweets = twitterService.parseTweetsFromResponse(responseBody);
                    String newestId = twitterService.parseNewestTweetId(responseBody);

                    if (tweets.isEmpty()) {
                        log.info("Ingen nye tweets funnet i dette intervallet.");
                    } else {
                        int newTweetsCount = 0;
                        for (JsonNode tweet : tweets) {
                            String tweetId = tweet.path("id").asText();
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

                    if (newestId != null) {
                        TwitterQueryState state = new TwitterQueryState();
                        state.setLastSeenTweetId(newestId);
                        queryStateRepository.save(state);
                        log.info("Oppdatert 'lastSeenTweetId' til: {}", newestId);
                    }

                }, error -> {
                    log.error("Feil ved kjøring av Twitter-søk: {}", error.getMessage());
                });
    }

    /**
     * NY METODE: Kjører periodisk for å hente sportsdata.
     * Har en annen tidsplan for å ikke forstyrre andre API-grenser.
     */
    @Scheduled(fixedRate = 600000, initialDelay = 120000) // Hvert 10. min, venter 2 min
    @Transactional
    public void runSportDataBots() {
        log.info("--- Starter planlagt kjøring av sportsdata-boter ---");

        List<BotConfiguration> activeSportBots = botConfigService.getAllBotsByStatusAndType(
                BotConfiguration.BotStatus.ACTIVE,
                BotConfiguration.SourceType.SPORT_API
        );

        if (activeSportBots.isEmpty()) {
            log.info("Ingen aktive sportsdata-boter funnet. Avslutter sport-kjøring.");
            return;
        }

        for (BotConfiguration bot : activeSportBots) {
            log.info("Kjører sports-bot: '{}' med kilde: {}", bot.getName(), bot.getSourceIdentifier());

            String[] params = bot.getSourceIdentifier().split(":");
            if (params.length != 3) {
                log.error("Ugyldig sourceIdentifier for sport-bot {}: {}. Forventet format: 'ligaId:sesong:lagId'", bot.getId(), bot.getSourceIdentifier());
                continue; // Gå til neste bot
            }

            String leagueId = params[0];
            String season = params[1];
            String teamId = params[2];

            footballApiService.getTeamStatistics(leagueId, season, teamId)
                    .subscribe(responseJson -> {
                        log.info("Mottatt statistikk for team {}: {}", teamId, responseJson.substring(0, Math.min(responseJson.length(), 200)) + "..."); // Logger kun starten av JSON

                        // TODO: Lagre denne dataen!
                        // 1. Lag en @Entity klasse, f.eks. TeamStatistics.java
                        // 2. Lag et TeamStatisticsRepository.
                        // 3. Bruk ObjectMapper til å parse responseJson inn i ditt/dine entity-objekter.
                        // 4. Lagre objektene i databasen.

                        // Oppdater lastRun for boten
                        bot.setLastRun(Instant.now());
                        botConfigRepository.save(bot);
                        log.info("Oppdatert lastRun for bot '{}'", bot.getName());

                    }, error -> {
                        log.error("Feil ved henting av sportsdata for bot '{}': {}", bot.getName(), error.getMessage());
                    });
        }
    }
}