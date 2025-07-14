// src/main/java/com/AiPortal/repository/MatchOddsRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.MatchOdds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchOddsRepository extends JpaRepository<MatchOdds, Long> {

    List<MatchOdds> findAllByFixtureIdIn(List<Long> fixtureIds);

    boolean existsByFixtureIdAndBookmakerIdAndBetName(Long fixtureId, Integer bookmakerId, String betName);

    /**
     * NY METODE: Henter alle odds-markeder for én enkelt kamp.
     */
    List<MatchOdds> findAllByFixtureId(Long fixtureId);
}