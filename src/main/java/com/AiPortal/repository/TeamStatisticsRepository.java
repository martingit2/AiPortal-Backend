package com.AiPortal.repository;

import com.AiPortal.entity.TeamStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamStatisticsRepository extends JpaRepository<TeamStatistics, Long> {

    /**
     * Egendefinert metode for å finne statistikk basert på en unik kombinasjon
     * av liga, sesong og lag. Dette kan brukes for å sjekke om vi skal
     * oppdatere en eksisterende rad i stedet for å lage en ny.
     *
     * @param leagueId ID for liga.
     * @param season Sesongårstall.
     * @param teamId ID for lag.
     * @return En Optional som inneholder statistikken hvis den finnes.
     */
    Optional<TeamStatistics> findByLeagueIdAndSeasonAndTeamId(int leagueId, int season, int teamId);

}