// src/main/java/com/AiPortal/repository/TeamStatisticsRepository.java
package com.AiPortal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.AiPortal.entity.TeamStatistics;

import java.util.Optional;

@Repository
public interface TeamStatisticsRepository extends JpaRepository<TeamStatistics, Long> {

    /**
     * Egendefinert metode for å finne statistikk basert på en unik kombinasjon
     * av liga, sesong og lag. Brukes for å oppdatere eksisterende rader.
     * @param leagueId ID for liga.
     * @param season Sesongårstall.
     * @param teamId ID for lag.
     * @return En Optional som inneholder statistikken hvis den finnes.
     */
    Optional<TeamStatistics> findByLeagueIdAndSeasonAndTeamId(int leagueId, int season, int teamId);

    /**
     * Henter den første (og antatt eneste) statistikkraden for et gitt lag-ID.
     * Brukes for å effektivt slå opp lagnavn uten å hente en hel liste.
     * @param teamId ID-en til laget.
     * @return En Optional som inneholder en TeamStatistics-entitet.
     */
    Optional<TeamStatistics> findTopByTeamId(Integer teamId);
}