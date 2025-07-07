package com.AiPortal.service;

import com.AiPortal.dto.ValueBetDto;
import com.AiPortal.entity.Fixture;
import com.AiPortal.entity.MatchOdds;
import com.AiPortal.entity.TeamStatistics;
import com.AiPortal.repository.MatchOddsRepository;
import com.AiPortal.repository.TeamStatisticsRepository;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OddsCalculationService {

    private static final Logger log = LoggerFactory.getLogger(OddsCalculationService.class);

    private final TeamStatisticsRepository statsRepository;
    private final MatchOddsRepository oddsRepository;

    public OddsCalculationService(TeamStatisticsRepository statsRepository, MatchOddsRepository matchOddsRepository) {
        this.statsRepository = statsRepository;
        this.oddsRepository = matchOddsRepository;
    }

    public Optional<ValueBetDto> calculateValue(Fixture fixture) {
        // Legg til en null-sjekk for ID-er før vi prøver å hente data
        if (fixture.getHomeTeamId() == null || fixture.getAwayTeamId() == null || fixture.getLeagueId() == null || fixture.getSeason() == null) {
            log.warn("Mangler nødvendige ID-er (lag, liga, eller sesong) for kamp ID: {}", fixture.getId());
            return Optional.empty();
        }

        // Hent statistikk for begge lag
        Optional<TeamStatistics> homeStatsOpt = statsRepository.findByLeagueIdAndSeasonAndTeamId(fixture.getLeagueId(), fixture.getSeason(), fixture.getHomeTeamId());
        Optional<TeamStatistics> awayStatsOpt = statsRepository.findByLeagueIdAndSeasonAndTeamId(fixture.getLeagueId(), fixture.getSeason(), fixture.getAwayTeamId());
        Optional<MatchOdds> marketOddsOpt = oddsRepository.findTopByFixtureId(fixture.getId());

        if (homeStatsOpt.isEmpty()) {
            log.warn("Mangler statistikk for hjemmelag ID: {} i liga {} sesong {}", fixture.getHomeTeamId(), fixture.getLeagueId(), fixture.getSeason());
            return Optional.empty();
        }
        if (awayStatsOpt.isEmpty()) {
            log.warn("Mangler statistikk for bortelag ID: {} i liga {} sesong {}", fixture.getAwayTeamId(), fixture.getLeagueId(), fixture.getSeason());
            return Optional.empty();
        }
        if (marketOddsOpt.isEmpty()) {
            log.warn("Mangler odds for kamp ID: {}", fixture.getId());
            return Optional.empty();
        }

        TeamStatistics homeStats = homeStatsOpt.get();
        TeamStatistics awayStats = awayStatsOpt.get();
        MatchOdds marketOdds = marketOddsOpt.get();

        if (homeStats.getPlayedTotal() == 0 || awayStats.getPlayedTotal() == 0) {
            log.warn("Kan ikke beregne for kamp ID: {} fordi et av lagene har 0 kamper spilt.", fixture.getId());
            return Optional.empty();
        }

        // --- Beregninger ---
        double avgLeagueGoalsFor = (double) (homeStats.getGoalsForTotal() + awayStats.getGoalsForTotal()) / (homeStats.getPlayedTotal() + awayStats.getPlayedTotal());
        double avgLeagueGoalsAgainst = (double) (homeStats.getGoalsAgainstTotal() + awayStats.getGoalsAgainstTotal()) / (homeStats.getPlayedTotal() + awayStats.getPlayedTotal());

        if (avgLeagueGoalsFor == 0 || avgLeagueGoalsAgainst == 0) {
            log.warn("Kan ikke beregne for kamp ID: {} fordi gjennomsnittlig antall mål i ligaen er 0.", fixture.getId());
            return Optional.empty();
        }

        double homeAttackStrength = ((double) homeStats.getGoalsForTotal() / homeStats.getPlayedTotal()) / avgLeagueGoalsFor;
        double awayAttackStrength = ((double) awayStats.getGoalsForTotal() / awayStats.getPlayedTotal()) / avgLeagueGoalsFor;
        double homeDefenceStrength = ((double) homeStats.getGoalsAgainstTotal() / homeStats.getPlayedTotal()) / avgLeagueGoalsAgainst;
        double awayDefenceStrength = ((double) awayStats.getGoalsAgainstTotal() / awayStats.getPlayedTotal()) / avgLeagueGoalsAgainst;

        double expectedHomeGoals = homeAttackStrength * awayDefenceStrength * avgLeagueGoalsFor;
        double expectedAwayGoals = awayAttackStrength * homeDefenceStrength * avgLeagueGoalsFor;

        double homeWinProb = 0, drawProb = 0, awayWinProb = 0;

        PoissonDistribution homePoisson = new PoissonDistribution(expectedHomeGoals);
        PoissonDistribution awayPoisson = new PoissonDistribution(expectedAwayGoals);

        for (int i = 0; i <= 7; i++) {
            for (int j = 0; j <= 7; j++) {
                double prob = homePoisson.probability(i) * awayPoisson.probability(j);
                if (i > j) homeWinProb += prob;
                else if (i == j) drawProb += prob;
                else awayWinProb += prob;
            }
        }

        double totalProb = homeWinProb + drawProb + awayWinProb;
        if (totalProb == 0) return Optional.empty();

        homeWinProb /= totalProb;
        drawProb /= totalProb;
        awayWinProb /= totalProb;

        // --- Lag ValueBetDto ---
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

        valueBet.setAracanixHomeOdds(homeWinProb > 0 ? 1 / homeWinProb : Double.POSITIVE_INFINITY);
        valueBet.setAracanixDrawOdds(drawProb > 0 ? 1 / drawProb : Double.POSITIVE_INFINITY);
        valueBet.setAracanixAwayOdds(awayWinProb > 0 ? 1 / awayWinProb : Double.POSITIVE_INFINITY);

        valueBet.setValueHome((marketOdds.getHomeOdds() * homeWinProb) - 1);
        valueBet.setValueDraw((marketOdds.getDrawOdds() * drawProb) - 1);
        valueBet.setValueAway((marketOdds.getAwayOdds() * awayWinProb) - 1);

        log.info("Vellykket beregning for kamp: {} vs {}", fixture.getHomeTeamName(), fixture.getAwayTeamName());
        return Optional.of(valueBet);
    }
}