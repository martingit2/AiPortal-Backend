// src/main/java/com/AiPortal/repository/InjuryRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.Injury;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // Importer List

@Repository
public interface InjuryRepository extends JpaRepository<Injury, Long> {

    boolean existsByFixtureIdAndPlayerId(Long fixtureId, Integer playerId);

    int countByFixtureIdAndTeamId(Long fixtureId, Integer teamId);

    /**
     * NY METODE: Henter all skadeinformasjon for en liste med kamp-IDer.
     * @param fixtureIds En liste med kamp-IDer.
     * @return En liste som inneholder all skadeinfo for alle de gitte kampene.
     */
    List<Injury> findAllByFixtureIdIn(List<Long> fixtureIds);
}