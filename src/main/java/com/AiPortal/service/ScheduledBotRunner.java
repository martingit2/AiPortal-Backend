// src/main/java/com/AiPortal/service/ScheduledBotRunner.java
package com.AiPortal.service;

import com.AiPortal.entity.*;
import com.AiPortal.repository.*;
import com.AiPortal.service.twitter.TwitterServiceProvider;
import com.AiPortal.service.twitter.TwitterServiceManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class ScheduledBotRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledBotRunner.class);
    private static final DateTimeFormatter TWITTER_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
    private final ReentrantLock chunkProcessingLock = new ReentrantLock();

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
    private final PinnacleApiService pinnacleApiService;
    private final ObjectMapper objectMapper;
    private final HistoricalDataWorker historicalDataWorker;

    @Autowired
    public ScheduledBotRunner(BotConfigurationRepository botConfigRepository, PendingFixtureChunkRepository pendingChunkRepository, RawTweetDataRepository tweetRepository, TeamStatisticsRepository teamStatisticsRepository, MatchOddsRepository matchOddsRepository, BookmakerRepository bookmakerRepository, BetTypeRepository betTypeRepository, FixtureRepository fixtureRepository, BotConfigurationService botConfigService, TwitterServiceManager twitterServiceManager, FootballApiService footballApiService, PinnacleApiService pinnacleApiService, ObjectMapper objectMapper, @Lazy HistoricalDataWorker historicalDataWorker) {
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
        this.pinnacleApiService = pinnacleApiService;
        this.objectMapper = objectMapper;
        this.historicalDataWorker = historicalDataWorker;
    }

    @Transactional
    public int cleanupIncompleteFixtures() {
        List<Fixture> incompleteFixtures = fixtureRepository.findByHomeTeamIdIsNullAndAwayTeamIdIsNull();
        if (!incompleteFixtures.isEmpty()) {
            fixtureRepository.deleteAll(incompleteFixtures);
            log.info("Slettet {} ufullstendige fixture-rader.", incompleteFixtures.size());
            return incompleteFixtures.size();
        }
        log.info("Fant ingen ufullstendige fixtures å slette.");
        return 0;
    }

    private Optional<Fixture> findExistingFixtureFromPinnacleEvent(JsonNode event) {
        String homeTeamRaw = event.path("home").asText();
        String awayTeamRaw = event.path("away").asText();
        String startsString = event.path("starts").asText();
        if (homeTeamRaw.isEmpty() || awayTeamRaw.isEmpty() || startsString.isEmpty()) {
            return Optional.empty();
        }
        String normalizedHome = normalizeTeamName(homeTeamRaw);
        String normalizedAway = normalizeTeamName(awayTeamRaw);
        Instant eventTime = Instant.parse(startsString + "Z");

        log.trace("---[PINNACLE MATCHER] Leter etter kamp: {} ({}) vs {} ({}) rundt {}",
                homeTeamRaw, normalizedHome, awayTeamRaw, normalizedAway, eventTime);

        List<Fixture> candidates = fixtureRepository.findAllByDateBetween(
                eventTime.minus(12, ChronoUnit.HOURS),
                eventTime.plus(12, ChronoUnit.HOURS)
        );

        Optional<Fixture> foundFixture = candidates.stream()
                .filter(fixture ->
                        normalizeTeamName(fixture.getHomeTeamName()).equals(normalizedHome) &&
                                normalizeTeamName(fixture.getAwayTeamName()).equals(normalizedAway)
                )
                .findFirst();

        if (foundFixture.isPresent()) {
            log.info("---[PINNACLE MATCHER] Fant match! Pinnacle: '{} vs {}' -> DB Fixture ID: {}", homeTeamRaw, awayTeamRaw, foundFixture.get().getId());
        } else {
            log.warn("---[PINNACLE MATCHER] Fant IKKE match for Pinnacle-kamp: '{} vs {}'", homeTeamRaw, awayTeamRaw);
        }
        return foundFixture;
    }

    // ... (og alle de andre metodene, som fetchPinnacleOdds, etc.)
    // La meg lime inn resten for å være 100% sikker ...
    private <T> List<List<T>> partitionList(List<T> list, final int size) {
        return new ArrayList<>(IntStream.range(0, list.size()).boxed().collect(Collectors.groupingBy(e -> e / size, Collectors.mapping(list::get, Collectors.toList()))).values());
    }
    private String normalizeTeamName(String name) {
        if (name == null) return "";
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        return normalized.replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase();
    }
    @Async("taskExecutor")
    @Scheduled(fixedRate = 300000, initialDelay = 30000)
    public void fetchPinnacleOdds() {
        log.info("---[PINNACLE V5] Starter Pinnacle Odds-innhenting.---");
        List<BotConfiguration> pinnacleBots = botConfigService.getAllBotsByStatusAndType(
                BotConfiguration.BotStatus.ACTIVE,
                BotConfiguration.SourceType.PINNACLE_ODDS
        );
        if (pinnacleBots.isEmpty()) return;
        for (BotConfiguration bot : pinnacleBots) {
            String sportId = bot.getSourceIdentifier();
            Long sinceTimestamp = bot.getSinceTimestamp();
            log.info("---[PINNACLE V5] Kjører bot '{}' for sportId {}. Siste timestamp: {}", bot.getName(), sportId, sinceTimestamp);
            try {
                ResponseEntity<String> marketsResponse = pinnacleApiService.getMarkets(sportId, sinceTimestamp).block();
                processPinnacleResponse(marketsResponse, bot, this::processPinnacleMarketEvent);
                Thread.sleep(1000);
                ResponseEntity<String> specialsResponse = pinnacleApiService.getSpecialMarkets(sportId, sinceTimestamp).block();
                processPinnacleResponse(specialsResponse, bot, this::processPinnacleSpecialEvent);
            } catch (Exception e) {
                log.error("---[PINNACLE V5] API-kall feilet for bot {}: {}", bot.getName(), e.getMessage());
            }
        }
    }
    private void processPinnacleResponse(ResponseEntity<String> responseEntity, BotConfiguration bot, java.util.function.Consumer<JsonNode> eventProcessor) {
        try {
            if (responseEntity == null || !responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
                log.warn("---[PINNACLE V5]--- Mottok ugyldig svar fra Pinnacle API.");
                return;
            }
            JsonNode root = objectMapper.readTree(responseEntity.getBody());
            JsonNode events = root.path(root.has("events") ? "events" : "specials");
            if (events.isArray() && events.size() > 0) {
                for (JsonNode event : events) {
                    eventProcessor.accept(event);
                }
            }
            long newSince = root.path("last").asLong();
            if (newSince > 0) {
                bot.setSinceTimestamp(newSince);
                bot.setLastRun(Instant.now());
                botConfigRepository.save(bot);
                log.info("---[PINNACLE V5] Oppdaterte 'since' timestamp til: {}", newSince);
            }
        } catch (Exception e) {
            log.error("---[PINNACLE V5] Feil ved parsing av Pinnacle-respons: {}", e.getMessage(), e);
        }
    }
    @Transactional
    public void processPinnacleMarketEvent(JsonNode event) {
        findExistingFixtureFromPinnacleEvent(event).ifPresent(fixture -> {
            Integer pinnacleBookmakerId = 4;
            JsonNode periods = event.path("periods");
            Iterator<Map.Entry<String, JsonNode>> periodIterator = periods.fields();
            while (periodIterator.hasNext()) {
                JsonNode period = periodIterator.next().getValue();
                if (period.has("money_line")) savePinnacleOdds(fixture, pinnacleBookmakerId, "Match Winner", period.path("money_line"), "moneyline");
                if (period.has("spreads")) savePinnacleOdds(fixture, pinnacleBookmakerId, "Handicap", period.path("spreads"), "spread");
                if (period.has("totals")) savePinnacleOdds(fixture, pinnacleBookmakerId, "Total Goals", period.path("totals"), "total");
            }
        });
    }
    @Transactional
    public void processPinnacleSpecialEvent(JsonNode special) {
        if (!special.has("event") || special.get("event").isNull()) return;
        findExistingFixtureFromPinnacleEvent(special.path("event")).ifPresent(fixture -> {
            Integer pinnacleBookmakerId = 4;
            String betName = special.path("name").asText("Spesialspill");
            JsonNode lines = special.path("lines");
            savePinnacleOdds(fixture, pinnacleBookmakerId, betName, lines, "special");
        });
    }
    private void savePinnacleOdds(Fixture fixture, Integer bookmakerId, String betName, JsonNode oddsNode, String type) {
        if (oddsNode.isMissingNode() || oddsNode.isEmpty() || matchOddsRepository.existsByFixtureIdAndBookmakerIdAndBetName(fixture.getId(), bookmakerId, betName)) {
            return;
        }
        try {
            String oddsDataJson = convertPinnacleNodeToJsonString(oddsNode, type);
            MatchOdds matchOdds = new MatchOdds();
            matchOdds.setFixture(fixture);
            bookmakerRepository.findById(bookmakerId).ifPresent(matchOdds::setBookmaker);
            matchOdds.setBetName(betName);
            matchOdds.setOddsData(oddsDataJson);
            matchOdds.setLastUpdated(Instant.now());
            matchOddsRepository.save(matchOdds);
        } catch (JsonProcessingException e) {
            log.error("Kunne ikke konvertere odds-data til JSON for kamp {}", fixture.getId(), e);
        }
    }
    private String convertPinnacleNodeToJsonString(JsonNode oddsNode, String type) throws JsonProcessingException {
        ArrayNode valuesArray = objectMapper.createArrayNode();
        if ("moneyline".equals(type)) {
            valuesArray.add(objectMapper.createObjectNode().put("name", "Home").put("odds", oddsNode.path("home").asDouble()));
            valuesArray.add(objectMapper.createObjectNode().put("name", "Draw").put("odds", oddsNode.path("draw").asDouble()));
            valuesArray.add(objectMapper.createObjectNode().put("name", "Away").put("odds", oddsNode.path("away").asDouble()));
        } else {
            Iterator<Map.Entry<String, JsonNode>> fields = oddsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode line = entry.getValue();
                ObjectNode valueNode = objectMapper.createObjectNode();
                if (line.has("name")) valueNode.put("name", line.path("name").asText());
                switch (type) {
                    case "spread":
                        valueNode.put("name", "Home");
                        valueNode.put("handicap", line.path("hdp").asText());
                        valueNode.put("odds", line.path("home").asDouble());
                        valuesArray.add(valueNode.deepCopy());
                        valueNode.put("name", "Away");
                        valueNode.put("odds", line.path("away").asDouble());
                        valuesArray.add(valueNode);
                        break;
                    case "total":
                        valueNode.put("name", "Over");
                        valueNode.put("points", line.path("points").asText());
                        valueNode.put("odds", line.path("over").asDouble());
                        valuesArray.add(valueNode.deepCopy());
                        valueNode.put("name", "Under");
                        valueNode.put("odds", line.path("under").asDouble());
                        valuesArray.add(valueNode);
                        break;
                    case "special":
                        valueNode.put("name", line.path("name").asText());
                        valueNode.put("odds", line.path("price").asDouble());
                        if (line.has("handicap") && !line.get("handicap").isNull()) {
                            valueNode.put("handicap", line.path("handicap").asText());
                        }
                        valuesArray.add(valueNode);
                        break;
                }
            }
        }
        return objectMapper.writeValueAsString(valuesArray);
    }
    @Transactional
    public void runHistoricalDataCollector() {
        log.info("---[PRODUCER V2] Forbereder historisk datainnsamling.---");
        List<BotConfiguration> historicalBots = botConfigService.getAllBotsByStatusAndType(BotConfiguration.BotStatus.ACTIVE, BotConfiguration.SourceType.HISTORICAL_FIXTURE_DATA);
        if (historicalBots.isEmpty()) return;
        for (BotConfiguration bot : historicalBots) {
            String sourceId = bot.getSourceIdentifier();
            if (pendingChunkRepository.existsBySourceIdentifierAndStatusIn(sourceId, List.of(PendingFixtureChunk.ChunkStatus.PENDING, PendingFixtureChunk.ChunkStatus.PROCESSING))) {
                log.warn("---[PRODUCER V2]--- Innsamling for {} er allerede i kø eller pågår. Starter ikke ny jobb.", sourceId);
                continue;
            }
            log.info("---[PRODUCER V2] Starter forberedelser for {}.", sourceId);
            try {
                ResponseEntity<String> fixturesListResponse = footballApiService.getFixturesByLeagueAndSeason(sourceId.split(":")[0], sourceId.split(":")[1]).block();
                if (fixturesListResponse == null || !fixturesListResponse.getStatusCode().is2xxSuccessful() || fixturesListResponse.getBody() == null) {
                    log.error("---[PRODUCER V2]--- Kunne ikke hente kampliste for {}. Hopper over.", sourceId);
                    continue;
                }
                JsonNode fixturesArray = objectMapper.readTree(fixturesListResponse.getBody()).path("response");
                List<Long> allFixtureIds = new ArrayList<>();
                fixturesArray.forEach(node -> allFixtureIds.add(node.path("fixture").path("id").asLong()));
                if (allFixtureIds.isEmpty()) {
                    log.warn("---[PRODUCER V2]--- Fant ingen kamper for {}.", sourceId);
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
                log.info("---[PRODUCER V2]--- Opprettet {} chunks for {} som nå ligger i kø.", fixtureIdChunks.size(), sourceId);
                bot.setStatus(BotConfiguration.BotStatus.PAUSED);
                bot.setLastRun(Instant.now());
                botConfigRepository.save(bot);
            } catch (Exception e) {
                log.error("---[PRODUCER V2]--- Kritisk feil under forberedelse for {}: {}", sourceId, e.getMessage());
            }
        }
    }
    @Async("taskExecutor")
    @Scheduled(fixedRate = 2000, initialDelay = 5000)
    public void processNextFixtureChunk() {
        if (!chunkProcessingLock.tryLock()) {
            return;
        }
        try {
            Optional<PendingFixtureChunk> chunkOpt = findAndLockNextChunk();
            if (chunkOpt.isEmpty()) {
                return;
            }
            PendingFixtureChunk chunk = chunkOpt.get();
            historicalDataWorker.processChunk(chunk);
            chunk.setProcessedAt(Instant.now());
            pendingChunkRepository.save(chunk);
            log.info("---[CONSUMER V2] Fullførte prosessering av chunk ID: {} med status: {}", chunk.getId(), chunk.getStatus());
        } finally {
            chunkProcessingLock.unlock();
        }
    }
    @Transactional
    public Optional<PendingFixtureChunk> findAndLockNextChunk() {
        Instant timeout = Instant.now().minus(5, ChronoUnit.MINUTES);
        Optional<PendingFixtureChunk> stuckChunk = pendingChunkRepository.findFirstByStatusAndCreatedAtBefore(PendingFixtureChunk.ChunkStatus.PROCESSING, timeout);
        if (stuckChunk.isPresent()) {
            log.warn("Fant en 'stuck' jobb (ID: {}). Prøver på nytt.", stuckChunk.get().getId());
            stuckChunk.get().setAttemptCount(stuckChunk.get().getAttemptCount() + 1);
            return stuckChunk;
        }
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
                try {
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
                } catch(Exception e) {
                    log.error("Feil ved parsing av Twitter-respons for {}: {}", twitterProvider.getProviderName(), e.getMessage());
                }
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
                    for (JsonNode btNode : responses) {
                        BetType bt = new BetType();
                        bt.setId(btNode.path("id").asInt());
                        bt.setName(btNode.path("name").asText());
                        betTypeRepository.save(bt);
                    }
                }
            } catch (Exception e) { log.error("Feil ved parsing av spilltyper", e); }
        });
    }
    @Scheduled(cron = "0 0 1 * * *", zone = "Europe/Oslo")
    public void fetchDailyOdds() {
        List<String> datesToFetch = IntStream.range(0, 7).mapToObj(i -> LocalDate.now().plusDays(i).toString()).collect(Collectors.toList());
        log.info("--- [ROBUST] Henter odds for de neste 7 dagene: {} ---", datesToFetch);
        for (String date : datesToFetch) {
            log.info("--- [ROBUST] Henter odds for dato: {}", date);
            try {
                ResponseEntity<String> response = footballApiService.getOddsByDate(date).block(Duration.ofMinutes(1));
                if (response == null || !response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    log.warn("Mottok ikke gyldig svar for odds på dato {}", date);
                    continue;
                }
                JsonNode root = objectMapper.readTree(response.getBody());
                if(root.path("results").asInt() == 0) {
                    log.info("Ingen odds funnet for dato {}", date);
                    continue;
                }
                int savedCount = 0;
                for(JsonNode oddsResponse : root.path("response")) {
                    processSingleFixtureWithOdds(oddsResponse);
                    savedCount++;
                }
                if (savedCount > 0) {
                    log.info("--- [ROBUST] Fullførte prosessering av odds for {} kamper for dato: {}", savedCount, date);
                }
                Thread.sleep(3000);
            } catch (Exception e) {
                log.error("--- [ROBUST] Feil i odds-strøm for dato {}. Feilmelding: {}", date, e.getMessage());
            }
        }
    }
    @Transactional
    public void processSingleFixtureWithOdds(JsonNode oddsResponse) {
        long fixtureId = oddsResponse.path("fixture").path("id").asLong();
        if (fixtureId == 0) return;
        Fixture fixture = saveOrUpdateFixtureFromOddsResponse(oddsResponse);
        JsonNode bookmakers = oddsResponse.path("bookmakers");
        if (!bookmakers.isArray()) return;
        for (JsonNode bookmakerNode : bookmakers) {
            int bookmakerId = bookmakerNode.path("id").asInt();
            for (JsonNode betNode : bookmakerNode.path("bets")) {
                if (betNode.path("id").asInt() == 1) { // Match Winner
                    if (matchOddsRepository.existsByFixtureIdAndBookmakerIdAndBetName(fixture.getId(), bookmakerId, "Match Winner")) {
                        continue;
                    }
                    MatchOdds odds = new MatchOdds();
                    odds.setFixture(fixture);
                    bookmakerRepository.findById(bookmakerId).ifPresent(odds::setBookmaker);
                    odds.setBetName("Match Winner");
                    ArrayNode valuesArray = objectMapper.createArrayNode();
                    for (JsonNode value : betNode.path("values")) {
                        ObjectNode valueNode = objectMapper.createObjectNode();
                        valueNode.put("name", value.path("value").asText());
                        valueNode.put("odds", value.path("odd").asDouble());
                        valuesArray.add(valueNode);
                    }
                    try {
                        odds.setOddsData(objectMapper.writeValueAsString(valuesArray));
                    } catch (JsonProcessingException e) {
                        log.error("Kunne ikke lage JSON for odds", e);
                        continue;
                    }
                    odds.setLastUpdated(Instant.now());
                    matchOddsRepository.save(odds);
                    break;
                }
            }
        }
    }
    @Transactional
    public Fixture saveOrUpdateFixtureFromOddsResponse(JsonNode oddsResponse) {
        JsonNode fixtureNode = oddsResponse.path("fixture");
        long fixtureId = fixtureNode.path("id").asLong();
        Fixture fixture = fixtureRepository.findById(fixtureId).orElse(new Fixture());
        fixture.setId(fixtureId);
        fixture.setDate(Instant.parse(fixtureNode.path("date").asText()));
        fixture.setStatus(fixtureNode.path("status").path("short").asText());
        JsonNode leagueNode = oddsResponse.path("league");
        fixture.setLeagueId(leagueNode.path("id").asInt());
        fixture.setSeason(leagueNode.path("season").asInt());
        JsonNode teamsNode = oddsResponse.path("teams");
        fixture.setHomeTeamId(teamsNode.path("home").path("id").asInt());
        fixture.setHomeTeamName(teamsNode.path("home").path("name").asText());
        fixture.setAwayTeamId(teamsNode.path("away").path("id").asInt());
        fixture.setAwayTeamName(teamsNode.path("away").path("name").asText());
        return fixtureRepository.save(fixture);
    }
    @Async("taskExecutor")
    @Scheduled(cron = "0 15 3 * * *", zone = "Europe/Oslo")
    @Transactional
    public void runLeagueStatsCollector() {
        log.info("---[EFFEKTIV] Starter innsamling av komplette ligatabeller.---");
        List<BotConfiguration> leagueBots = botConfigService.getAllBotsByStatusAndType(
                BotConfiguration.BotStatus.ACTIVE,
                BotConfiguration.SourceType.LEAGUE_STATS
        );
        if (leagueBots.isEmpty()) return;
        for (BotConfiguration bot : leagueBots) {
            String[] params = bot.getSourceIdentifier().split(":");
            if (params.length != 2) {
                log.error("Ugyldig sourceIdentifier for LEAGUE_STATS-bot {}: {}.", bot.getId(), bot.getSourceIdentifier());
                continue;
            }
            String leagueId = params[0];
            String season = params[1];
            log.info("---[EFFEKTIV]--- Henter tabell for liga: {}, sesong: {}", leagueId, season);
            try {
                ResponseEntity<String> response = footballApiService.getStandings(leagueId, season).block();
                if (response == null || !response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    log.error("Kunne ikke hente tabell for liga {}-{}", leagueId, season);
                    continue;
                }
                JsonNode responseNode = objectMapper.readTree(response.getBody()).path("response");
                if (!responseNode.isArray() || responseNode.isEmpty()) {
                    log.warn("Fant ingen tabell-data for liga {}-{}", leagueId, season);
                    continue;
                }
                JsonNode standingsArray = responseNode.get(0).path("league").path("standings").get(0);
                if (standingsArray == null || !standingsArray.isArray()) {
                    continue;
                }
                String leagueName = responseNode.get(0).path("league").path("name").asText("Ukjent Liga");
                for (JsonNode teamStandingNode : standingsArray) {
                    saveTeamStatisticsFromStanding(teamStandingNode, bot, leagueName, Integer.parseInt(leagueId), Integer.parseInt(season));
                }
                bot.setLastRun(Instant.now());
                botConfigRepository.save(bot);
                log.info("---[EFFEKTIV]--- Fullførte oppdatering for {}.", bot.getName());
            } catch (Exception e) {
                log.error("---[EFFEKTIV]--- Kritisk feil under innsamling for ligatabell {}-{}: {}", leagueId, season, e.getMessage());
            }
        }
    }
    @Scheduled(fixedRate = 600000, initialDelay = 120000)
    @Transactional
    public void runSportDataBots() {
        log.info("--- Starter sportsdata-boter (enkelt-lag) ---");
        List<BotConfiguration> activeSportBots = botConfigService.getAllBotsByStatusAndType(BotConfiguration.BotStatus.ACTIVE, BotConfiguration.SourceType.SPORT_API);
        if (activeSportBots.isEmpty()) return;
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
                        saveTeamStatisticsFromSingleBot(objectMapper.readTree(responseEntity.getBody()).path("response"), bot);
                    }
                } catch (Exception e) {
                    log.error("Feil ved parsing av sportsdata for bot '{}'", bot.getName(), e);
                }
            }, error -> log.error("Feil ved henting av sportsdata for bot '{}'", bot.getName(), error));
        }
    }
    @Transactional
    public void saveTeamStatisticsFromStanding(JsonNode standingNode, BotConfiguration sourceBot, String leagueName, int leagueId, int season) {
        int teamId = standingNode.path("team").path("id").asInt();
        TeamStatistics stats = teamStatisticsRepository.findByLeagueIdAndSeasonAndTeamId(leagueId, season, teamId)
                .orElse(new TeamStatistics());
        stats.setTeamId(teamId);
        stats.setLeagueId(leagueId);
        stats.setSeason(season);
        stats.setTeamName(standingNode.path("team").path("name").asText());
        stats.setLeagueName(leagueName);
        JsonNode allStats = standingNode.path("all");
        stats.setPlayedTotal(allStats.path("played").asInt());
        stats.setWinsTotal(allStats.path("win").asInt());
        stats.setDrawsTotal(allStats.path("draw").asInt());
        stats.setLossesTotal(allStats.path("lose").asInt());
        stats.setGoalsForTotal(allStats.path("goals").path("for").asInt());
        stats.setGoalsAgainstTotal(allStats.path("goals").path("against").asInt());
        stats.setSourceBot(sourceBot);
        stats.setLastUpdated(Instant.now());
        teamStatisticsRepository.save(stats);
    }

    @Transactional
    public void saveTeamStatisticsFromSingleBot(JsonNode statsResponse, BotConfiguration sourceBot) {
        if (statsResponse.isMissingNode() || !statsResponse.isObject() || statsResponse.isEmpty()) return;
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
        if (sourceBot != null) {
            sourceBot.setLastRun(Instant.now());
            botConfigRepository.save(sourceBot);
        }
    }
}