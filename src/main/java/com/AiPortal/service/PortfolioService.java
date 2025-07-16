// src/main/java/com/AiPortal/service/PortfolioService.java
package com.AiPortal.service;

import com.AiPortal.dto.PortfolioDto;
import com.AiPortal.entity.AnalysisModel;
import com.AiPortal.entity.VirtualPortfolio;
import com.AiPortal.repository.AnalysisModelRepository;
import com.AiPortal.repository.VirtualPortfolioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PortfolioService {

    private final VirtualPortfolioRepository portfolioRepository;
    private final AnalysisModelRepository modelRepository;

    public PortfolioService(VirtualPortfolioRepository portfolioRepository, AnalysisModelRepository modelRepository) {
        this.portfolioRepository = portfolioRepository;
        this.modelRepository = modelRepository;
    }

    @Transactional(readOnly = true)
    public List<VirtualPortfolio> getAllPortfolios() {
        return portfolioRepository.findAllWithModel();
    }

    public VirtualPortfolio createPortfolio(PortfolioDto dto) {
        AnalysisModel model = modelRepository.findById(dto.getModelId())
                .orElseThrow(() -> new EntityNotFoundException("Finner ikke modell med ID: " + dto.getModelId()));

        VirtualPortfolio portfolio = new VirtualPortfolio();
        portfolio.setName(dto.getName());
        portfolio.setStartingBalance(dto.getStartingBalance());
        portfolio.setCurrentBalance(dto.getStartingBalance());
        portfolio.setDiscordWebhookUrl(dto.getDiscordWebhookUrl());
        portfolio.setModel(model);
        portfolio.setActive(false);

        return portfolioRepository.save(portfolio);
    }

    public Optional<VirtualPortfolio> toggleActiveStatus(Long portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .map(portfolio -> {
                    portfolio.setActive(!portfolio.isActive());
                    return portfolioRepository.save(portfolio);
                });
    }

    public void deletePortfolio(Long portfolioId) {
        portfolioRepository.deleteById(portfolioId);
    }
}