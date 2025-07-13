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

@Repository
public interface FixtureRepository extends JpaRepository<Fixture, Long> {

    @Query("SELECT f FROM Fixture f WHERE (f.homeTeamId = :teamId OR f.awayTeamId = :teamId) AND f.season = :season ORDER BY f.date ASC")
    List<Fixture> findFixturesByTeamAndSeason(@Param("teamId") Integer teamId, @Param("season") Integer season);

    @Query("SELECT f FROM Fixture f WHERE (f.homeTeamId = :teamId OR f.awayTeamId = :teamId) AND f.season = :season AND f.status IN ('FT', 'AET', 'PEN') ORDER BY f.date DESC")
    List<Fixture> findLastNCompletedFixturesByTeamAndSeason(@Param("teamId") Integer teamId, @Param("season") Integer season, Pageable pageable);

    Page<Fixture> findByDateAfterOrderByDateAsc(Instant now, Pageable pageable);

    Page<Fixture> findByDateBeforeAndStatusInOrderByDateDesc(Instant now, List<String> finishedStatus, Pageable pageable);

    /**
     * NY METODE: Finner alle kamper med en status som er i den gitte listen.
     * Brukes for å hente alle ferdigspilte kamper for treningssettet.
     * @param statuses En liste med status-strenger (f.eks. "FT", "AET").
     * @return En liste med alle kamper som matcher.
     */
    List<Fixture> findByStatusIn(List<String> statuses);

    /**
     * NY METODE: Finner de N siste ferdigspilte kampene for et lag FØR en gitt dato.
     * Dette er essensielt for å unngå data-lekkasje i ML-modellen, slik at vi kun
     * bruker historisk data som var tilgjengelig FØR den aktuelle kampen ble spilt.
     * @param teamId ID-en til laget.
     * @param beforeDate Datoen vi skal se før.
     * @param pageable Paginering for å begrense til N siste kamper.
     * @return En liste med de N siste kampene før 'beforeDate'.
     */
    @Query("SELECT f FROM Fixture f WHERE (f.homeTeamId = :teamId OR f.awayTeamId = :teamId) AND f.status IN ('FT', 'AET', 'PEN') AND f.date < :beforeDate ORDER BY f.date DESC")
    List<Fixture> findLastNCompletedFixturesByTeamBeforeDate(@Param("teamId") Integer teamId, @Param("beforeDate") Instant beforeDate, Pageable pageable);
}