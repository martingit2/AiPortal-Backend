package com.AiPortal.repository;

import com.AiPortal.entity.BotConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BotConfigurationRepository extends JpaRepository<BotConfiguration, Long> {

    /**
     * Finner alle bot-konfigurasjoner for en spesifikk bruker.
     * @param userId ID-en til brukeren (fra Clerk).
     * @return En liste av BotConfiguration-objekter som tilhører brukeren.
     */
    List<BotConfiguration> findByUserId(String userId);

    /**
     * NY METODE: Finner alle bot-konfigurasjoner med en gitt status og kildetype.
     * Nødvendig for den planlagte jobben.
     * @param status Statusen å søke etter (f.eks. ACTIVE).
     * @param sourceType Kildetype å søke etter (f.eks. TWITTER).
     * @return En liste av matchende bot-konfigurasjoner.
     */
    List<BotConfiguration> findByStatusAndSourceType(BotConfiguration.BotStatus status, BotConfiguration.SourceType sourceType);
}