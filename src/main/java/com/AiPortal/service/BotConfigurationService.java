package com.AiPortal.service;

import com.AiPortal.entity.BotConfiguration;
import com.AiPortal.repository.BotConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BotConfigurationService {

    private final BotConfigurationRepository botRepository;

    @Autowired
    public BotConfigurationService(BotConfigurationRepository botRepository) {
        this.botRepository = botRepository;
    }

    public List<BotConfiguration> getBotsForUser(String userId) {
        return botRepository.findByUserId(userId);
    }

    public BotConfiguration createBot(BotConfiguration botConfiguration, String userId) {
        botConfiguration.setUserId(userId);
        botConfiguration.setStatus(BotConfiguration.BotStatus.PAUSED);
        return botRepository.save(botConfiguration);
    }

    public Optional<BotConfiguration> updateBotStatus(Long botId, BotConfiguration.BotStatus newStatus, String userId) {
        return botRepository.findById(botId)
                .filter(bot -> bot.getUserId().equals(userId))
                .map(bot -> {
                    bot.setStatus(newStatus);
                    return botRepository.save(bot);
                });
    }

    public boolean deleteBot(Long botId, String userId) {
        Optional<BotConfiguration> botOptional = botRepository.findById(botId);
        if (botOptional.isPresent() && botOptional.get().getUserId().equals(userId)) {
            botRepository.deleteById(botId);
            return true;
        }
        return false;
    }

    /**
     * NY METODE: Henter alle boter med gitt status og type, uavhengig av bruker.
     * Brukes av den planlagte jobben.
     * @param status Status å søke etter.
     * @param sourceType Kildetype å søke etter.
     * @return En liste med matchende boter.
     */
    public List<BotConfiguration> getAllBotsByStatusAndType(BotConfiguration.BotStatus status, BotConfiguration.SourceType sourceType) {
        return botRepository.findByStatusAndSourceType(status, sourceType);
    }
}