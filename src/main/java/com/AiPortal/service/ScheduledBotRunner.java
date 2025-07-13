// src/main/java/com/AiPortal/service/ScheduledBotRunner.java

package com.AiPortal.service;

import com.AiPortal.entity.*;
import com.AiPortal.repository.*;
import com.AiPortal.service.twitter.TwitterServiceProvider;
import com.AiPortal.service.twitter.TwitterServiceManager;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class ScheduledBotRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledBotRunner.class);
    private static final DateTimeFormatter TWITTER_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);

    private final BotConfigurationService botConfigService;
    private final BotConfigurationRepository botConfigRepository;
    private final TwitterServiceManager twitterServiceManager;
    private final RawTweetDataRepository tweetRepository;
    private final TwitterQueryStateRepository queryStateRepository;
    private final FootballApiService footballApiService;
    private final TeamStatisticsRepository statsRepository;
    private final FixtureRepository fixtureRepository;
    private final MatchOddsRepository matchOddsRepository;
    private final BookmakerRepository bookmakerRepository;
    private final BetTypeRepository betTypeRepository;
    private final MatchStatisticsRepository matchStatisticsRepository;
    private final InjuryRepository injuryRepository;
    private final ObjectMapper objectMapper;

    public ScheduledBotRunner(BotConfigurationService botConfigService, BotConfigurationRepository botConfigRepository, TwitterServiceManager twitterServiceManager, RawTweetDataRepository tweetRepository, TwitterQueryStateRepository queryStateRepository, FootballApiService footballApiService, TeamStatisticsRepository statsRepository, FixtureRepository fixtureRepository, MatchOddsRepository matchOddsRepository, BookmakerRepository bookmakerRepository, BetTypeRepository betTypeRepository, MatchStatisticsRepository matchStatisticsRepository, InjuryRepository injuryRepository, ObjectMapper objectMapper) {
        this.botConfigService = botConfigService;
        this.botConfigRepository = botConfigRepository;
        this.twitterServiceManager = twitterServiceManager;
        this.tweetRepository = tweetRepository;
        this.queryStateRepository = queryStateRepository;
        this.footballApiService = footballApiService;
        this.statsRepository = statsRepository;
        this.fixtureRepository = fixtureRepository;
        this.matchOddsRepository = matchOddsRepository;
        this.bookmakerRepository = bookmakerRepository;
        this.betTypeRepository = betTypeRepository;
        this.matchStatisticsRepository = matchStatisticsRepository;
        this.injuryRepository = injuryRepository;
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

        for (BotConfiguration bot : activeTwitterBots) {
            TwitterServiceProvider twitterProvider = twitterServiceManager.getNextProvider();
            if (twitterProvider == null) {
                log.warn("Ingen flere Twitter-leverandører tilgjengelig for denne kjøringen.");
                break;
            }

            String username = bot.getSourceIdentifier();
            String query = "from:" + username;

            if ("TwttrAPI241".equals(twitterProvider.getProviderName())) {
                query = username;
            } else if ("TwitterAPI45".equals(twitterProvider.getProviderName())) {
                query = username;
            }

            log.info("Kjører bot '{}' med leverandør '{}' for query '{}'", bot.getName(), twitterProvider.getProviderName(), query);

            twitterProvider.searchRecentTweets(query, null)
                    .subscribe(responseBody -> {
                        List<JsonNode> tweets = twitterProvider.parseTweetsFromResponse(responseBody);
                        int newTweetsCount = 0;

                        for (JsonNode tweet : tweets) {
                            String tweetId;
                            if ("TwttrAPI241".equals(twitterProvider.getProviderName())) {
                                tweetId = tweet.path("rest_id").asText();
                            } else {
                                tweetId = tweet.path("id_str").asText(tweet.path("id").asText());
                            }

                            if (tweetId.isEmpty() || tweetRepository.existsByTweetId(tweetId)) {
                                continue;
                            }

                            RawTweetData newTweetData = new RawTweetData();
                            newTweetData.setTweetId(tweetId);
                            newTweetData.setAuthorUsername(username);

                            if ("TwttrAPI241".equals(twitterProvider.getProviderName())) {
                                newTweetData.setContent(tweet.path("legacy").path("full_text").asText());
                            } else {
                                newTweetData.setContent(tweet.path("text").asText(tweet.path("full_text").asText()));
                            }

                            String createdAtStr = tweet.path("legacy").path("created_at").asText(tweet.path("created_at").asText());
                            try {
                                newTweetData.setTweetedAt(Instant.from(TWITTER_DATE_FORMATTER.parse(createdAtStr)));
                            } catch (DateTimeParseException e) {
                                try {
                                    newTweetData.setTweetedAt(Instant.parse(createdAtStr));
                                } catch (Exception e2) {
                                    log.warn("Kunne ikke parse dato: '{}' for tweet {}. Bruker nåtid.", createdAtStr, tweetId);
                                    newTweetData.setTweetedAt(Instant.now());
                                }
                            }

                            newTweetData.setSourceBot(bot);
                            tweetRepository.save(newTweetData);
                            newTweetsCount++;
                        }

                        if (newTweetsCount > 0) {
                            log.info("Lagret {} nye tweets for bot '{}'", newTweetsCount, bot.getName());
                        }
                        bot.setLastRun(Instant.now());
                        botConfigRepository.save(bot);

                    }, error -> {
                        log.error("Feil for bot '{}' med leverandør '{}': {}", bot.getName(), twitterProvider.getProviderName(), error.getMessage());
                    });

            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Twitter-bot-løkke ble avbrutt.", e);
                break;
            }
        }
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
        // Lager en liste med dato-strenger for i dag og de neste 6 dagene (totalt 7 dager)
        List<String> datesToFetch = IntStream.range(0, 7)
                .mapToObj(i -> LocalDate.now().plusDays(i).toString())
                .collect(Collectors.toList());

        log.info("--- Starter planlagt jobb for å hente odds for de neste 7 dagene: {} ---", datesToFetch);

        for (String date : datesToFetch) {
            log.info("Henter odds for dato: {}", date);
            footballApiService.getOddsByDate(date)
                    .flatMap(responseEntity -> {
                        if (!responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
                            log.warn("Mottok ugyldig svar fra odds-API for dato {}: status={}, body er tom.", date, responseEntity.getStatusCode());
                            return Mono.empty();
                        }
                        try {
                            JsonNode root = objectMapper.readTree(responseEntity.getBody());
                            if (root.has("errors") && !root.path("errors").isEmpty()) {
                                log.error("Odds-API returnerte feil i body for dato {}: {}", date, root.path("errors"));
                                return Mono.empty();
                            }
                            JsonNode responses = root.path("response");
                            if (!responses.isArray() || responses.isEmpty()) {
                                log.warn("Ingen odds funnet for dato: {}. 'response'-arrayen er tom eller mangler.", date);
                                return Mono.empty();
                            }
                            return Flux.fromIterable(responses)
                                    .flatMap(this::processSingleFixtureWithOdds)
                                    .collectList()
                                    .doOnSuccess(processedFixtures -> {
                                        if (!processedFixtures.isEmpty()) {
                                            log.info("Fullførte prosessering av odds for {} kamper for dato: {}", processedFixtures.size(), date);
                                        }
                                    });
                        } catch (Exception e) {
                            log.error("Kritisk feil ved parsing av ytre odds-respons for dato {}. Body: {}", date, responseEntity.getBody(), e);
                            return Mono.error(e);
                        }
                    })
                    .timeout(Duration.ofMinutes(5)) // Timeout per dato
                    .doOnError(error -> {
                        if (error instanceof WebClientResponseException ex) {
                            log.error("API-kall for odds feilet for dato {} med status: {}. Body: {}", date, ex.getStatusCode(), ex.getResponseBodyAsString());
                        } else {
                            log.error("En uventet feil oppstod i WebClient-strømmen ved henting av odds-data for dato {}.", date, error);
                        }
                    })
                    .subscribe();

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Odds-innhenting ble avbrutt under pause.");
                break;
            }
        }
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

        JsonNode goalsNode = fixtureData.path("goals");
        if (!goalsNode.path("home").isNull()) {
            fixture.setGoalsHome(goalsNode.path("home").asInt());
        }
        if (!goalsNode.path("away").isNull()) {
            fixture.setGoalsAway(goalsNode.path("away").asInt());
        }

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

                for (JsonNode teamNode : teamsArray) {
                    String teamId = teamNode.path("team").path("id").asText();

                    try {
                        ResponseEntity<String> statsResponse = footballApiService.getTeamStatistics(leagueId, season, teamId).block();
                        if (statsResponse != null && statsResponse.getBody() != null) {
                            JsonNode response = objectMapper.readTree(statsResponse.getBody()).path("response");
                            saveTeamStatistics(response, bot);
                        }

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

    @Async
    @Scheduled(cron = "0 0 2 * * *")
    public void runHistoricalDataCollector() {
        log.info("---[HISTORICAL COLLECTOR]--- Starter innsamling av detaljert kampdata.");
        List<BotConfiguration> historicalBots = botConfigService.getAllBotsByStatusAndType(
                BotConfiguration.BotStatus.ACTIVE,
                BotConfiguration.SourceType.HISTORICAL_FIXTURE_DATA
        );

        if (historicalBots.isEmpty()) {
            log.info("---[HISTORICAL COLLECTOR]--- Ingen aktive HISTORICAL_FIXTURE_DATA-boter funnet.");
            return;
        }

        for (BotConfiguration bot : historicalBots) {
            String[] params = bot.getSourceIdentifier().split(":");
            if (params.length != 2) {
                log.error("---[HISTORICAL COLLECTOR]--- Ugyldig sourceIdentifier for bot {}: {}. Forventet format: 'ligaId:sesong'", bot.getId(), bot.getSourceIdentifier());
                continue;
            }
            String leagueId = params[0];
            String season = params[1];
            log.info("---[HISTORICAL COLLECTOR]--- Starter for liga: {}, sesong: {}", leagueId, season);

            try {
                ResponseEntity<String> fixturesResponse = footballApiService.getFixturesByLeagueAndSeason(leagueId, season).block();
                if (fixturesResponse == null || !fixturesResponse.getStatusCode().is2xxSuccessful() || fixturesResponse.getBody() == null) {
                    log.error("---[HISTORICAL COLLECTOR]--- Kunne ikke hente kampliste for liga {}.", leagueId);
                    continue;
                }

                JsonNode fixturesArray = objectMapper.readTree(fixturesResponse.getBody()).path("response");
                if (!fixturesArray.isArray() || fixturesArray.isEmpty()) {
                    log.warn("---[HISTORICAL COLLECTOR]--- Fant ingen kamper for liga {} sesong {}.", leagueId, season);
                    continue;
                }

                int newFixturesCount = 0;
                for (JsonNode fixtureNode : fixturesArray) {
                    boolean wasUpdated = saveOrUpdateFixtureFromJson(fixtureNode);
                    if (wasUpdated) {
                        newFixturesCount++;
                    }
                }
                if (newFixturesCount > 0) {
                    log.info("---[HISTORICAL COLLECTOR]--- Lagret/oppdatert {} kamper i databasen for liga {}.", newFixturesCount, leagueId);
                } else {
                    log.info("---[HISTORICAL COLLECTOR]--- Alle {} kamper for liga {} fantes allerede i databasen.", fixturesArray.size(), leagueId);
                }

                log.info("---[HISTORICAL COLLECTOR]--- Starter innsamling av manglende kampstatistikk og skadedata...", fixturesArray.size());

                for (JsonNode fixtureNode : fixturesArray) {
                    Long fixtureId = fixtureNode.path("fixture").path("id").asLong();

                    String status = fixtureNode.path("fixture").path("status").path("short").asText();
                    if (!("FT".equals(status) || "AET".equals(status) || "PEN".equals(status))) {
                        continue;
                    }

                    if (!matchStatisticsRepository.findByFixtureIdAndTeamId(fixtureId, fixtureNode.path("teams").path("home").path("id").asInt()).isPresent()) {
                        try {
                            ResponseEntity<String> statsApiResponse = footballApiService.getStatisticsForFixture(fixtureId).block();
                            if (statsApiResponse != null && statsApiResponse.getBody() != null) {
                                JsonNode statsResponse = objectMapper.readTree(statsApiResponse.getBody()).path("response");
                                saveMatchStatistics(statsResponse, fixtureId);
                            }
                            Thread.sleep(1500);
                        } catch (Exception e) {
                            log.error("---[HISTORICAL COLLECTOR]--- Feil ved henting/prosessering av stats for fixture {}", fixtureId, e);
                        }
                    } else {
                        log.info("---[HISTORICAL COLLECTOR]--- Statistikk for fixture {} finnes allerede. Hopper over.", fixtureId);
                    }

                    try {
                        ResponseEntity<String> injuriesResponse = footballApiService.getInjuriesForFixture(fixtureId).block();
                        if(injuriesResponse != null && injuriesResponse.getBody() != null) {
                            JsonNode injuriesData = objectMapper.readTree(injuriesResponse.getBody()).path("response");
                            saveInjuryData(injuriesData, fixtureId);
                        }
                        Thread.sleep(1500);
                    } catch (Exception e) {
                        log.error("---[HISTORICAL COLLECTOR]--- Feil ved henting/prosessering av skadedata for fixture {}", fixtureId, e);
                    }
                }

                log.info("---[HISTORICAL COLLECTOR]--- Fullførte innsamling for liga {}.", leagueId);
                bot.setStatus(BotConfiguration.BotStatus.PAUSED);
                bot.setLastRun(Instant.now());
                botConfigRepository.save(bot);

            } catch (Exception e) {
                log.error("---[HISTORICAL COLLECTOR]--- Kritisk feil under innsamling for liga {}", leagueId, e);
            }
        }
    }

    @Transactional
    public void saveInjuryData(JsonNode injuriesResponse, Long fixtureId) {
        if (!injuriesResponse.isArray()) return;

        int newInjuriesCount = 0;
        for (JsonNode injuryNode : injuriesResponse) {
            Integer playerId = injuryNode.path("player").path("id").asInt();

            if (playerId == 0 || injuryRepository.existsByFixtureIdAndPlayerId(fixtureId, playerId)) {
                continue;
            }

            Injury injury = new Injury();
            injury.setFixtureId(fixtureId);
            injury.setPlayerId(playerId);
            injury.setPlayerName(injuryNode.path("player").path("name").asText());
            injury.setTeamId(injuryNode.path("team").path("id").asInt());
            injury.setLeagueId(injuryNode.path("league").path("id").asInt());
            injury.setSeason(injuryNode.path("league").path("season").asInt());
            injury.setType(injuryNode.path("player").path("type").asText());
            injury.setReason(injuryNode.path("player").path("reason").asText());

            injuryRepository.save(injury);
            newInjuriesCount++;
        }

        if (newInjuriesCount > 0) {
            log.info("---[HISTORICAL COLLECTOR]--- Lagret {} nye skadeoppføringer for fixture {}.", newInjuriesCount, fixtureId);
        }
    }

    @Transactional
    public boolean saveOrUpdateFixtureFromJson(JsonNode fixtureNode) {
        long fixtureId = fixtureNode.path("fixture").path("id").asLong();

        Optional<Fixture> existingFixtureOpt = fixtureRepository.findById(fixtureId);

        Fixture fixtureToSave;
        boolean needsUpdate = false;

        Integer goalsHome = fixtureNode.path("goals").path("home").isNull() ? null : fixtureNode.path("goals").path("home").asInt();
        Integer goalsAway = fixtureNode.path("goals").path("away").isNull() ? null : fixtureNode.path("goals").path("away").asInt();

        if (existingFixtureOpt.isPresent()) {
            fixtureToSave = existingFixtureOpt.get();
            if ((goalsHome != null && !goalsHome.equals(fixtureToSave.getGoalsHome())) ||
                    (goalsAway != null && !goalsAway.equals(fixtureToSave.getGoalsAway()))) {
                fixtureToSave.setGoalsHome(goalsHome);
                fixtureToSave.setGoalsAway(goalsAway);
                needsUpdate = true;
            }
        } else {
            fixtureToSave = new Fixture();
            fixtureToSave.setId(fixtureId);
            fixtureToSave.setLeagueId(fixtureNode.path("league").path("id").asInt());
            fixtureToSave.setSeason(fixtureNode.path("league").path("season").asInt());
            fixtureToSave.setDate(Instant.parse(fixtureNode.path("fixture").path("date").asText()));
            fixtureToSave.setStatus(fixtureNode.path("fixture").path("status").path("short").asText());
            fixtureToSave.setHomeTeamId(fixtureNode.path("teams").path("home").path("id").asInt());
            fixtureToSave.setHomeTeamName(fixtureNode.path("teams").path("home").path("name").asText());
            fixtureToSave.setAwayTeamId(fixtureNode.path("teams").path("away").path("id").asInt());
            fixtureToSave.setAwayTeamName(fixtureNode.path("teams").path("away").path("name").asText());
            fixtureToSave.setGoalsHome(goalsHome);
            fixtureToSave.setGoalsAway(goalsAway);
            needsUpdate = true;
        }

        if (needsUpdate) {
            fixtureRepository.save(fixtureToSave);
        }

        return needsUpdate;
    }

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

        if (sourceBot != null && sourceBot.getSourceType() == BotConfiguration.SourceType.SPORT_API) {
            sourceBot.setLastRun(Instant.now());
            botConfigRepository.save(sourceBot);
            log.info("Oppdatert lastRun for enkelt-lag-bot '{}'", sourceBot.getName());
        }
    }

    @Transactional
    public void saveMatchStatistics(JsonNode statsResponse, Long fixtureId) {
        if (!statsResponse.isArray()) return;

        for (JsonNode teamStatsNode : statsResponse) {
            Integer teamId = teamStatsNode.path("team").path("id").asInt();

            if (matchStatisticsRepository.findByFixtureIdAndTeamId(fixtureId, teamId).isPresent()) {
                continue;
            }

            MatchStatistics matchStats = new MatchStatistics();
            matchStats.setFixtureId(fixtureId);
            matchStats.setTeamId(teamId);

            for (JsonNode stat : teamStatsNode.path("statistics")) {
                String type = stat.path("type").asText();
                JsonNode valueNode = stat.path("value");

                if (valueNode.isNull()) continue;

                switch (type) {
                    case "Shots on Goal": matchStats.setShotsOnGoal(valueNode.asInt()); break;
                    case "Shots off Goal": matchStats.setShotsOffGoal(valueNode.asInt()); break;
                    case "Total Shots": matchStats.setTotalShots(valueNode.asInt()); break;
                    case "Blocked Shots": matchStats.setBlockedShots(valueNode.asInt()); break;
                    case "Shots insidebox": matchStats.setShotsInsideBox(valueNode.asInt()); break;
                    case "Shots outsidebox": matchStats.setShotsOutsideBox(valueNode.asInt()); break;
                    case "Fouls": matchStats.setFouls(valueNode.asInt()); break;
                    case "Corner Kicks": matchStats.setCornerKicks(valueNode.asInt()); break;
                    case "Offsides": matchStats.setOffsides(valueNode.asInt()); break;
                    case "Ball Possession": matchStats.setBallPossession(valueNode.asText()); break;
                    case "Yellow Cards": matchStats.setYellowCards(valueNode.asInt()); break;
                    case "Red Cards": matchStats.setRedCards(valueNode.asInt()); break;
                    case "Goalkeeper Saves": matchStats.setGoalkeeperSaves(valueNode.asInt()); break;
                    case "Total passes": matchStats.setTotalPasses(valueNode.asInt()); break;
                    case "Passes accurate": matchStats.setPassesAccurate(valueNode.asInt()); break;
                    case "Passes %": matchStats.setPassesPercentage(valueNode.asText()); break;
                }
            }
            matchStatisticsRepository.save(matchStats);
        }
        log.info("Lagret detaljert statistikk for fixture {}", fixtureId);
    }
}