// src/main/java/com/AiPortal/repository/TeamStatisticsRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.TeamStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamStatisticsRepository extends JpaRepository<TeamStatistics, Long> {

    Optional<TeamStatistics> findByLeagueIdAndSeasonAndTeamId(int leagueId, int season, int teamId);

    Optional<TeamStatistics> findTopByTeamId(Integer teamId);


}