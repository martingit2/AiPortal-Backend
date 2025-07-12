// src/main/java/com/AiPortal/service/OddsCalculationService.java

package com.AiPortal.service;

import com.AiPortal.dto.ValueBetDto;
import com.AiPortal.entity.Fixture;
import com.AiPortal.entity.MatchOdds;
import com.AiPortal.entity.MatchStatistics;
import com.AiPortal.entity.TeamStatistics;
import com.AiPortal.repository.FixtureRepository;
import com.AiPortal.repository.MatchOddsRepository;
import com.AiPortal.repository.MatchStatisticsRepository;
import com.AiPortal.repository.TeamStatisticsRepository;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OddsCalculationService {

    private static final Logger log = LoggerFactory.getLogger(OddsCalculationService.class);
    private static final int FORM_MATCH_COUNT = 10;
    private static final int MINIMUM_MATCHES_FOR_FORM_STATS = 3;
    private static final double SENTIMENT_INFLUENCE_FACTOR = 0.05; // 5% påvirkning
    private static final int SENTIMENT_SEARCH_HOURS = 48; // Se på tweets fra de siste 48 timene
    private static final double SENTIMENT_THRESHOLD = 0.25; // Hvor høy scoren må være for å lage et signal

    private final TeamStatisticsRepository teamStatsRepository;
    private final MatchOddsRepository oddsRepository;
    private final FixtureRepository fixtureRepository;
    private final MatchStatisticsRepository matchStatsRepository;
    private final AnalysisService analysisService;

    public OddsCalculationService(
            TeamStatisticsRepository teamStatsRepository,
            MatchOddsRepository matchOddsRepository,
            FixtureRepository fixtureRepository,
            MatchStatisticsRepository matchStatsRepository,
            AnalysisService analysisService) {
        this.teamStatsRepository = teamStatsRepository;
        this.oddsRepository = matchOddsRepository;
        this.fixtureRepository = fixtureRepository;
        this.matchStatsRepository = matchStatsRepository;
        this.analysisService = analysisService;
    }

    private static class StrengthData {
        final double expectedHomeGoals;
        final double expectedAwayGoals;
        final String dataSource;
        StrengthData(double h, double a, String s) { this.expectedHomeGoals = h; this.expectedAwayGoals = a; this.dataSource = s; }
    }

    /**
     * Hovedmetoden. Returnerer nå en LISTE med potensielle verdispill for ulike markeder.
     */
    @Transactional(readOnly = true)
    public List<ValueBetDto> calculateValue(Fixture fixture) {
        List<ValueBetDto> foundValueBets = new ArrayList<>();

        // 1. Beregn "Match Winner"-verdispillet
        calculateMatchWinnerValue(fixture).ifPresent(foundValueBets::add);

        // 2. Beregn sentiment-baserte signaler for andre markeder
        foundValueBets.addAll(calculateSentimentMarketSignals(fixture));

        return foundValueBets;
    }

    private Optional<ValueBetDto> calculateMatchWinnerValue(Fixture fixture) {
        Optional<MatchOdds> marketOddsOpt = oddsRepository.findTopByFixtureId(fixture.getId());
        if (marketOddsOpt.isEmpty()) {
            return Optional.empty();
        }

        StrengthData strengths = getExpectedGoals(fixture);
        if (strengths == null) {
            return Optional.empty();
        }

        log.info("Kamp ID {}: Beregner 'Match Winner' basert på datakilde: '{}'. Forventede mål (H-A): {:.2f} - {:.2f}",
                fixture.getId(), strengths.dataSource, strengths.expectedHomeGoals, strengths.expectedAwayGoals);

        // Poisson-beregning som før...
        double homeWinProb = 0, drawProb = 0, awayWinProb = 0;
        PoissonDistribution homePoisson = new PoissonDistribution(strengths.expectedHomeGoals);
        PoissonDistribution awayPoisson = new PoissonDistribution(strengths.expectedAwayGoals);
        for (int i = 0; i <= 7; i++) {
            for (int j = 0; j <= 7; j++) {
                double prob = homePoisson.probability(i) * awayPoisson.probability(j);
                if (i > j) homeWinProb += prob;
                else if (i == j) drawProb += prob;
                else awayWinProb += prob;
            }
        }

        // Første normalisering av statistiske sannsynligheter
        double totalProb = homeWinProb + drawProb + awayWinProb;
        if(totalProb > 0) {
            homeWinProb /= totalProb;
            drawProb /= totalProb;
            awayWinProb /= totalProb;
        }

        // Juster for generelt sentiment om kampresultat
        double generalHomeSentiment = analysisService.getAggregatedSentimentForMarket(fixture.getHomeTeamName(), "TEAM_RESULT", SENTIMENT_SEARCH_HOURS);
        double generalAwaySentiment = analysisService.getAggregatedSentimentForMarket(fixture.getAwayTeamName(), "TEAM_RESULT", SENTIMENT_SEARCH_HOURS);

        homeWinProb = homeWinProb + (generalHomeSentiment * SENTIMENT_INFLUENCE_FACTOR);
        awayWinProb = awayWinProb + (generalAwaySentiment * SENTIMENT_INFLUENCE_FACTOR);

        // Sikrer at sannsynlighetene ikke blir negative
        homeWinProb = Math.max(0, homeWinProb);
        awayWinProb = Math.max(0, awayWinProb);

        // Normaliser igjen etter sentiment-justering
        double totalAdjustedProb = homeWinProb + drawProb + awayWinProb;
        if (totalAdjustedProb > 0) {
            homeWinProb /= totalAdjustedProb;
            drawProb /= totalAdjustedProb;
            awayWinProb /= totalAdjustedProb;
        }

        return Optional.of(buildValueBetDto(fixture, marketOddsOpt.get(), homeWinProb, drawProb, awayWinProb));
    }

    private List<ValueBetDto> calculateSentimentMarketSignals(Fixture fixture) {
        List<ValueBetDto> signals = new ArrayList<>();

        // Sjekk for CORNERS
        double homeCornersSentiment = analysisService.getAggregatedSentimentForMarket(fixture.getHomeTeamName(), "CORNERS", SENTIMENT_SEARCH_HOURS);
        if (homeCornersSentiment > SENTIMENT_THRESHOLD) {
            signals.add(createSentimentSignalDto(fixture, String.format("Høyt positivt sentiment (%.2f) for cornere til %s", homeCornersSentiment, fixture.getHomeTeamName())));
        }
        double awayCornersSentiment = analysisService.getAggregatedSentimentForMarket(fixture.getAwayTeamName(), "CORNERS", SENTIMENT_SEARCH_HOURS);
        if (awayCornersSentiment > SENTIMENT_THRESHOLD) {
            signals.add(createSentimentSignalDto(fixture, String.format("Høyt positivt sentiment (%.2f) for cornere til %s", awayCornersSentiment, fixture.getAwayTeamName())));
        }

        // Sjekk for CARDS
        double cardsSentiment = analysisService.getAggregatedSentimentForMarket(fixture.getHomeTeamName() + " " + fixture.getAwayTeamName(), "CARDS", SENTIMENT_SEARCH_HOURS);
        if (cardsSentiment > SENTIMENT_THRESHOLD) {
            signals.add(createSentimentSignalDto(fixture, String.format("Høyt positivt sentiment (%.2f) for kort i kampen", cardsSentiment)));
        }

        // Sjekk for GOALS_OVER_UNDER
        double goalsSentiment = analysisService.getAggregatedSentimentForMarket(fixture.getHomeTeamName() + " " + fixture.getAwayTeamName(), "GOALS_OVER_UNDER", SENTIMENT_SEARCH_HOURS);
        if (goalsSentiment > SENTIMENT_THRESHOLD) {
            signals.add(createSentimentSignalDto(fixture, String.format("Høyt positivt sentiment (%.2f) for mange mål i kampen", goalsSentiment)));
        } else if (goalsSentiment < -SENTIMENT_THRESHOLD) {
            signals.add(createSentimentSignalDto(fixture, String.format("Høyt negativt sentiment (%.2f) for få mål i kampen", goalsSentiment)));
        }

        return signals;
    }

    private ValueBetDto createSentimentSignalDto(Fixture fixture, String description) {
        ValueBetDto signalDto = new ValueBetDto();
        signalDto.setFixtureId(fixture.getId());
        signalDto.setHomeTeamName(fixture.getHomeTeamName());
        signalDto.setAwayTeamName(fixture.getAwayTeamName());
        signalDto.setFixtureDate(fixture.getDate());
        signalDto.setMarketDescription(description); // Her legger vi inn signalet
        // De andre feltene (odds, value) er 0/default, siden dette kun er et signal.
        return signalDto;
    }

    private StrengthData getExpectedGoals(Fixture fixture) {
        Optional<StrengthData> formBasedStrengths = calculateStrengthFromForm(fixture);
        if (formBasedStrengths.isPresent()) {
            return formBasedStrengths.get();
        }
        log.warn("Kamp ID {}: Kunne ikke bruke form-basert statistikk. Faller tilbake på generell sesong-statistikk.", fixture.getId());
        return calculateStrengthFromSeason(fixture).orElse(null);
    }

    private Optional<StrengthData> calculateStrengthFromForm(Fixture fixture) {
        List<Fixture> homeTeamLastFixtures = fixtureRepository.findLastNCompletedFixturesByTeamAndSeason(fixture.getHomeTeamId(), fixture.getSeason(), PageRequest.of(0, FORM_MATCH_COUNT));
        List<Fixture> awayTeamLastFixtures = fixtureRepository.findLastNCompletedFixturesByTeamAndSeason(fixture.getAwayTeamId(), fixture.getSeason(), PageRequest.of(0, FORM_MATCH_COUNT));

        if (homeTeamLastFixtures.size() < MINIMUM_MATCHES_FOR_FORM_STATS || awayTeamLastFixtures.size() < MINIMUM_MATCHES_FOR_FORM_STATS) {
            log.info("Kamp ID {}: Ikke nok spilte kamper for form-analyse (H: {}, A: {}). Minimum kreves: {}", fixture.getId(), homeTeamLastFixtures.size(), awayTeamLastFixtures.size(), MINIMUM_MATCHES_FOR_FORM_STATS);
            return Optional.empty();
        }

        List<Long> homeFixtureIds = homeTeamLastFixtures.stream().map(Fixture::getId).collect(Collectors.toList());
        List<Long> awayFixtureIds = awayTeamLastFixtures.stream().map(Fixture::getId).collect(Collectors.toList());
        List<MatchStatistics> homeTeamStats = matchStatsRepository.findAllByFixtureIdIn(homeFixtureIds);
        List<MatchStatistics> awayTeamStats = matchStatsRepository.findAllByFixtureIdIn(awayFixtureIds);

        double homeSotFor = calculateAverageSotForTeam(homeTeamStats, fixture.getHomeTeamId());
        double homeSotAgainst = calculateAverageSotAgainstTeam(homeTeamStats, fixture.getHomeTeamId());
        double awaySotFor = calculateAverageSotForTeam(awayTeamStats, fixture.getAwayTeamId());
        double awaySotAgainst = calculateAverageSotAgainstTeam(awayTeamStats, fixture.getAwayTeamId());

        double leagueAvgSot = calculateLeagueAverageSot(fixture.getLeagueId(), fixture.getSeason());

        if (leagueAvgSot <= 0) {
            log.error("Kan ikke beregne styrke for kamp ID {}: Ligaens gjennomsnittlige skudd på mål er 0 eller mindre.", fixture.getId());
            return Optional.empty();
        }

        double homeAttackStrength = homeSotFor / leagueAvgSot;
        double homeDefenceStrength = homeSotAgainst / leagueAvgSot;
        double awayAttackStrength = awaySotFor / leagueAvgSot;
        double awayDefenceStrength = awaySotAgainst / leagueAvgSot;

        double expectedHomeGoals = homeAttackStrength * awayDefenceStrength * leagueAvgSot;
        double expectedAwayGoals = awayAttackStrength * homeDefenceStrength * leagueAvgSot;

        return Optional.of(new StrengthData(expectedHomeGoals, expectedAwayGoals, "Form-basert (MatchStats)"));
    }

    private Optional<StrengthData> calculateStrengthFromSeason(Fixture fixture) {
        Optional<TeamStatistics> homeStatsOpt = findValidTeamStatistics(fixture.getLeagueId(), fixture.getSeason(), fixture.getHomeTeamId());
        Optional<TeamStatistics> awayStatsOpt = findValidTeamStatistics(fixture.getLeagueId(), fixture.getSeason(), fixture.getAwayTeamId());

        if (homeStatsOpt.isEmpty() || awayStatsOpt.isEmpty()) {
            return Optional.empty();
        }

        TeamStatistics homeStats = homeStatsOpt.get();
        TeamStatistics awayStats = awayStatsOpt.get();

        if (homeStats.getPlayedTotal() == 0 || awayStats.getPlayedTotal() == 0) {
            return Optional.empty();
        }

        double avgLeagueGoalsFor = (double) (homeStats.getGoalsForTotal() + awayStats.getGoalsForTotal()) / (homeStats.getPlayedTotal() + awayStats.getPlayedTotal());
        double avgLeagueGoalsAgainst = (double) (homeStats.getGoalsAgainstTotal() + awayStats.getGoalsAgainstTotal()) / (homeStats.getPlayedTotal() + awayStats.getPlayedTotal());

        if (avgLeagueGoalsFor == 0 || avgLeagueGoalsAgainst == 0) {
            return Optional.empty();
        }

        double homeAttackStrength = ((double) homeStats.getGoalsForTotal() / homeStats.getPlayedTotal()) / avgLeagueGoalsFor;
        double awayAttackStrength = ((double) awayStats.getGoalsForTotal() / awayStats.getPlayedTotal()) / avgLeagueGoalsFor;
        double homeDefenceStrength = ((double) homeStats.getGoalsAgainstTotal() / homeStats.getPlayedTotal()) / avgLeagueGoalsAgainst;
        double awayDefenceStrength = ((double) awayStats.getGoalsAgainstTotal() / awayStats.getPlayedTotal()) / avgLeagueGoalsAgainst;

        double expectedHomeGoals = homeAttackStrength * awayDefenceStrength * avgLeagueGoalsFor;
        double expectedAwayGoals = awayAttackStrength * homeDefenceStrength * avgLeagueGoalsFor;

        return Optional.of(new StrengthData(expectedHomeGoals, expectedAwayGoals, "Sesong-basert (TeamStats)"));
    }

    private ValueBetDto buildValueBetDto(Fixture fixture, MatchOdds marketOdds, double homeProb, double drawProb, double awayProb) {
        ValueBetDto valueBet = new ValueBetDto();
        valueBet.setFixtureId(fixture.getId());
        valueBet.setHomeTeamName(fixture.getHomeTeamName());
        valueBet.setAwayTeamName(fixture.getAwayTeamName());
        valueBet.setFixtureDate(fixture.getDate());
        valueBet.setMarketDescription("Match Winner"); // Standard marked
        valueBet.setMarketHomeOdds(marketOdds.getHomeOdds());
        valueBet.setMarketDrawOdds(marketOdds.getDrawOdds());
        valueBet.setMarketAwayOdds(marketOdds.getAwayOdds());
        if (marketOdds.getBookmaker() != null) {
            valueBet.setBookmakerName(marketOdds.getBookmaker().getName());
        }
        valueBet.setAracanixHomeOdds(homeProb > 0 ? 1 / homeProb : Double.POSITIVE_INFINITY);
        valueBet.setAracanixDrawOdds(drawProb > 0 ? 1 / drawProb : Double.POSITIVE_INFINITY);
        valueBet.setAracanixAwayOdds(awayProb > 0 ? 1 / awayProb : Double.POSITIVE_INFINITY);
        valueBet.setValueHome((marketOdds.getHomeOdds() * homeProb) - 1);
        valueBet.setValueDraw((marketOdds.getDrawOdds() * drawProb) - 1);
        valueBet.setValueAway((marketOdds.getAwayOdds() * awayProb) - 1);
        return valueBet;
    }

    private double calculateAverageSotForTeam(List<MatchStatistics> teamMatchStats, int teamId) {
        double totalSot = teamMatchStats.stream()
                .filter(stat -> stat.getTeamId().equals(teamId) && stat.getShotsOnGoal() != null)
                .mapToInt(MatchStatistics::getShotsOnGoal)
                .sum();
        long matchCount = teamMatchStats.stream().map(MatchStatistics::getFixtureId).distinct().count();
        return matchCount > 0 ? totalSot / matchCount : 0.0;
    }

    private double calculateAverageSotAgainstTeam(List<MatchStatistics> teamMatchStats, int teamId) {
        double totalSot = teamMatchStats.stream()
                .filter(stat -> !stat.getTeamId().equals(teamId) && stat.getShotsOnGoal() != null)
                .mapToInt(MatchStatistics::getShotsOnGoal)
                .sum();
        long matchCount = teamMatchStats.stream().map(MatchStatistics::getFixtureId).distinct().count();
        return matchCount > 0 ? totalSot / matchCount : 0.0;
    }

    @Cacheable("leagueAverageSot")
    public double calculateLeagueAverageSot(int leagueId, int season) {
        log.info("--- CACHE MISS: Beregner ligagjennomsnitt for skudd på mål for liga {}, sesong {} ---", leagueId, season);

        List<MatchStatistics> allStats = matchStatsRepository.findAllByLeagueAndSeason(leagueId, season);

        if (allStats.isEmpty()) {
            log.warn("Ingen MatchStatistics funnet for liga {} sesong {}. Kan ikke beregne gjennomsnitt.", leagueId, season);
            return 0.0;
        }

        int totalShotsOnGoal = allStats.stream()
                .filter(stat -> stat.getShotsOnGoal() != null)
                .mapToInt(MatchStatistics::getShotsOnGoal)
                .sum();

        long totalFixtures = allStats.stream().map(MatchStatistics::getFixtureId).distinct().count();

        if (totalFixtures == 0) {
            return 0.0;
        }

        double averagePerMatch = (double) totalShotsOnGoal / totalFixtures;

        log.info("Beregnet ligagjennomsnitt for liga {} sesong {}: {:.2f} skudd på mål per kamp (basert på {} kamper)", leagueId, season, averagePerMatch, totalFixtures);

        return averagePerMatch / 2.0;
    }

    private Optional<TeamStatistics> findValidTeamStatistics(int leagueId, int season, int teamId) {
        Optional<TeamStatistics> statsOpt = teamStatsRepository.findByLeagueIdAndSeasonAndTeamId(leagueId, season, teamId);
        if (statsOpt.isPresent() && statsOpt.get().getPlayedTotal() > 0) {
            return statsOpt;
        }
        log.warn("Statistikk for lag {} i sesong {} er ubrukelig eller mangler. Forsøker sesong {}...", teamId, season, season - 1);
        return teamStatsRepository.findByLeagueIdAndSeasonAndTeamId(leagueId, season - 1, teamId)
                .filter(stats -> stats.getPlayedTotal() > 0);
    }
}