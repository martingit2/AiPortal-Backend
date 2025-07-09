// src/main/java/com/AiPortal/repository/FixtureRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.Fixture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FixtureRepository extends JpaRepository<Fixture, Long> {

    /**
     * NY METODE: Finner alle kamper for et gitt lag i en gitt sesong,
     * sortert etter dato.
     * @param teamId ID-en til laget.
     * @param season Årstallet for sesongen.
     * @return En liste med Fixture-entiteter.
     */
    @Query("SELECT f FROM Fixture f WHERE (f.homeTeamId = :teamId OR f.awayTeamId = :teamId) AND f.season = :season ORDER BY f.date ASC")
    List<Fixture> findFixturesByTeamAndSeason(@Param("teamId") Integer teamId, @Param("season") Integer season);
}