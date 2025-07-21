// src/main/java/com/AiPortal/service/HistoricalDataWorker.java
package com.AiPortal.service;

import com.AiPortal.entity.*;
import com.AiPortal.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration; // <-- DEN MANGLENDE IMPORTEN ER NÅ LAGT TIL HER
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class HistoricalDataWorker {

    private static final Logger log = LoggerFactory.getLogger(HistoricalDataWorker.class);

    private final FootballApiService footballApiService;
    private final ObjectMapper objectMapper;
    private final PlayerRepository playerRepository;
    private final PlayerMatchStatisticsRepository playerMatchStatsRepository;
    private final FixtureRepository fixtureRepository;
    private final MatchStatisticsRepository matchStatsRepository;
    private final InjuryRepository injuryRepository;
    private final HeadToHeadStatsRepository h2hStatsRepository;

    @Autowired
    public HistoricalDataWorker(
            FootballApiService footballApiService,
            ObjectMapper objectMapper,
            PlayerRepository playerRepository,
            PlayerMatchStatisticsRepository playerMatchStatsRepository,
            FixtureRepository fixtureRepository,
            MatchStatisticsRepository matchStatsRepository,
            InjuryRepository injuryRepository,
            HeadToHeadStatsRepository h2hStatsRepository
    ) {
        this.footballApiService = footballApiService;
        this.objectMapper = objectMapper;
        this.playerRepository = playerRepository;
        this.playerMatchStatsRepository = playerMatchStatsRepository;
        this.fixtureRepository = fixtureRepository;
        this.matchStatsRepository = matchStatsRepository;
        this.injuryRepository = injuryRepository;
        this.h2hStatsRepository = h2hStatsRepository;
    }

    public void processChunk(PendingFixtureChunk chunk) {
        String idString = chunk.getFixtureIds();
        log.info("---[WORKER v3 Async] Starter prosessering av chunk ID: {} (Kilde: {}) ---", chunk.getId(), chunk.getSourceIdentifier());

        try {
            ResponseEntity<String> detailsResponse = footballApiService.getFixturesByIds(idString).block();
            if (detailsResponse != null && detailsResponse.getBody() != null) {
                JsonNode bulkFixtures = objectMapper.readTree(detailsResponse.getBody()).path("response");
                saveAllDataFromFixtures(bulkFixtures);
            }

            ResponseEntity<String> injuriesResponse = footballApiService.getInjuriesByIds(idString).block();
            if (injuriesResponse != null && injuriesResponse.getBody() != null) {
                JsonNode bulkInjuries = objectMapper.readTree(injuriesResponse.getBody()).path("response");
                saveAllInjuries(bulkInjuries);
            }

            List<Long> fixtureIdsInChunk = Arrays.stream(idString.split("-"))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            List<Fixture> fixturesInChunk = fixtureRepository.findAllById(fixtureIdsInChunk);
            saveHeadToHeadData(fixturesInChunk);

            chunk.setStatus(PendingFixtureChunk.ChunkStatus.COMPLETED);

        } catch (Exception e) {
            log.error("---[WORKER v3 Async] Feil under prosessering av chunk ID: {}. Feil: {}", chunk.getId(), e.getMessage(), e);
            chunk.setStatus(PendingFixtureChunk.ChunkStatus.FAILED);
            chunk.setLastErrorMessage(e.getMessage());
        }
    }

    @Transactional
    public void saveAllDataFromFixtures(JsonNode bulkFixtures) {
        List<Player> newPlayersToSave = new ArrayList<>();
        List<PlayerMatchStatistics> newPlayerStatsToSave = new ArrayList<>();
        List<MatchStatistics> newTeamStatsToSave = new ArrayList<>();
        List<Fixture> fixturesToSave = new ArrayList<>();

        List<Long> fixtureIdsInChunk = StreamSupport.stream(bulkFixtures.spliterator(), false)
                .map(node -> node.path("fixture").path("id").asLong())
                .collect(Collectors.toList());

        if (fixtureIdsInChunk.isEmpty()) return;

        Set<Integer> playerIdsInChunk = extractAllPlayerIds(bulkFixtures);

        Map<Long, Fixture> existingFixturesMap = fixtureRepository.findAllById(fixtureIdsInChunk).stream()
                .collect(Collectors.toMap(Fixture::getId, Function.identity()));

        Set<Integer> existingPlayerIds = new HashSet<>(playerRepository.findAllById(playerIdsInChunk)
                .stream().map(Player::getId).collect(Collectors.toSet()));

        Set<String> existingPlayerMatchKeys = playerMatchStatsRepository.findAllByFixtureIdIn(fixtureIdsInChunk)
                .stream().map(pms -> pms.getFixtureId() + ":" + pms.getPlayerId())
                .collect(Collectors.toSet());

        Set<String> existingTeamMatchKeys = matchStatsRepository.findAllByFixtureIdIn(fixtureIdsInChunk)
                .stream().map(ms -> ms.getFixtureId() + ":" + ms.getTeamId())
                .collect(Collectors.toSet());

        for (JsonNode fixtureNode : bulkFixtures) {
            fixturesToSave.add(createOrUpdateFixtureFromJson(fixtureNode, existingFixturesMap));

            for (JsonNode teamStatsNode : fixtureNode.path("statistics")) {
                long fixtureId = fixtureNode.path("fixture").path("id").asLong();
                int teamId = teamStatsNode.path("team").path("id").asInt();
                if (!existingTeamMatchKeys.contains(fixtureId + ":" + teamId)) {
                    newTeamStatsToSave.add(createMatchStatistics(teamStatsNode, fixtureId, teamId));
                    existingTeamMatchKeys.add(fixtureId + ":" + teamId);
                }
            }

            for (JsonNode teamPlayersNode : fixtureNode.path("players")) {
                int teamId = teamPlayersNode.path("team").path("id").asInt();
                for (JsonNode playerPerformanceNode : teamPlayersNode.path("players")) {
                    JsonNode playerInfoNode = playerPerformanceNode.path("player");
                    int playerId = playerInfoNode.path("id").asInt();
                    long fixtureId = fixtureNode.path("fixture").path("id").asLong();

                    if (playerId != 0 && !existingPlayerIds.contains(playerId)) {
                        newPlayersToSave.add(createPlayer(playerInfoNode));
                        existingPlayerIds.add(playerId);
                    }

                    if (playerId != 0 && !existingPlayerMatchKeys.contains(fixtureId + ":" + playerId)) {
                        JsonNode statsNode = playerPerformanceNode.path("statistics").get(0);
                        if (statsNode != null && !statsNode.isMissingNode()) {
                            newPlayerStatsToSave.add(createPlayerMatchStatistics(statsNode, fixtureId, teamId, playerId));
                            existingPlayerMatchKeys.add(fixtureId + ":" + playerId);
                        }
                    }
                }
            }
        }

        if (!fixturesToSave.isEmpty()) fixtureRepository.saveAll(fixturesToSave);
        if (!newPlayersToSave.isEmpty()) playerRepository.saveAll(newPlayersToSave);
        if (!newTeamStatsToSave.isEmpty()) matchStatsRepository.saveAll(newTeamStatsToSave);
        if (!newPlayerStatsToSave.isEmpty()) playerMatchStatsRepository.saveAll(newPlayerStatsToSave);
    }

    @Transactional
    public void saveAllInjuries(JsonNode bulkInjuries) {
        List<Injury> newInjuriesToSave = new ArrayList<>();
        for (JsonNode injuryNode : bulkInjuries) {
            long fixtureId = injuryNode.path("fixture").path("id").asLong();
            int playerId = injuryNode.path("player").path("id").asInt();
            if (playerId != 0 && !injuryRepository.existsByFixtureIdAndPlayerId(fixtureId, playerId)) {
                newInjuriesToSave.add(createInjury(injuryNode));
            }
        }
        if (!newInjuriesToSave.isEmpty()) injuryRepository.saveAll(newInjuriesToSave);
    }

    @Transactional
    public void saveHeadToHeadData(List<Fixture> fixtures) throws InterruptedException {
        log.info("Starter H2H-datainnsamling for {} kamper...", fixtures.size());
        for (Fixture fixture : fixtures) {
            if (h2hStatsRepository.existsByFixtureId(fixture.getId())) {
                continue;
            }

            String h2hQuery = fixture.getHomeTeamId() + "-" + fixture.getAwayTeamId();
            try {
                Thread.sleep(220);

                ResponseEntity<String> response = footballApiService.getHeadToHead(h2hQuery).block(Duration.ofSeconds(15));
                if (response != null && response.getBody() != null) {
                    JsonNode h2hFixtures = objectMapper.readTree(response.getBody()).path("response");
                    HeadToHeadStats h2hStats = parseAndCreateH2hStats(h2hFixtures, fixture);
                    h2hStatsRepository.save(h2hStats);
                    log.info("Lagret H2H-data for fixture ID: {}", fixture.getId());
                }
            } catch (Exception e) {
                log.warn("Kunne ikke hente eller lagre H2H for fixture {}: {}", fixture.getId(), e.getMessage());
            }
        }
        log.info("Fullførte H2H-datainnsamling.");
    }

    private HeadToHeadStats parseAndCreateH2hStats(JsonNode h2hFixtures, Fixture contextFixture) {
        HeadToHeadStats stats = new HeadToHeadStats();
        stats.setFixture(contextFixture);
        stats.setTeam1Id(contextFixture.getHomeTeamId());
        stats.setTeam2Id(contextFixture.getAwayTeamId());

        if (h2hFixtures == null || !h2hFixtures.isArray() || h2hFixtures.isEmpty()) {
            return stats;
        }

        int homeWins = 0, awayWins = 0, draws = 0, totalGoals = 0;

        for (JsonNode h2h : h2hFixtures) {
            JsonNode teams = h2h.path("teams");
            Integer h2hHomeId = teams.path("home").path("id").asInt();
            boolean isHomeWinner = teams.path("home").path("winner").asBoolean(false);
            boolean isAwayWinner = teams.path("away").path("winner").asBoolean(false);

            if (isHomeWinner) {
                if (h2hHomeId.equals(contextFixture.getHomeTeamId())) homeWins++; else awayWins++;
            } else if (isAwayWinner) {
                if (h2hHomeId.equals(contextFixture.getHomeTeamId())) awayWins++; else homeWins++;
            } else {
                draws++;
            }
            totalGoals += h2h.path("goals").path("home").asInt(0) + h2h.path("goals").path("away").asInt(0);
        }

        stats.setMatchesPlayed(h2hFixtures.size());
        stats.setTeam1Wins(homeWins);
        stats.setTeam2Wins(awayWins);
        stats.setDraws(draws);
        stats.setAvgTotalGoals(h2hFixtures.size() > 0 ? (double) totalGoals / h2hFixtures.size() : 0.0);

        return stats;
    }

    private Set<Integer> extractAllPlayerIds(JsonNode bulkFixtures) {
        Set<Integer> playerIds = new HashSet<>();
        for (JsonNode fixtureNode : bulkFixtures) {
            for (JsonNode teamPlayersNode : fixtureNode.path("players")) {
                for (JsonNode playerPerformanceNode : teamPlayersNode.path("players")) {
                    playerIds.add(playerPerformanceNode.path("player").path("id").asInt());
                }
            }
        }
        return playerIds;
    }

    private Player createPlayer(JsonNode playerInfoNode) {
        Player player = new Player();
        player.setId(playerInfoNode.path("id").asInt());
        player.setName(playerInfoNode.path("name").asText());
        player.setPhotoUrl(playerInfoNode.path("photo").asText(null));
        return player;
    }

    private Fixture createOrUpdateFixtureFromJson(JsonNode fixtureNode, Map<Long, Fixture> existingFixturesMap) {
        long fixtureId = fixtureNode.path("fixture").path("id").asLong();
        Fixture fixture = existingFixturesMap.getOrDefault(fixtureId, new Fixture());
        fixture.setId(fixtureId);
        fixture.setLeagueId(fixtureNode.path("league").path("id").asInt());
        fixture.setSeason(fixtureNode.path("league").path("season").asInt());
        fixture.setDate(Instant.parse(fixtureNode.path("fixture").path("date").asText()));
        fixture.setStatus(fixtureNode.path("fixture").path("status").path("short").asText());
        fixture.setHomeTeamId(fixtureNode.path("teams").path("home").path("id").asInt());
        fixture.setHomeTeamName(fixtureNode.path("teams").path("home").path("name").asText());
        fixture.setAwayTeamId(fixtureNode.path("teams").path("away").path("id").asInt());
        fixture.setAwayTeamName(fixtureNode.path("teams").path("away").path("name").asText());
        JsonNode goalsNode = fixtureNode.path("goals");
        if (!goalsNode.path("home").isNull()) fixture.setGoalsHome(goalsNode.path("home").asInt());
        if (!goalsNode.path("away").isNull()) fixture.setGoalsAway(goalsNode.path("away").asInt());
        return fixture;
    }

    private MatchStatistics createMatchStatistics(JsonNode teamStatsNode, long fixtureId, int teamId) {
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
        return matchStats;
    }

    private PlayerMatchStatistics createPlayerMatchStatistics(JsonNode statsNode, long fixtureId, int teamId, int playerId) {
        PlayerMatchStatistics pms = new PlayerMatchStatistics();
        pms.setFixtureId(fixtureId);
        pms.setTeamId(teamId);
        pms.setPlayerId(playerId);
        pms.setMinutesPlayed(statsNode.path("games").path("minutes").asInt(0));
        pms.setRating(statsNode.path("games").path("rating").asText(null));
        pms.setCaptain(statsNode.path("games").path("captain").asBoolean(false));
        pms.setSubstitute(statsNode.path("games").path("substitute").asBoolean(false));
        pms.setShotsTotal(statsNode.path("shots").path("total").asInt(0));
        pms.setShotsOnGoal(statsNode.path("shots").path("on").asInt(0));
        pms.setGoalsTotal(statsNode.path("goals").path("total").asInt(0));
        pms.setGoalsConceded(statsNode.path("goals").path("conceded").asInt(0));
        pms.setAssists(statsNode.path("goals").path("assists").asInt(0));
        pms.setSaves(statsNode.path("goals").path("saves").asInt(0));
        pms.setPassesTotal(statsNode.path("passes").path("total").asInt(0));
        pms.setPassesKey(statsNode.path("passes").path("key").asInt(0));
        pms.setPassesAccuracy(statsNode.path("passes").path("accuracy").asText(null));
        pms.setTacklesTotal(statsNode.path("tackles").path("total").asInt(0));
        pms.setTacklesBlocks(statsNode.path("tackles").path("blocks").asInt(0));
        pms.setTacklesInterceptions(statsNode.path("tackles").path("interceptions").asInt(0));
        pms.setDribblesAttempts(statsNode.path("dribbles").path("attempts").asInt(0));
        pms.setDribblesSuccess(statsNode.path("dribbles").path("success").asInt(0));
        pms.setDuelsTotal(statsNode.path("duels").path("total").asInt(0));
        pms.setDuelsWon(statsNode.path("duels").path("won").asInt(0));
        pms.setFoulsDrawn(statsNode.path("fouls").path("drawn").asInt(0));
        pms.setFoulsCommitted(statsNode.path("fouls").path("committed").asInt(0));
        pms.setCardsYellow(statsNode.path("cards").path("yellow").asInt(0));
        pms.setCardsRed(statsNode.path("cards").path("red").asInt(0));
        pms.setPenaltyWon(statsNode.path("penalty").path("won").asInt(0));
        pms.setPenaltyCommitted(statsNode.path("penalty").path("commited").asInt(0));
        pms.setPenaltyScored(statsNode.path("penalty").path("scored").asInt(0));
        pms.setPenaltyMissed(statsNode.path("penalty").path("missed").asInt(0));
        pms.setPenaltySaved(statsNode.path("penalty").path("saved").asInt(0));
        return pms;
    }

    private Injury createInjury(JsonNode injuryNode) {
        Injury injury = new Injury();
        injury.setFixtureId(injuryNode.path("fixture").path("id").asLong());
        injury.setPlayerId(injuryNode.path("player").path("id").asInt());
        injury.setPlayerName(injuryNode.path("player").path("name").asText());
        injury.setTeamId(injuryNode.path("team").path("id").asInt());
        injury.setLeagueId(injuryNode.path("league").path("id").asInt());
        injury.setSeason(injuryNode.path("league").path("season").asInt());
        injury.setType(injuryNode.path("player").path("type").asText());
        injury.setReason(injuryNode.path("player").path("reason").asText());
        return injury;
    }
}