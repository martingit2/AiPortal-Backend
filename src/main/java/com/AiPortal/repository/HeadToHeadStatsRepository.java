// src/main/java/com/AiPortal/repository/HeadToHeadStatsRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.HeadToHeadStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HeadToHeadStatsRepository extends JpaRepository<HeadToHeadStats, Long> {

    // Metode for å sjekke om H2H-data allerede eksisterer for en kamp
    boolean existsByFixtureId(Long fixtureId);

    // Metode for å hente H2H-data for mange kamper samtidig
    List<HeadToHeadStats> findAllByFixtureIdIn(List<Long> fixtureIds);
}