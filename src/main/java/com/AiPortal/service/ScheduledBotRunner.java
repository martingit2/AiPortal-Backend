// src/main/java/com/AiPortal/service/ScheduledBotRunner.java

package com.AiPortal.service;

import com.AiPortal.entity.*;
import com.AiPortal.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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
    private final FootballApiService footballApiService;
    private final TeamStatisticsRepository statsRepository;
    private final FixtureRepository fixtureRepository;
    private final MatchOddsRepository matchOddsRepository;
    private final BookmakerRepository bookmakerRepository;
    private final BetTypeRepository betTypeRepository;
    private final ObjectMapper objectMapper;

    public ScheduledBotRunner(BotConfigurationService botConfigService, BotConfigurationRepository botConfigRepository, TwitterService twitterService, RawTweetDataRepository tweetRepository, TwitterQueryStateRepository queryStateRepository, FootballApiService footballApiService, TeamStatisticsRepository statsRepository, FixtureRepository fixtureRepository, MatchOddsRepository matchOddsRepository, BookmakerRepository bookmakerRepository, BetTypeRepository betTypeRepository, ObjectMapper objectMapper) {
        this.botConfigService = botConfigService;
        this.botConfigRepository = botConfigRepository;
        this.twitterService = twitterService;
        this.tweetRepository = tweetRepository;
        this.queryStateRepository = queryStateRepository;
        this.footballApiService = footballApiService;
        this.statsRepository = statsRepository;
        this.fixtureRepository = fixtureRepository;
        this.matchOddsRepository = matchOddsRepository;
        this.bookmakerRepository = bookmakerRepository;
        this.betTypeRepository = betTypeRepository;
        this.objectMapper = objectMapper;
    }

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

                    if (!tweets.isEmpty()) {
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
                    } else {
                        log.info("Ingen nye tweets funnet i dette intervallet.");
                    }

                    if (newestId != null) {
                        TwitterQueryState state = new TwitterQueryState();
                        state.setLastSeenTweetId(newestId);
                        queryStateRepository.save(state);
                        log.info("Oppdatert 'lastSeenTweetId' til: {}", newestId);
                    }

                }, error -> {
                    if (error instanceof WebClientResponseException wce && wce.getStatusCode().value() == 429) {
                        log.warn("Twitter API rate limit truffet. Jobben vil prøve igjen i neste intervall.");
                    } else {
                        log.error("Feil ved kjøring av Twitter-søk: {}", error.getMessage());
                    }
                });
    }

    @Scheduled(cron = "0 0 5 * * *", zone = "Europe/Oslo")
    @Transactional
    public void updateFootballMetadata() {
        log.info("--- Starter planlagt jobb for å oppdatere fotball-metadata (Bookmakere, Spilltyper) ---");
        footballApiService.getBookmakers().subscribe(responseEntity -> {
            try {
                String json = responseEntity.getBody();
                JsonNode responses = objectMapper.readTree(json).path("response");
                if (responses.isArray()) {
                    for (JsonNode bookmakerNode : responses) {
                        Bookmaker b = new Bookmaker();
                        b.setId(bookmakerNode.path("id").asInt());
                        b.setName(bookmakerNode.path("name").asText());
                        bookmakerRepository.save(b);
                    }
                    log.info("Oppdatert {} bookmakere.", responses.size());
                }
            } catch (Exception e) { log.error("Feil ved parsing av bookmakere", e); }
        });
        footballApiService.getBetTypes().subscribe(responseEntity -> {
            try {
                String json = responseEntity.getBody();
                JsonNode responses = objectMapper.readTree(json).path("response");
                if (responses.isArray()) {
                    for (JsonNode betNode : responses) {
                        BetType bt = new BetType();
                        bt.setId(betNode.path("id").asInt());
                        bt.setName(betNode.path("name").asText());
                        betTypeRepository.save(bt);
                    }
                    log.info("Oppdatert {} spilltyper.", responses.size());
                }
            } catch (Exception e) { log.error("Feil ved parsing av spilltyper", e); }
        });
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "Europe/Oslo")
    public void fetchDailyOdds() {
        String tomorrow = LocalDate.now().plusDays(1).toString();
        log.info("--- Starter planlagt jobb for å hente odds for dato: {} ---", tomorrow);

        footballApiService.getOddsByDate(tomorrow)
                .flatMap(responseEntity -> {
                    if (!responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
                        log.warn("Mottok ugyldig svar fra odds-API: status={}, body er tom.", responseEntity.getStatusCode());
                        return Mono.empty();
                    }
                    try {
                        JsonNode root = objectMapper.readTree(responseEntity.getBody());
                        if (root.has("errors") && !root.path("errors").isEmpty()) {
                            log.error("Odds-API returnerte feil i body: {}", root.path("errors"));
                            return Mono.empty();
                        }
                        JsonNode responses = root.path("response");
                        if (!responses.isArray() || responses.isEmpty()) {
                            log.warn("Ingen odds funnet for dato: {}. 'response'-arrayen er tom eller mangler.", tomorrow);
                            return Mono.empty();
                        }
                        return Flux.fromIterable(responses)
                                .flatMap(this::processSingleFixtureWithOdds)
                                .collectList()
                                .doOnSuccess(processedFixtures -> {
                                    log.info("Fullførte prosessering av odds for {} kamper for dato: {}", processedFixtures.size(), tomorrow);
                                });
                    } catch (Exception e) {
                        log.error("Kritisk feil ved parsing av ytre odds-respons. Body: {}", responseEntity.getBody(), e);
                        return Mono.error(e);
                    }
                })
                .timeout(Duration.ofMinutes(5))
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException ex) {
                        log.error("API-kall for odds feilet med status: {}. Body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                    } else {
                        log.error("En uventet feil oppstod i WebClient-strømmen ved henting av odds-data.", error);
                    }
                })
                .subscribe();
    }

    private Mono<Fixture> processSingleFixtureWithOdds(JsonNode oddsResponse) {
        long fixtureId = oddsResponse.path("fixture").path("id").asLong();

        JsonNode initialHomeNode = oddsResponse.path("teams").path("home");
        JsonNode initialAwayNode = oddsResponse.path("teams").path("away");
        boolean hasTeamData = initialHomeNode.has("id") && initialAwayNode.has("id");

        if (hasTeamData) {
            return Mono.fromCallable(() -> saveFixtureAndOdds(oddsResponse))
                    .subscribeOn(Schedulers.boundedElastic())
                    .onErrorResume(e -> {
                        log.error("Feil under lagring av fixture {} (happy path)", fixtureId, e);
                        return Mono.empty();
                    });
        } else {
            log.warn("Team-data mangler i odds-respons for fixture ID: {}. Henter detaljer separat...", fixtureId);
            return footballApiService.getFixtureById(fixtureId)
                    .flatMap(detailsResponseEntity -> Mono.fromCallable(() -> {
                                if (detailsResponseEntity.getStatusCode().is2xxSuccessful() && detailsResponseEntity.getBody() != null) {
                                    JsonNode detailsRoot = objectMapper.readTree(detailsResponseEntity.getBody());
                                    JsonNode fixtureDataArray = detailsRoot.path("response");
                                    if (fixtureDataArray.isArray() && !fixtureDataArray.isEmpty()) {
                                        JsonNode fullFixtureData = fixtureDataArray.get(0);
                                        return saveFixtureAndOdds(oddsResponse, fullFixtureData);
                                    }
                                }
                                log.error("Kunne ikke hente/parse separate detaljer for fixture ID: {}. Hopper over.", fixtureId);
                                return null;
                            }).subscribeOn(Schedulers.boundedElastic())
                    )
                    .flatMap(savedFixture -> (savedFixture != null) ? Mono.just(savedFixture) : Mono.empty())
                    .onErrorResume(e -> {
                        log.error("Feil under separat henting/lagring for fixture {}", fixtureId, e);
                        return Mono.empty();
                    });
        }
    }

    @Transactional
    public Fixture saveFixtureAndOdds(JsonNode oddsData, JsonNode fixtureData) {
        long fixtureId = fixtureData.path("fixture").path("id").asLong();

        Fixture fixture = fixtureRepository.findById(fixtureId).orElse(new Fixture());
        fixture.setId(fixtureId);
        fixture.setLeagueId(fixtureData.path("league").path("id").asInt());
        fixture.setSeason(fixtureData.path("league").path("season").asInt());
        fixture.setDate(Instant.parse(fixtureData.path("fixture").path("date").asText()));
        fixture.setStatus(fixtureData.path("fixture").path("status").path("short").asText());
        fixture.setHomeTeamId(fixtureData.path("teams").path("home").path("id").asInt());
        fixture.setHomeTeamName(fixtureData.path("teams").path("home").path("name").asText());
        fixture.setAwayTeamId(fixtureData.path("teams").path("away").path("id").asInt());
        fixture.setAwayTeamName(fixtureData.path("teams").path("away").path("name").asText());

        Fixture savedFixture = fixtureRepository.save(fixture);

        JsonNode bookmakers = oddsData.path("bookmakers");
        if (bookmakers.isArray()) {
            for (JsonNode bookmakerNode : bookmakers) {
                int bookmakerId = bookmakerNode.path("id").asInt();
                for (JsonNode betNode : bookmakerNode.path("bets")) {
                    if (betNode.path("id").asInt() == 1) { // 'Match Winner'
                        MatchOdds odds = new MatchOdds();
                        odds.setFixture(savedFixture);
                        bookmakerRepository.findById(bookmakerId).ifPresent(odds::setBookmaker);
                        betTypeRepository.findById(1).ifPresent(odds::setBetType);
                        for (JsonNode value : betNode.path("values")) {
                            switch (value.path("value").asText()) {
                                case "Home": odds.setHomeOdds(value.path("odd").asDouble()); break;
                                case "Draw": odds.setDrawOdds(value.path("odd").asDouble()); break;
                                case "Away": odds.setAwayOdds(value.path("odd").asDouble()); break;
                            }
                        }
                        matchOddsRepository.save(odds);
                        break;
                    }
                }
            }
        }
        return savedFixture;
    }

    @Transactional
    public Fixture saveFixtureAndOdds(JsonNode allData) {
        return saveFixtureAndOdds(allData, allData);
    }

    /**
     * Kjører for boter av typen SPORT_API (enkelt-lag).
     */
    @Scheduled(fixedRate = 600000, initialDelay = 120000)
    public void runSportDataBots() {
        log.info("--- Starter planlagt kjøring av sportsdata-boter (enkelt-lag) ---");

        List<BotConfiguration> activeSportBots = botConfigService.getAllBotsByStatusAndType(
                BotConfiguration.BotStatus.ACTIVE,
                BotConfiguration.SourceType.SPORT_API
        );

        if (activeSportBots.isEmpty()) {
            log.info("Ingen aktive SPORT_API-boter funnet.");
            return;
        }

        for (BotConfiguration bot : activeSportBots) {
            log.info("Kjører SPORT_API-bot: '{}' med kilde: {}", bot.getName(), bot.getSourceIdentifier());

            String[] params = bot.getSourceIdentifier().split(":");
            if (params.length != 3) {
                log.error("Ugyldig sourceIdentifier for sport-bot {}: {}. Forventet format: 'ligaId:sesong:lagId'", bot.getId(), bot.getSourceIdentifier());
                continue;
            }
            String leagueIdStr = params[0], seasonStr = params[1], teamIdStr = params[2];

            footballApiService.getTeamStatistics(leagueIdStr, seasonStr, teamIdStr)
                    .subscribe(responseEntity -> {
                        try {
                            String responseJson = responseEntity.getBody();
                            if (responseJson != null) {
                                JsonNode root = objectMapper.readTree(responseJson);
                                JsonNode response = root.path("response");
                                saveTeamStatistics(response, bot);
                            }
                        } catch (Exception e) {
                            log.error("Feil ved parsing av sportsdata for bot '{}'", bot.getName(), e);
                        }
                    }, error -> log.error("Feil ved henting av sportsdata for bot '{}'", bot.getName(), error));
        }
    }

    /**
     * Henter statistikk for en hel liga.
     * Denne metoden er @Async for å kjøre i en egen tråd og ikke blokkere web-forespørsler.
     * Den bruker en enkel for-løkke med innebygd pause for å håndtere API rate-limiting.
     */
    @Async
    @Scheduled(fixedRate = 86400000, initialDelay = 180000)
    public void runLeagueStatsCollector() {
        log.info("--- Starter planlagt kjøring av liga-statistikk-innsamler ---");
        List<BotConfiguration> leagueBots = botConfigService.getAllBotsByStatusAndType(
                BotConfiguration.BotStatus.ACTIVE,
                BotConfiguration.SourceType.LEAGUE_STATS
        );

        if (leagueBots.isEmpty()) {
            log.info("Ingen aktive LEAGUE_STATS-boter funnet.");
            return;
        }

        for (BotConfiguration bot : leagueBots) {
            String[] params = bot.getSourceIdentifier().split(":");
            if (params.length != 2) {
                log.error("Ugyldig sourceIdentifier for LEAGUE_STATS-bot {}: {}. Forventet format: 'ligaId:sesong'", bot.getId(), bot.getSourceIdentifier());
                continue;
            }
            String leagueId = params[0];
            String season = params[1];
            log.info("Starter innsamling for liga: {}, sesong: {} (fra bot: '{}')", leagueId, season, bot.getName());

            try {
                // Steg 1: Hent listen av lag i ligaen (blokkerende kall)
                ResponseEntity<String> teamsResponse = footballApiService.getTeamsInLeague(leagueId, season).block();
                if (teamsResponse == null || !teamsResponse.getStatusCode().is2xxSuccessful() || teamsResponse.getBody() == null) {
                    log.error("Kunne ikke hente lagliste for liga {}.", leagueId);
                    continue;
                }

                JsonNode teamsArray = objectMapper.readTree(teamsResponse.getBody()).path("response");
                if (!teamsArray.isArray() || teamsArray.isEmpty()) {
                    log.warn("Fant ingen lag for liga {} sesong {}.", leagueId, season);
                    continue;
                }

                log.info("Fant {} lag for liga {}. Starter henting av statistikk for hvert lag...", teamsArray.size(), leagueId);

                // Steg 2: Loop gjennom hvert lag med en innebygd pause
                for (JsonNode teamNode : teamsArray) {
                    String teamId = teamNode.path("team").path("id").asText();

                    try {
                        // Hent statistikk for dette ene laget (blokkerende kall)
                        ResponseEntity<String> statsResponse = footballApiService.getTeamStatistics(leagueId, season, teamId).block();
                        if (statsResponse != null && statsResponse.getBody() != null) {
                            JsonNode response = objectMapper.readTree(statsResponse.getBody()).path("response");
                            saveTeamStatistics(response, bot);
                        }

                        // Pause for å respektere rate limits. 2.5 sekunder er trygt for 30 kall/minutt.
                        Thread.sleep(2500);

                    } catch (WebClientResponseException e) {
                        if (e.getStatusCode().value() == 429) {
                            log.error("Rate limit truffet for team {}. Venter 60 sekunder før fortsettelse.", teamId);
                            Thread.sleep(60000);
                        } else {
                            log.error("API-feil (status: {}) ved henting av stats for team {}: {}", e.getStatusCode(), teamId, e.getResponseBodyAsString());
                        }
                    } catch (InterruptedException e) {
                        log.warn("Tråden ble avbrutt under pause. Avslutter innsamling for denne ligaen.");
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.error("Uventet feil ved prosessering av team {}", teamId, e);
                    }
                }

                log.info("Fullførte innsamling for alle lag i liga {}.", leagueId);
                bot.setLastRun(Instant.now());
                botConfigRepository.save(bot);

            } catch (Exception e) {
                log.error("En kritisk feil oppstod under innsamling for liga {}", leagueId, e);
            }
        }
    }

    /**
     * Hjelpemetode for å lagre TeamStatistics. Gjenbrukes av både enkelt-lag og liga-boter.
     * Denne metoden er @Transactional for å sikre atomiske databaseoperasjoner.
     */
    @Transactional
    public void saveTeamStatistics(JsonNode statsResponse, BotConfiguration sourceBot) {
        if (statsResponse.isMissingNode() || !statsResponse.isObject() || statsResponse.size() == 0) {
            log.warn("Mottok tomt eller ugyldig statsResponse-objekt. Hopper over lagring.");
            return;
        }
        int teamId = statsResponse.path("team").path("id").asInt();
        int leagueId = statsResponse.path("league").path("id").asInt();
        int season = statsResponse.path("league").path("season").asInt();

        TeamStatistics stats = statsRepository.findByLeagueIdAndSeasonAndTeamId(leagueId, season, teamId)
                .orElse(new TeamStatistics());

        stats.setTeamId(teamId);
        stats.setLeagueId(leagueId);
        stats.setSeason(season);
        stats.setTeamName(statsResponse.path("team").path("name").asText());
        stats.setLeagueName(statsResponse.path("league").path("name").asText());
        stats.setPlayedTotal(statsResponse.path("fixtures").path("played").path("total").asInt());
        stats.setWinsTotal(statsResponse.path("fixtures").path("wins").path("total").asInt());
        stats.setDrawsTotal(statsResponse.path("fixtures").path("draws").path("total").asInt());
        stats.setLossesTotal(statsResponse.path("fixtures").path("loses").path("total").asInt());
        stats.setGoalsForTotal(statsResponse.path("goals").path("for").path("total").path("total").asInt());
        stats.setGoalsAgainstTotal(statsResponse.path("goals").path("against").path("total").path("total").asInt());
        stats.setCleanSheetTotal(statsResponse.path("clean_sheet").path("total").asInt());
        stats.setFailedToScoreTotal(statsResponse.path("failed_to_score").path("total").asInt());
        stats.setSourceBot(sourceBot);
        stats.setLastUpdated(Instant.now());

        statsRepository.save(stats);
        log.info("Lagret/oppdatert statistikk for team: {} (ID: {})", stats.getTeamName(), stats.getTeamId());

        // Oppdaterer kun lastRun for den originale enkelt-lag boten
        if (sourceBot != null && sourceBot.getSourceType() == BotConfiguration.SourceType.SPORT_API) {
            sourceBot.setLastRun(Instant.now());
            botConfigRepository.save(sourceBot);
            log.info("Oppdatert lastRun for enkelt-lag-bot '{}'", sourceBot.getName());
        }
    }
}