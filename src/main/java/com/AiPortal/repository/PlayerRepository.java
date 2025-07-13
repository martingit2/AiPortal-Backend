// src/main/java/com/AiPortal/repository/PlayerRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository-grensesnitt for Player-entiteten.
 * Gir standard databaseoperasjoner (CRUD) for spillere.
 */
@Repository
public interface PlayerRepository extends JpaRepository<Player, Integer> {
    // JpaRepository gir oss metoder som save(), findById(), existsById() etc. gratis.
    // Vi kan legge til egendefinerte spørringer her senere om nødvendig.
}