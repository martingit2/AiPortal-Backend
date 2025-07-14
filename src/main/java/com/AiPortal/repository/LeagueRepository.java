// src/main/java/com/AiPortal/repository/LeagueRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeagueRepository extends JpaRepository<League, Integer> {}