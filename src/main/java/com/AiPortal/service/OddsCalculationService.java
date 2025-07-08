// src/main/java/com/AiPortal/service/OddsCalculationService.java

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
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public Optional<ValueBetDto> calculateValue(Fixture fixture) {
        if (fixture.getHomeTeamId() == null || fixture.getAwayTeamId() == null || fixture.getLeagueId() == null || fixture.getSeason() == null) {
            log.warn("Mangler nødvendige ID-er (lag, liga, eller sesong) for kamp ID: {}", fixture.getId());
            return Optional.empty();
        }

        // --- START PÅ BEDRE LOGGING ---
        log.info("Henter statistikk for hjemmelag ID: {}", fixture.getHomeTeamId());
        Optional<TeamStatistics> homeStatsOpt = findValidTeamStatistics(fixture.getLeagueId(), fixture.getSeason(), fixture.getHomeTeamId());
        log.info("--> Hjemmelag-statistikk funnet: {}", homeStatsOpt.isPresent());

        log.info("Henter statistikk for bortelag ID: {}", fixture.getAwayTeamId());
        Optional<TeamStatistics> awayStatsOpt = findValidTeamStatistics(fixture.getLeagueId(), fixture.getSeason(), fixture.getAwayTeamId());
        log.info("--> Bortelag-statistikk funnet: {}", awayStatsOpt.isPresent());

        log.info("Henter odds for fixture ID: {}", fixture.getId());
        Optional<MatchOdds> marketOddsOpt = oddsRepository.findTopByFixtureId(fixture.getId());
        log.info("--> Odds funnet: {}", marketOddsOpt.isPresent());
        // --- SLUTT PÅ BEDRE LOGGING ---

        if (homeStatsOpt.isEmpty() || awayStatsOpt.isEmpty() || marketOddsOpt.isEmpty()) {
            log.warn("Analyse avbrutt for kamp ID {}: Mangler en eller flere datakilder (hjemme-stats: {}, borte-stats: {}, odds: {}).",
                    fixture.getId(), homeStatsOpt.isPresent(), awayStatsOpt.isPresent(), marketOddsOpt.isPresent());
            return Optional.empty();
        }

        TeamStatistics homeStats = homeStatsOpt.get();
        TeamStatistics awayStats = awayStatsOpt.get();
        MatchOdds marketOdds = marketOddsOpt.get();

        log.info("Bruker statistikk for sesong {} for hjemmelag og sesong {} for bortelag.", homeStats.getSeason(), awayStats.getSeason());

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

        // ... resten av Poisson-kalkulasjonen er uendret ...
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

    private Optional<TeamStatistics> findValidTeamStatistics(int leagueId, int season, int teamId) {
        Optional<TeamStatistics> statsOpt = statsRepository.findByLeagueIdAndSeasonAndTeamId(leagueId, season, teamId);
        if (statsOpt.isPresent() && statsOpt.get().getPlayedTotal() > 0) {
            return statsOpt;
        }
        log.warn("Statistikk for lag {} i sesong {} er ubrukelig eller mangler. Forsøker sesong {}...", teamId, season, season - 1);
        Optional<TeamStatistics> lastSeasonStatsOpt = statsRepository.findByLeagueIdAndSeasonAndTeamId(leagueId, season - 1, teamId);
        if (lastSeasonStatsOpt.isPresent() && lastSeasonStatsOpt.get().getPlayedTotal() > 0) {
            return lastSeasonStatsOpt;
        }
        return Optional.empty();
    }
}