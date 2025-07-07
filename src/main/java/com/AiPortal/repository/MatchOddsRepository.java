package com.AiPortal.repository;

import com.AiPortal.entity.MatchOdds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchOddsRepository extends JpaRepository<MatchOdds, Long> {

    /**
     * NY METODE: Finner den første (Top) oddsen den kommer over for en gitt kamp (fixture).
     * Dette er en enkel måte å hente et eksempel på odds for en kamp.
     * @param fixtureId ID-en til kampen.
     * @return En Optional som inneholder MatchOdds hvis den finnes.
     */
    Optional<MatchOdds> findTopByFixtureId(Long fixtureId);


    // Optional<MatchOdds> findTopByFixtureIdOrderByHomeOddsDesc(Long fixtureId);
}