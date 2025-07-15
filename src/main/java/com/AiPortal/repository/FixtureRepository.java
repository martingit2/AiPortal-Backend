// src/main/java/com/AiPortal/repository/FixtureRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.Fixture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface FixtureRepository extends JpaRepository<Fixture, Long> {

    @Query("SELECT f FROM Fixture f WHERE (f.homeTeamId = :teamId OR f.awayTeamId = :teamId) AND f.season = :season ORDER BY f.date ASC")
    List<Fixture> findFixturesByTeamAndSeason(@Param("teamId") Integer teamId, @Param("season") Integer season);

    @Query("SELECT f FROM Fixture f WHERE (f.homeTeamId = :teamId OR f.awayTeamId = :teamId) AND f.season = :season AND f.status IN ('FT', 'AET', 'PEN') ORDER BY f.date DESC")
    List<Fixture> findLastNCompletedFixturesByTeamAndSeason(@Param("teamId") Integer teamId, @Param("season") Integer season, Pageable pageable);

    Page<Fixture> findByDateAfterOrderByDateAsc(Instant now, Pageable pageable);
    List<Fixture> findAllByDateAfterOrderByDateAsc(Instant now);
    Page<Fixture> findByDateBeforeAndStatusInOrderByDateDesc(Instant now, List<String> finishedStatus, Pageable pageable);
    List<Fixture> findByStatusIn(List<String> statuses);

    @Query("SELECT f FROM Fixture f WHERE (f.homeTeamId = :teamId OR f.awayTeamId = :teamId) AND f.status IN ('FT', 'AET', 'PEN') AND f.date < :beforeDate ORDER BY f.date DESC")
    List<Fixture> findLastNCompletedFixturesByTeamBeforeDate(@Param("teamId") Integer teamId, @Param("beforeDate") Instant beforeDate, Pageable pageable);

    Optional<Fixture> findFirstByHomeTeamNameAndAwayTeamNameAndDateBetween(String homeTeam, String awayTeam, Instant start, Instant end);
    List<Fixture> findAllByDateBetween(Instant start, Instant end);

    /**
     * NY METODE: Finner ufullstendige fixtures (opprettet av Pinnacle-boten)
     * ved å se etter rader hvor team ID-er er null.
     */
    List<Fixture> findByHomeTeamIdIsNullAndAwayTeamIdIsNull();
}