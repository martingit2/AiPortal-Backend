// src/main/java/com/AiPortal/repository/MatchStatisticsRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.MatchStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Importer Query
import org.springframework.data.repository.query.Param; // Importer Param
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchStatisticsRepository extends JpaRepository<MatchStatistics, Long> {

    /**
     * Finner statistikk for et gitt lag i en gitt kamp.
     * Brukes for å unngå å lagre duplikatdata under innsamling.
     */
    Optional<MatchStatistics> findByFixtureIdAndTeamId(Long fixtureId, Integer teamId);

    /**
     * Henter all kampstatistikk (typisk for hjemme- og bortelag) for en gitt kamp-ID.
     */
    List<MatchStatistics> findAllByFixtureId(Long fixtureId);

    /**
     * Henter all kampstatistikk for en liste med kamp-IDer.
     * Dette er en mye mer effektiv måte å hente data på enn å gjøre ett og ett kall i en løkke.
     */
    List<MatchStatistics> findAllByFixtureIdIn(List<Long> fixtureIds);

    /**
     * NY METODE: Henter all kampstatistikk for en hel liga i en gitt sesong.
     * Den bruker en JPQL-spørring til å koble MatchStatistics med Fixture
     * for å filtrere på ligaId og season.
     * @param leagueId ID-en til ligaen.
     * @param season Årstallet for sesongen.
     * @return En liste med all kampstatistikk for alle kamper i den sesongen.
     */
    @Query("SELECT ms FROM MatchStatistics ms JOIN Fixture f ON ms.fixtureId = f.id WHERE f.leagueId = :leagueId AND f.season = :season")
    List<MatchStatistics> findAllByLeagueAndSeason(@Param("leagueId") int leagueId, @Param("season") int season);
}