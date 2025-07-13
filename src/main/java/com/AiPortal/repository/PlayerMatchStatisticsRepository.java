// src/main/java/com/AiPortal/repository/PlayerMatchStatisticsRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.PlayerMatchStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerMatchStatisticsRepository extends JpaRepository<PlayerMatchStatistics, Long> {

    /**
     * Sjekker om det allerede finnes statistikk for en gitt spiller i en gitt kamp.
     * Brukes for å unngå duplikater under datainnsamling.
     */
    boolean existsByFixtureIdAndPlayerId(Long fixtureId, Integer playerId);

    /**
     * Henter all spillerstatistikk for en liste med kamp-IDer.
     * Dette er en kritisk metode for bulk-henting av data i TrainingDataService.
     */
    List<PlayerMatchStatistics> findAllByFixtureIdIn(List<Long> fixtureIds);

    /**
     * NY, MANGLENDE METODE: Henter all spillerstatistikk for én enkelt kamp-ID.
     * Denne brukes av StatisticsService for å vise statistikk i modalen.
     *
     * @param fixtureId ID-en til kampen.
     * @return En liste med all spillerstatistikk for den gitte kampen.
     */
    List<PlayerMatchStatistics> findAllByFixtureId(Long fixtureId);

}