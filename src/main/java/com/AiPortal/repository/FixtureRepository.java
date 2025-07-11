// src/main/java/com/AiPortal/repository/FixtureRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.Fixture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // VIKTIG: Importer Page og Pageable
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant; // Importer Instant
import java.util.List;

@Repository
public interface FixtureRepository extends JpaRepository<Fixture, Long> {

    /**
     * Finner alle kamper for et gitt lag i en gitt sesong,
     * sortert etter dato.
     * @param teamId ID-en til laget.
     * @param season Årstallet for sesongen.
     * @return En liste med Fixture-entiteter.
     */
    @Query("SELECT f FROM Fixture f WHERE (f.homeTeamId = :teamId OR f.awayTeamId = :teamId) AND f.season = :season ORDER BY f.date ASC")
    List<Fixture> findFixturesByTeamAndSeason(@Param("teamId") Integer teamId, @Param("season") Integer season);

    /**
     * Finner de N siste ferdigspilte kampene for et lag i en gitt sesong.
     * Den ser etter kamper som har status FT (Full Time), AET (After Extra Time), eller PEN (Penalties).
     * @param teamId ID-en til laget.
     * @param season Årstallet for sesongen.
     * @param pageable Et Pageable-objekt som begrenser resultatet (f.eks. PageRequest.of(0, 10)).
     * @return En liste med de N siste Fixture-entitetene.
     */
    @Query("SELECT f FROM Fixture f WHERE (f.homeTeamId = :teamId OR f.awayTeamId = :teamId) AND f.season = :season AND f.status IN ('FT', 'AET', 'PEN') ORDER BY f.date DESC")
    List<Fixture> findLastNCompletedFixturesByTeamAndSeason(@Param("teamId") Integer teamId, @Param("season") Integer season, Pageable pageable);

    /**
     * NY METODE: Finner en paginert liste over kommende kamper.
     * Kriterier: Kampdato er etter nåværende tidspunkt.
     * Sortering: Kampdato stigende (nærmeste kamper først).
     * @param now Nåværende tidspunkt, sendes inn fra service-laget.
     * @param pageable Pagineringinformasjon.
     * @return En Page med kommende Fixture-entiteter.
     */
    Page<Fixture> findByDateAfterOrderByDateAsc(Instant now, Pageable pageable);

    /**
     * NY METODE: Finner en paginert liste over nylig spilte kamper.
     * Kriterier: Kampdato er før nåværende tidspunkt OG status indikerer ferdigspilt.
     * Sortering: Kampdato synkende (nyeste resultater først).
     * @param now Nåværende tidspunkt, sendes inn fra service-laget.
     * @param finishedStatus En liste med statuser som betyr "ferdigspilt".
     * @param pageable Pagineringinformasjon.
     * @return En Page med nylig spilte Fixture-entiteter.
     */
    Page<Fixture> findByDateBeforeAndStatusInOrderByDateDesc(Instant now, List<String> finishedStatus, Pageable pageable);
}