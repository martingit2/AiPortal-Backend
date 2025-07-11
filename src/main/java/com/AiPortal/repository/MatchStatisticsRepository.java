// src/main/java/com/AiPortal/repository/MatchStatisticsRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.MatchStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchStatisticsRepository extends JpaRepository<MatchStatistics, Long> {

    /**
     * Finner statistikk for et gitt lag i en gitt kamp.
     * Brukes for å unngå å lagre duplikatdata under innsamling.
     * @param fixtureId ID-en til kampen.
     * @param teamId ID-en til laget.
     * @return En Optional som inneholder statistikken hvis den finnes.
     */
    Optional<MatchStatistics> findByFixtureIdAndTeamId(Long fixtureId, Integer teamId);

    /**
     * Henter all kampstatistikk (typisk for hjemme- og bortelag) for en gitt kamp-ID.
     * @param fixtureId ID-en til kampen.
     * @return En liste med MatchStatistics-entiteter.
     */
    List<MatchStatistics> findAllByFixtureId(Long fixtureId);

    /**
     * NY METODE: Henter all kampstatistikk for en liste med kamp-IDer.
     * Dette er en mye mer effektiv måte å hente data på enn å gjøre ett og ett kall i en løkke.
     * @param fixtureIds En liste med kamp-IDer.
     * @return En liste som inneholder all statistikk for alle de gitte kampene.
     */
    List<MatchStatistics> findAllByFixtureIdIn(List<Long> fixtureIds);
}