// src/main/java/com/AiPortal/repository/TeamStatisticsRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.TeamStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Importer Query
import org.springframework.stereotype.Repository;

import java.util.List; // Importer List
import java.util.Optional;

@Repository
public interface TeamStatisticsRepository extends JpaRepository<TeamStatistics, Long> {

    // Optimalisert metode for å hente alt i én spørring
    @Query("SELECT ts FROM TeamStatistics ts LEFT JOIN FETCH ts.sourceBot")
    List<TeamStatistics> findAllWithBot();

    Optional<TeamStatistics> findByLeagueIdAndSeasonAndTeamId(int leagueId, int season, int teamId);

    Optional<TeamStatistics> findTopByTeamId(Integer teamId);

}