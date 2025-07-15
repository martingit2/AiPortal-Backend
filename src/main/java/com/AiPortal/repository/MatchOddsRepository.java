// src/main/java/com/AiPortal/repository/MatchOddsRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.MatchOdds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MatchOddsRepository extends JpaRepository<MatchOdds, Long> {

    List<MatchOdds> findAllByFixtureIdIn(List<Long> fixtureIds);

    boolean existsByFixtureIdAndBookmakerIdAndBetName(Long fixtureId, Integer bookmakerId, String betName);

    List<MatchOdds> findAllByFixtureId(Long fixtureId);

    /**
     * NY METODE: Finner alle unike ID-er for kommende kamper som har minst én
     * odds-oppføring i databasen.
     * @param now Det nåværende tidspunktet.
     * @return En liste med Fixture ID-er.
     */
    @Query("SELECT DISTINCT mo.fixture.id FROM MatchOdds mo WHERE mo.fixture.date > :now")
    List<Long> findDistinctFixtureIdsWithUpcomingOdds(@Param("now") Instant now);
}