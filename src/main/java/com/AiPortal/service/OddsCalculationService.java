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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OddsCalculationService {

    private static final Logger log = LoggerFactory.getLogger(OddsCalculationService.class);
    private static final int FORM_MATCH_COUNT = 10; // Hvor mange kamper skal vi basere form på?
    private static final int MINIMUM_MATCHES_FOR_FORM_STATS = 3; // Minimum antall kamper for å bruke form-statistikk

    // Alle repositories vi trenger
    private final TeamStatisticsRepository teamStatsRepository;
    private final MatchOddsRepository oddsRepository;
    private final FixtureRepository fixtureRepository;
    private final MatchStatisticsRepository matchStatsRepository;

    public OddsCalculationService(
            TeamStatisticsRepository teamStatsRepository,
            MatchOddsRepository matchOddsRepository,
            FixtureRepository fixtureRepository,
            MatchStatisticsRepository matchStatsRepository) {
        this.teamStatsRepository = teamStatsRepository;
        this.oddsRepository = matchOddsRepository;
        this.fixtureRepository = fixtureRepository;
        this.matchStatsRepository = matchStatsRepository;
    }

    /**
     * En intern "holder"-klasse for å returnere beregnede styrker fra en metode.
     */
    private static class StrengthData {
        final double expectedHomeGoals;
        final double expectedAwayGoals;
        final String dataSource;

        StrengthData(double expectedHomeGoals, double expectedAwayGoals, String dataSource) {
            this.expectedHomeGoals = expectedHomeGoals;
            this.expectedAwayGoals = expectedAwayGoals;
            this.dataSource = dataSource;
        }
    }

    /**
     * Hovedmetode for å kalkulere verdien i et spill.
     */
    @Transactional(readOnly = true)
    public Optional<ValueBetDto> calculateValue(Fixture fixture) {
        // Hent markedsodds. Hvis ingen odds finnes, kan vi ikke gjøre en analyse.
        Optional<MatchOdds> marketOddsOpt = oddsRepository.findTopByFixtureId(fixture.getId());
        if (marketOddsOpt.isEmpty()) {
            log.warn("Ingen markedsodds funnet for kamp ID {}. Avbryter analyse.", fixture.getId());
            return Optional.empty();
        }

        // Prøv å beregne forventede mål med den nye, form-baserte metoden.
        // Hvis den feiler eller ikke finner nok data, faller den tilbake på den gamle metoden.
        StrengthData strengths = getExpectedGoals(fixture);

        // Hvis vi ikke fikk noen styrkedata (verken fra ny eller gammel metode), avbryt.
        if (strengths == null) {
            log.warn("Kunne ikke beregne styrke for lagene i kamp ID {}. Avbryter analyse.", fixture.getId());
            return Optional.empty();
        }

        log.info("Kamp ID {}: Beregner odds basert på datakilde: '{}'. Forventede mål (H-A): {:.2f} - {:.2f}",
                fixture.getId(), strengths.dataSource, strengths.expectedHomeGoals, strengths.expectedAwayGoals);

        // Kjør Poisson-distribusjonen på de forventede målene.
        double homeWinProb = 0, drawProb = 0, awayWinProb = 0;
        PoissonDistribution homePoisson = new PoissonDistribution(strengths.expectedHomeGoals);
        PoissonDistribution awayPoisson = new PoissonDistribution(strengths.expectedAwayGoals);

        // Beregn sannsynlighet for H, U, B
        for (int i = 0; i <= 7; i++) { // Hjemmelagets mål
            for (int j = 0; j <= 7; j++) { // Bortelagets mål
                double prob = homePoisson.probability(i) * awayPoisson.probability(j);
                if (i > j) homeWinProb += prob;
                else if (i == j) drawProb += prob;
                else awayWinProb += prob;
            }
        }

        // Normaliser sannsynlighetene slik at de summerer til 1 (eller 100%)
        double totalProb = homeWinProb + drawProb + awayWinProb;
        if (totalProb == 0) return Optional.empty();
        homeWinProb /= totalProb;
        drawProb /= totalProb;
        awayWinProb /= totalProb;

        // Bygg og returner DTO-en med alle resultatene
        return Optional.of(buildValueBetDto(fixture, marketOddsOpt.get(), homeWinProb, drawProb, awayWinProb));
    }

    /**
     * Strategi-metode: Prøver først å hente form-basert statistikk.
     * Hvis det feiler, faller den tilbake til sesong-basert statistikk.
     */
    private StrengthData getExpectedGoals(Fixture fixture) {
        Optional<StrengthData> formBasedStrengths = calculateStrengthFromForm(fixture);
        if (formBasedStrengths.isPresent()) {
            return formBasedStrengths.get();
        }

        log.warn("Kamp ID {}: Kunne ikke bruke form-basert statistikk. Faller tilbake på generell sesong-statistikk.", fixture.getId());

        Optional<StrengthData> seasonBasedStrengths = calculateStrengthFromSeason(fixture);
        return seasonBasedStrengths.orElse(null);
    }


    /**
     * NY METODE: Beregner forventede mål basert på detaljert statistikk fra de siste N kampene.
     */
    private Optional<StrengthData> calculateStrengthFromForm(Fixture fixture) {
        // Hent de siste N spilte kampene for hvert lag
        List<Fixture> homeTeamLastFixtures = fixtureRepository.findLastNCompletedFixturesByTeamAndSeason(fixture.getHomeTeamId(), fixture.getSeason(), PageRequest.of(0, FORM_MATCH_COUNT));
        List<Fixture> awayTeamLastFixtures = fixtureRepository.findLastNCompletedFixturesByTeamAndSeason(fixture.getAwayTeamId(), fixture.getSeason(), PageRequest.of(0, FORM_MATCH_COUNT));

        // Sjekk om vi har nok data til å lage en pålitelig analyse
        if (homeTeamLastFixtures.size() < MINIMUM_MATCHES_FOR_FORM_STATS || awayTeamLastFixtures.size() < MINIMUM_MATCHES_FOR_FORM_STATS) {
            log.info("Kamp ID {}: Ikke nok spilte kamper for form-analyse (H: {}, A: {}). Minimum kreves: {}", fixture.getId(), homeTeamLastFixtures.size(), awayTeamLastFixtures.size(), MINIMUM_MATCHES_FOR_FORM_STATS);
            return Optional.empty();
        }

        // Hent all kampstatistikk for disse kampene i to effektive kall
        List<Long> homeFixtureIds = homeTeamLastFixtures.stream().map(Fixture::getId).collect(Collectors.toList());
        List<Long> awayFixtureIds = awayTeamLastFixtures.stream().map(Fixture::getId).collect(Collectors.toList());
        List<MatchStatistics> homeTeamStats = matchStatsRepository.findAllByFixtureIdIn(homeFixtureIds);
        List<MatchStatistics> awayTeamStats = matchStatsRepository.findAllByFixtureIdIn(awayFixtureIds);

        // Beregn gjennomsnittlig "shots on goal" for og mot
        double homeSotFor = calculateAverageSot(homeTeamStats, fixture.getHomeTeamId(), true);
        double homeSotAgainst = calculateAverageSot(homeTeamStats, fixture.getHomeTeamId(), false);
        double awaySotFor = calculateAverageSot(awayTeamStats, fixture.getAwayTeamId(), true);
        double awaySotAgainst = calculateAverageSot(awayTeamStats, fixture.getAwayTeamId(), false);

        // Hent hele ligaens gjennomsnitt for "shots on goal"
        // (Dette er en forenklet versjon, kan forbedres ved å cache resultatet)
        double leagueAvgSot = calculateLeagueAverageSot(fixture.getLeagueId(), fixture.getSeason());

        if (leagueAvgSot == 0) {
            log.error("Kan ikke beregne styrke for kamp ID {}: Ligaens gjennomsnittlige skudd på mål er 0.", fixture.getId());
            return Optional.empty();
        }

        // Beregn angreps- og forsvarsstyrke basert på form
        double homeAttackStrength = homeSotFor / leagueAvgSot;
        double homeDefenceStrength = homeSotAgainst / leagueAvgSot;
        double awayAttackStrength = awaySotFor / leagueAvgSot;
        double awayDefenceStrength = awaySotAgainst / leagueAvgSot;

        // Beregn forventet antall mål (vår xG-proxy)
        double expectedHomeGoals = homeAttackStrength * awayDefenceStrength * leagueAvgSot;
        double expectedAwayGoals = awayAttackStrength * homeDefenceStrength * leagueAvgSot;

        return Optional.of(new StrengthData(expectedHomeGoals, expectedAwayGoals, "Form-basert (MatchStats)"));
    }

    /**
     * GAMMEL METODE (nå en fallback): Beregner forventede mål basert på hele sesongens statistikk.
     */
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

    // --- Hjelpemetoder ---

    private ValueBetDto buildValueBetDto(Fixture fixture, MatchOdds marketOdds, double homeProb, double drawProb, double awayProb) {
        ValueBetDto valueBet = new ValueBetDto();
        valueBet.setFixtureId(fixture.getId());
        valueBet.setHomeTeamName(fixture.getHomeTeamName());
        valueBet.setAwayTeamName(fixture.getAwayTeamName());
        valueBet.setFixtureDate(fixture.getDate());
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

    private double calculateAverageSot(List<MatchStatistics> stats, int teamId, boolean isFor) {
        int totalSot = 0;
        long matchCount = stats.stream().map(MatchStatistics::getFixtureId).distinct().count();
        if (matchCount == 0) return 0.0;

        for (MatchStatistics stat : stats) {
            if (isFor) { // Skudd FOR laget
                if (stat.getTeamId() == teamId && stat.getShotsOnGoal() != null) {
                    totalSot += stat.getShotsOnGoal();
                }
            } else { // Skudd MOT laget
                if (stat.getTeamId() != teamId && stat.getShotsOnGoal() != null) {
                    totalSot += stat.getShotsOnGoal();
                }
            }
        }
        return (double) totalSot / matchCount;
    }

    private double calculateLeagueAverageSot(int leagueId, int season) {
        // Denne metoden er en forenkling. En produksjonsklar versjon bør hente
        // all statistikk for ligaen/sesongen og beregne et reelt gjennomsnitt.
        // For nå bruker vi en fornuftig, hardkodet verdi som representerer
        // et typisk antall skudd på mål per lag i en kamp.
        return 4.5; // TODO: Implementer reell beregning av ligagjennomsnitt
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