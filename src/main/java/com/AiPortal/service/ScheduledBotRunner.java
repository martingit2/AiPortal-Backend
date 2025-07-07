package com.AiPortal.service;

import com.AiPortal.entity.*;
import com.AiPortal.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.AiPortal.entity.BetType;
import com.AiPortal.entity.Bookmaker;
import com.AiPortal.entity.Fixture;
import com.AiPortal.entity.MatchOdds;
import com.AiPortal.repository.BetTypeRepository;
import com.AiPortal.repository.BookmakerRepository;
import com.AiPortal.repository.FixtureRepository;
import com.AiPortal.repository.MatchOddsRepository;

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
    private final FootballApiService footballApiService;
    private final TeamStatisticsRepository statsRepository;
    private final FixtureRepository fixtureRepository;
    private final MatchOddsRepository matchOddsRepository;
    private final BookmakerRepository bookmakerRepository;
    private final BetTypeRepository betTypeRepository;
    private final ObjectMapper objectMapper;

    public ScheduledBotRunner(BotConfigurationService botConfigService,
                              BotConfigurationRepository botConfigRepository,
                              TwitterService twitterService,
                              RawTweetDataRepository tweetRepository,
                              TwitterQueryStateRepository queryStateRepository,
                              FootballApiService footballApiService,
                              TeamStatisticsRepository statsRepository,
                              FixtureRepository fixtureRepository,
                              MatchOddsRepository matchOddsRepository,
                              BookmakerRepository bookmakerRepository,
                              BetTypeRepository betTypeRepository,
                              ObjectMapper objectMapper) {
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

    // runTwitterSearchBot-metoden er uendret og kan stå her
    @Scheduled(fixedRate = 960000, initialDelay = 60000)
    @Transactional
    public void runTwitterSearchBot() {
        // ... Koden for denne metoden forblir som før ...
    }

    // runSportDataBots for statistikk er også uendret og kan stå her
    @Scheduled(fixedRate = 600000, initialDelay = 120000)
    @Transactional
    public void runSportDataBots() {
        // ... Koden for denne metoden forblir som før ...
    }

    /**
     * NY JOBB: Oppdaterer metadata som bookmakere og spilltyper.
     * Kjører én gang om dagen kl. 05:00.
     */
    @Scheduled(cron = "0 0 5 * * *", zone = "Europe/Oslo")
    @Transactional
    public void updateFootballMetadata() {
        log.info("--- Starter planlagt jobb for å oppdatere fotball-metadata (Bookmakere, Spilltyper) ---");

        // Hent og lagre Bookmakere
        footballApiService.getBookmakers().subscribe(json -> {
            try {
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

        // Hent og lagre Bet Types
        footballApiService.getBetTypes().subscribe(json -> {
            try {
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

    /**
     * NY JOBB: Henter daglig odds for morgendagens kamper.
     * Kjører kl. 01:00 hver natt.
     */
    @Scheduled(cron = "0 0 1 * * *", zone = "Europe/Oslo")
    @Transactional
    public void fetchDailyOdds() {
        String tomorrow = java.time.LocalDate.now().plusDays(1).toString();
        log.info("--- Starter planlagt jobb for å hente odds for dato: {} ---", tomorrow);

        footballApiService.getOddsByDate(tomorrow)
                .subscribe(responseJson -> {
                    try {
                        JsonNode root = objectMapper.readTree(responseJson);
                        JsonNode responses = root.path("response");

                        if (!responses.isArray() || responses.isEmpty()) {
                            log.info("Ingen odds funnet for dato: {}", tomorrow);
                            return;
                        }

                        for (JsonNode response : responses) {
                            JsonNode fixtureNode = response.path("fixture");
                            long fixtureId = fixtureNode.path("id").asLong();

                            // Opprett eller hent eksisterende Fixture
                            Fixture fixture = fixtureRepository.findById(fixtureId).orElse(new Fixture());
                            fixture.setId(fixtureId);
                            fixture.setLeagueId(response.path("league").path("id").asInt());
                            fixture.setSeason(response.path("league").path("season").asInt());
                            fixture.setDate(Instant.parse(fixtureNode.path("date").asText()));
                            fixture.setStatus(fixtureNode.path("status").path("short").asText());
                            fixture.setHomeTeamId(response.path("teams").path("home").path("id").asInt());
                            fixture.setHomeTeamName(response.path("teams").path("home").path("name").asText());
                            fixture.setAwayTeamId(response.path("teams").path("away").path("id").asInt());
                            fixture.setAwayTeamName(response.path("teams").path("away").path("name").asText());
                            Fixture savedFixture = fixtureRepository.save(fixture);

                            JsonNode bookmakers = response.path("bookmakers");
                            if (bookmakers.isArray()) {
                                for (JsonNode bookmakerNode : bookmakers) {
                                    int bookmakerId = bookmakerNode.path("id").asInt();

                                    for (JsonNode betNode : bookmakerNode.path("bets")) {
                                        if (betNode.path("id").asInt() == 1) { // 1 = Match Winner (H-U-B)

                                            // TODO: Sjekk om denne oddsen allerede finnes for å unngå duplikater

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
                        }
                        log.info("Fullførte lagring av odds for {} kamper for dato: {}", responses.size(), tomorrow);

                    } catch (Exception e) {
                        log.error("Feil ved parsing av odds-data", e);
                    }
                }, error -> {
                    log.error("Feil ved henting av odds-data: {}", error.getMessage());
                });
    }
}