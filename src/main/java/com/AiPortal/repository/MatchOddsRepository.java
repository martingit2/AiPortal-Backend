package com.AiPortal.repository;

import com.AiPortal.entity.MatchOdds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchOddsRepository extends JpaRepository<MatchOdds, Long> {
   // Mer stuff kommer her
}