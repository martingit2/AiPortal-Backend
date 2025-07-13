// src/main/java/com/AiPortal/repository/PlayerMatchStatisticsRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.PlayerMatchStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerMatchStatisticsRepository extends JpaRepository<PlayerMatchStatistics, Long> {

    /**
     * Sjekker om det allerede finnes statistikk for en gitt spiller i en gitt kamp.
     * Brukes for å unngå duplikater under datainnsamling.
     *
     * @param fixtureId ID-en til kampen.
     * @param playerId  ID-en til spilleren.
     * @return true hvis en oppføring finnes, ellers false.
     */
    boolean existsByFixtureIdAndPlayerId(Long fixtureId, Integer playerId);

    /**
     * Henter all spillerstatistikk for en liste med kamp-IDer.
     * Dette er en kritisk metode for bulk-henting av data i TrainingDataService.
     *
     * @param fixtureIds En liste med kamp-IDer.
     * @return En liste med all spillerstatistikk for de gitte kampene.
     */
    List<PlayerMatchStatistics> findAllByFixtureIdIn(List<Long> fixtureIds);

}