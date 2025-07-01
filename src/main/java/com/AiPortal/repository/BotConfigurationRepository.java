package com.AiPortal.repository;

import com.AiPortal.entity.BotConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BotConfigurationRepository extends JpaRepository<BotConfiguration, Long> {

    /**
     * Egendefinert metode for å finne alle bot-konfigurasjoner for en spesifikk bruker.
     * Spring Data JPA vil automatisk generere SQL-spørringen basert på metodenavnet.
     * @param userId ID-en til brukeren (fra Clerk).
     * @return En liste av BotConfiguration-objekter som tilhører brukeren.
     */
    List<BotConfiguration> findByUserId(String userId);

}