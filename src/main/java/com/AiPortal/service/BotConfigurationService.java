package com.AiPortal.service;

import com.AiPortal.entity.BotConfiguration;
import com.AiPortal.repository.BotConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional // God praksis for service-lag som endrer data
public class BotConfigurationService {

    private final BotConfigurationRepository botRepository;

    @Autowired
    public BotConfigurationService(BotConfigurationRepository botRepository) {
        this.botRepository = botRepository;
    }

    /**
     * Henter alle bot-konfigurasjoner for en gitt bruker.
     * @param userId Clerk User ID.
     * @return Liste av bot-konfigurasjoner.
     */
    public List<BotConfiguration> getBotsForUser(String userId) {
        return botRepository.findByUserId(userId);
    }

    /**
     * Oppretter en ny bot-konfigurasjon for en bruker.
     * @param botConfiguration Konfigurasjonsobjektet som skal lagres.
     * @param userId Clerk User ID som skal eie boten.
     * @return Den lagrede bot-konfigurasjonen.
     */
    public BotConfiguration createBot(BotConfiguration botConfiguration, String userId) {
        botConfiguration.setUserId(userId); // Sikrer at eieren er satt
        botConfiguration.setStatus(BotConfiguration.BotStatus.PAUSED); // Starter som pauset som standard
        return botRepository.save(botConfiguration);
    }

    /**
     * Oppdaterer statusen på en eksisterende bot.
     * @param botId ID-en til boten som skal oppdateres.
     * @param newStatus Den nye statusen (ACTIVE/PAUSED).
     * @param userId Clerk User ID, for å verifisere eierskap.
     * @return En Optional som inneholder den oppdaterte boten, eller er tom hvis boten ikke ble funnet eller brukeren ikke eier den.
     */
    public Optional<BotConfiguration> updateBotStatus(Long botId, BotConfiguration.BotStatus newStatus, String userId) {
        // Finn boten og sjekk at den tilhører den innloggede brukeren
        return botRepository.findById(botId)
                .filter(bot -> bot.getUserId().equals(userId)) // Sikrer at du kun kan endre dine egne boter
                .map(bot -> {
                    bot.setStatus(newStatus);
                    return botRepository.save(bot);
                });
    }

    /**
     * Sletter en bot.
     * @param botId ID-en til boten som skal slettes.
     * @param userId Clerk User ID, for å verifisere eierskap.
     * @return true hvis boten ble slettet, false ellers.
     */
    public boolean deleteBot(Long botId, String userId) {
        Optional<BotConfiguration> botOptional = botRepository.findById(botId);
        if (botOptional.isPresent() && botOptional.get().getUserId().equals(userId)) {
            botRepository.deleteById(botId);
            return true;
        }
        return false;
    }
}