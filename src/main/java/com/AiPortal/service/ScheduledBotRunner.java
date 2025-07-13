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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class ScheduledBotRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledBotRunner.class);
    private static final DateTimeFormatter TWITTER_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);

    // Reduserte avhengigheter til kun det som trengs direkte i denne klassen
    private final BotConfigurationRepository botConfigRepository;
    private final PendingFixtureChunkRepository pendingChunkRepository;
    private final RawTweetDataRepository tweetRepository;
    private final TeamStatisticsRepository teamStatisticsRepository;
    private final MatchOddsRepository matchOddsRepository;
    private final BookmakerRepository bookmakerRepository;
    private final BetTypeRepository betTypeRepository;
    private final FixtureRepository fixtureRepository;

    private final BotConfigurationService botConfigService;
    private final TwitterServiceManager twitterServiceManager;
    private final FootballApiService footballApiService;
    private final ObjectMapper objectMapper;
    private final HistoricalDataWorker historicalDataWorker;

    @Autowired
    public ScheduledBotRunner(
            BotConfigurationRepository botConfigRepository,
            PendingFixtureChunkRepository pendingChunkRepository,
            RawTweetDataRepository tweetRepository,
            TeamStatisticsRepository teamStatisticsRepository,
            MatchOddsRepository matchOddsRepository,
            BookmakerRepository bookmakerRepository,
            BetTypeRepository betTypeRepository,
            FixtureRepository fixtureRepository,
            BotConfigurationService botConfigService,
            TwitterServiceManager twitterServiceManager,
            FootballApiService footballApiService,
            ObjectMapper objectMapper,
            @Lazy HistoricalDataWorker historicalDataWorker
    ) {
        this.botConfigRepository = botConfigRepository;
        this.pendingChunkRepository = pendingChunkRepository;
        this.tweetRepository = tweetRepository;
        this.teamStatisticsRepository = teamStatisticsRepository;
        this.matchOddsRepository = matchOddsRepository;
        this.bookmakerRepository = bookmakerRepository;
        this.betTypeRepository = betTypeRepository;
        this.fixtureRepository = fixtureRepository;
        this.botConfigService = botConfigService;
        this.twitterServiceManager = twitterServiceManager;
        this.footballApiService = footballApiService;
        this.objectMapper = objectMapper;
        this.historicalDataWorker = historicalDataWorker;
    }

    private <T> List<List<T>> partitionList(List<T> list, final int size) {
        return new ArrayList<>(IntStream.range(0, list.size()).boxed().collect(Collectors.groupingBy(e -> e / size, Collectors.mapping(list::get, Collectors.toList()))).values());
    }

    @Transactional
    public void runHistoricalDataCollector() {
        log.info("---[PRODUCER] Forbereder historisk datainnsamling.---");
        List<BotConfiguration> historicalBots = botConfigService.getAllBotsByStatusAndType(BotConfiguration.BotStatus.ACTIVE, BotConfiguration.SourceType.HISTORICAL_FIXTURE_DATA);
        if (historicalBots.isEmpty()) {
            log.info("---[PRODUCER]--- Ingen aktive HISTORICAL_FIXTURE_DATA-boter funnet.");
            return;
        }
        for (BotConfiguration bot : historicalBots) {
            String sourceId = bot.getSourceIdentifier();
            log.info("---[PRODUCER] Starter forberedelser for {}.", sourceId);
            try {
                ResponseEntity<String> fixturesListResponse = footballApiService.getFixturesByLeagueAndSeason(sourceId.split(":")[0], sourceId.split(":")[1]).block();
                if (fixturesListResponse == null || !fixturesListResponse.getStatusCode().is2xxSuccessful() || fixturesListResponse.getBody() == null) {
                    log.error("---[PRODUCER]--- Kunne ikke hente kampliste for {}. Hopper over.", sourceId);
                    continue;
                }
                JsonNode fixturesArray = objectMapper.readTree(fixturesListResponse.getBody()).path("response");
                List<Long> allFixtureIds = new ArrayList<>();
                fixturesArray.forEach(node -> allFixtureIds.add(node.path("fixture").path("id").asLong()));
                if (allFixtureIds.isEmpty()) {
                    log.warn("---[PRODUCER]--- Fant ingen kamper for {}.", sourceId);
                    continue;
                }
                List<List<Long>> fixtureIdChunks = partitionList(allFixtureIds, 20);
                for (List<Long> chunk : fixtureIdChunks) {
                    PendingFixtureChunk pendingChunk = new PendingFixtureChunk();
                    pendingChunk.setFixtureIds(chunk.stream().map(String::valueOf).collect(Collectors.joining("-")));
                    pendingChunk.setSourceIdentifier(sourceId);
                    pendingChunk.setStatus(PendingFixtureChunk.ChunkStatus.PENDING);
                    pendingChunkRepository.save(pendingChunk);
                }
                log.info("---[PRODUCER]--- Opprettet {} chunks for {} som nå ligger i kø.", fixtureIdChunks.size(), sourceId);
                bot.setStatus(BotConfiguration.BotStatus.PAUSED);
                bot.setLastRun(Instant.now());
                botConfigRepository.save(bot);
            } catch (Exception e) {
                log.error("---[PRODUCER]--- Kritisk feil under forberedelse for {}: {}", sourceId, e.getMessage());
            }
        }
    }

    @Async("taskExecutor")
    @Scheduled(fixedRate = 2000, initialDelay = 5000)
    public void processNextFixtureChunk() {
        Optional<PendingFixtureChunk> chunkOpt;
        synchronized (this) {
            chunkOpt = findAndLockNextChunk();
        }
        if (chunkOpt.isEmpty()) {
            return;
        }
        PendingFixtureChunk chunk = chunkOpt.get();
        historicalDataWorker.processChunk(chunk);
        chunk.setProcessedAt(Instant.now());
        pendingChunkRepository.save(chunk);
        log.info("---[CONSUMER] Fullførte prosessering av chunk ID: {} med status: {}", chunk.getId(), chunk.getStatus());
    }

    @Transactional
    public Optional<PendingFixtureChunk> findAndLockNextChunk() {
        Optional<PendingFixtureChunk> chunkOpt = pendingChunkRepository.findFirstByStatusOrderByCreatedAtAsc(PendingFixtureChunk.ChunkStatus.PENDING);
        chunkOpt.ifPresent(chunk -> {
            chunk.setStatus(PendingFixtureChunk.ChunkStatus.PROCESSING);
            chunk.setAttemptCount(chunk.getAttemptCount() + 1);
            pendingChunkRepository.save(chunk);
        });
        return chunkOpt;
    }

    @Scheduled(fixedRate = 960000, initialDelay = 60000)
    @Transactional
    public void runTwitterSearchBot() {
        log.info("--- Starter planlagt Twitter-søk ---");
        List<BotConfiguration> activeTwitterBots = botConfigService.getAllBotsByStatusAndType(BotConfiguration.BotStatus.ACTIVE, BotConfiguration.SourceType.TWITTER);
        if (activeTwitterBots.isEmpty()) return;

        for (BotConfiguration bot : activeTwitterBots) {
            TwitterServiceProvider twitterProvider = twitterServiceManager.getNextProvider();
            if (twitterProvider == null) {
                log.warn("Ingen flere Twitter-leverandører tilgjengelig.");
                break;
            }
            String username = bot.getSourceIdentifier();
            String query = "from:" + username;
            if ("TwttrAPI241".equals(twitterProvider.getProviderName()) || "TwitterAPI45".equals(twitterProvider.getProviderName())) {
                query = username;
            }
            twitterProvider.searchRecentTweets(query, null).subscribe(responseBody -> {
                List<JsonNode> tweets = twitterProvider.parseTweetsFromResponse(responseBody);
                int newTweetsCount = 0;
                for (JsonNode tweet : tweets) {
                    String tweetId = "TwttrAPI241".equals(twitterProvider.getProviderName()) ? tweet.path("rest_id").asText() : tweet.path("id_str").asText(tweet.path("id").asText());
                    if (tweetId.isEmpty() || tweetRepository.existsByTweetId(tweetId)) continue;
                    RawTweetData newTweetData = new RawTweetData();
                    newTweetData.setTweetId(tweetId);
                    newTweetData.setAuthorUsername(username);
                    newTweetData.setContent("TwttrAPI241".equals(twitterProvider.getProviderName()) ? tweet.path("legacy").path("full_text").asText() : tweet.path("text").asText(tweet.path("full_text").asText()));
                    String createdAtStr = tweet.path("legacy").path("created_at").asText(tweet.path("created_at").asText());
                    try {
                        newTweetData.setTweetedAt(Instant.from(TWITTER_DATE_FORMATTER.parse(createdAtStr)));
                    } catch (DateTimeParseException e) {
                        try {
                            newTweetData.setTweetedAt(Instant.parse(createdAtStr));
                        } catch (Exception e2) {
                            newTweetData.setTweetedAt(Instant.now());
                        }
                    }
                    newTweetData.setSourceBot(bot);
                    tweetRepository.save(newTweetData);
                    newTweetsCount++;
                }
                if (newTweetsCount > 0) log.info("Lagret {} nye tweets for bot '{}'", newTweetsCount, bot.getName());
                bot.setLastRun(Instant.now());
                botConfigRepository.save(bot);
            }, error -> log.error("Feil for bot '{}' med leverandør '{}': {}", bot.getName(), twitterProvider.getProviderName(), error.getMessage()));
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
        log.info("--- Starter metadata-oppdatering ---");
        footballApiService.getBookmakers().subscribe(responseEntity -> {
            try {
                if (responseEntity.getBody() == null) return;
                JsonNode responses = objectMapper.readTree(responseEntity.getBody()).path("response");
                if (responses.isArray()) {
                    for (JsonNode bookmakerNode : responses) {
                        Bookmaker b = new Bookmaker();
                        b.setId(bookmakerNode.path("id").asInt());
                        b.setName(bookmakerNode.path("name").asText());
                        bookmakerRepository.save(b);
                    }
                }
            } catch (Exception e) { log.error("Feil ved parsing av bookmakere", e); }
        });
        footballApiService.getBetTypes().subscribe(responseEntity -> {
            try {
                if (responseEntity.getBody() == null) return;
                JsonNode responses = objectMapper.readTree(responseEntity.getBody()).path("response");
                if (responses.isArray()) {
                    for (JsonNode betNode : responses) {
                        BetType bt = new BetType();
                        bt.setId(betNode.path("id").asInt());
                        bt.setName(betNode.path("name").asText());
                        betTypeRepository.save(bt);
                    }
                }
            } catch (Exception e) { log.error("Feil ved parsing av spilltyper", e); }
        });
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "Europe/Oslo")
    @Transactional
    public void fetchDailyOdds() {
        List<String> datesToFetch = IntStream.range(0, 7).mapToObj(i -> LocalDate.now().plusDays(i).toString()).collect(Collectors.toList());
        log.info("--- Henter odds for de neste 7 dagene: {} ---", datesToFetch);
        for (String date : datesToFetch) {
            footballApiService.getOddsByDate(date).flatMap(responseEntity -> {
                try {
                    if (!responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) return Mono.empty();
                    JsonNode root = objectMapper.readTree(responseEntity.getBody());
                    if (root.has("errors") && !root.path("errors").isEmpty()) return Mono.empty();
                    JsonNode responses = root.path("response");
                    if (!responses.isArray() || responses.isEmpty()) return Mono.empty();
                    return Flux.fromIterable(responses).flatMap(this::processSingleFixtureWithOdds).collectList();
                } catch (Exception e) {
                    return Mono.error(e);
                }
            }).timeout(Duration.ofMinutes(5)).subscribe(
                    processedFixtures -> {
                        if (processedFixtures != null && !processedFixtures.isEmpty()) {
                            log.info("Fullførte prosessering av odds for {} kamper for dato: {}", processedFixtures.size(), date);
                        }
                    },
                    error -> log.error("Feil i odds-strøm for dato {}.", date, error)
            );
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Odds-innhenting ble avbrutt.");
                break;
            }
        }
    }

    private Mono<Fixture> processSingleFixtureWithOdds(JsonNode oddsResponse) {
        long fixtureId = oddsResponse.path("fixture").path("id").asLong();
        boolean hasTeamData = oddsResponse.path("teams").path("home").has("id") && oddsResponse.path("teams").path("away").has("id");
        if (hasTeamData) {
            return Mono.fromCallable(() -> saveFixtureAndOdds(oddsResponse)).subscribeOn(Schedulers.boundedElastic()).onErrorResume(e -> Mono.empty());
        }
        return Mono.empty();
    }

    @Transactional
    public Fixture saveFixtureAndOdds(JsonNode oddsData) {
        long fixtureId = oddsData.path("fixture").path("id").asLong();
        Fixture fixture = fixtureRepository.findById(fixtureId).orElse(new Fixture());
        fixture.setId(fixtureId);
        fixture.setLeagueId(oddsData.path("league").path("id").asInt());
        fixture.setSeason(oddsData.path("league").path("season").asInt());
        fixture.setDate(Instant.parse(oddsData.path("fixture").path("date").asText()));
        fixture.setStatus(oddsData.path("fixture").path("status").path("short").asText());
        fixture.setHomeTeamId(oddsData.path("teams").path("home").path("id").asInt());
        fixture.setHomeTeamName(oddsData.path("teams").path("home").path("name").asText());
        fixture.setAwayTeamId(oddsData.path("teams").path("away").path("id").asInt());
        fixture.setAwayTeamName(oddsData.path("teams").path("away").path("name").asText());
        JsonNode goalsNode = oddsData.path("goals");
        if (!goalsNode.path("home").isNull()) fixture.setGoalsHome(goalsNode.path("home").asInt());
        if (!goalsNode.path("away").isNull()) fixture.setGoalsAway(goalsNode.path("away").asInt());
        Fixture savedFixture = fixtureRepository.save(fixture);
        JsonNode bookmakers = oddsData.path("bookmakers");
        if (bookmakers.isArray()) {
            for (JsonNode bookmakerNode : bookmakers) {
                int bookmakerId = bookmakerNode.path("id").asInt();
                for (JsonNode betNode : bookmakerNode.path("bets")) {
                    if (betNode.path("id").asInt() == 1) {
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

    public void runLeagueStatsCollector() {
        log.warn("--- runLeagueStatsCollector er deaktivert. Jobben er erstattet av runHistoricalDataCollector. Ingen handling utført. ---");
    }

    @Scheduled(fixedRate = 600000, initialDelay = 120000)
    @Transactional
    public void runSportDataBots() {
        log.info("--- Starter sportsdata-boter (enkelt-lag) ---");
        List<BotConfiguration> activeSportBots = botConfigService.getAllBotsByStatusAndType(BotConfiguration.BotStatus.ACTIVE, BotConfiguration.SourceType.SPORT_API);
        if (activeSportBots.isEmpty()) {
            return;
        }
        for (BotConfiguration bot : activeSportBots) {
            log.info("Kjører SPORT_API-bot: '{}'", bot.getName());
            String[] params = bot.getSourceIdentifier().split(":");
            if (params.length != 3) {
                log.error("Ugyldig sourceIdentifier for sport-bot {}.", bot.getId());
                continue;
            }
            footballApiService.getTeamStatistics(params[0], params[1], params[2]).subscribe(responseEntity -> {
                try {
                    if(responseEntity.getBody() != null) {
                        saveTeamStatistics(objectMapper.readTree(responseEntity.getBody()).path("response"), bot);
                    }
                } catch (Exception e) {
                    log.error("Feil ved parsing av sportsdata for bot '{}'", bot.getName(), e);
                }
            }, error -> log.error("Feil ved henting av sportsdata for bot '{}'", bot.getName(), error));
        }
    }

    @Transactional
    public void saveTeamStatistics(JsonNode statsResponse, BotConfiguration sourceBot) {
        if (statsResponse.isMissingNode() || !statsResponse.isObject() || statsResponse.size() == 0) return;
        int teamId = statsResponse.path("team").path("id").asInt();
        int leagueId = statsResponse.path("league").path("id").asInt();
        int season = statsResponse.path("league").path("season").asInt();
        TeamStatistics stats = teamStatisticsRepository.findByLeagueIdAndSeasonAndTeamId(leagueId, season, teamId).orElse(new TeamStatistics());
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
        teamStatisticsRepository.save(stats);
        if (sourceBot != null && sourceBot.getSourceType() == BotConfiguration.SourceType.SPORT_API) {
            sourceBot.setLastRun(Instant.now());
            botConfigRepository.save(sourceBot);
        }
    }
}