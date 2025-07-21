// src/main/java/com/AiPortal/service/PortfolioService.java
package com.AiPortal.service;

import com.AiPortal.dto.PlacedBetDto;
import com.AiPortal.dto.PortfolioDto;
import com.AiPortal.dto.VirtualPortfolioDto;
import com.AiPortal.entity.AnalysisModel;
import com.AiPortal.entity.Fixture;
import com.AiPortal.entity.PlacedBet;
import com.AiPortal.entity.VirtualPortfolio;
import com.AiPortal.repository.AnalysisModelRepository;
import com.AiPortal.repository.FixtureRepository;
import com.AiPortal.repository.PlacedBetRepository;
import com.AiPortal.repository.VirtualPortfolioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PortfolioService {

    private final VirtualPortfolioRepository portfolioRepository;
    private final AnalysisModelRepository modelRepository;
    private final PlacedBetRepository placedBetRepository;
    private final FixtureRepository fixtureRepository;

    public PortfolioService(VirtualPortfolioRepository portfolioRepository,
                            AnalysisModelRepository modelRepository,
                            PlacedBetRepository placedBetRepository,
                            FixtureRepository fixtureRepository) {
        this.portfolioRepository = portfolioRepository;
        this.modelRepository = modelRepository;
        this.placedBetRepository = placedBetRepository;
        this.fixtureRepository = fixtureRepository;
    }

    @Transactional(readOnly = true)
    public List<VirtualPortfolioDto> getAllPortfolios() {
        return portfolioRepository.findAllWithModel().stream()
                .map(VirtualPortfolioDto::new)
                .collect(Collectors.toList());
    }

    public VirtualPortfolioDto createPortfolio(PortfolioDto dto) {
        AnalysisModel model = modelRepository.findById(dto.getModelId())
                .orElseThrow(() -> new EntityNotFoundException("Finner ikke modell med ID: " + dto.getModelId()));

        VirtualPortfolio portfolio = new VirtualPortfolio();
        portfolio.setName(dto.getName());
        portfolio.setStartingBalance(dto.getStartingBalance());
        portfolio.setCurrentBalance(dto.getStartingBalance());
        portfolio.setDiscordWebhookUrl(dto.getDiscordWebhookUrl());
        portfolio.setModel(model);
        portfolio.setActive(false);

        VirtualPortfolio savedPortfolio = portfolioRepository.save(portfolio);
        return new VirtualPortfolioDto(savedPortfolio);
    }

    public Optional<VirtualPortfolioDto> toggleActiveStatus(Long portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .map(portfolio -> {
                    portfolio.setActive(!portfolio.isActive());
                    VirtualPortfolio updatedPortfolio = portfolioRepository.save(portfolio);
                    // Last inn på nytt med modellen for å returnere en komplett DTO
                    VirtualPortfolio reloadedPortfolio = portfolioRepository.findAllWithModel().stream()
                            .filter(p -> p.getId().equals(updatedPortfolio.getId()))
                            .findFirst()
                            .orElse(updatedPortfolio);
                    return new VirtualPortfolioDto(reloadedPortfolio);
                });
    }

    public void deletePortfolio(Long portfolioId) {
        if (portfolioRepository.existsById(portfolioId)) {
            // Slett alle bets knyttet til porteføljen først for å unngå constraint-feil
            List<PlacedBet> betsToDelete = placedBetRepository.findByPortfolioIdOrderByPlacedAtDesc(portfolioId);
            placedBetRepository.deleteAll(betsToDelete);

            portfolioRepository.deleteById(portfolioId);
        }
    }

    @Transactional(readOnly = true)
    public List<PlacedBetDto> getBetsForPortfolio(Long portfolioId) {
        List<PlacedBet> bets = placedBetRepository.findByPortfolioIdOrderByPlacedAtDesc(portfolioId);
        if (bets.isEmpty()) {
            return List.of();
        }

        List<Long> fixtureIds = bets.stream()
                .map(PlacedBet::getFixtureId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Fixture> fixtureMap = fixtureRepository.findAllById(fixtureIds).stream()
                .collect(Collectors.toMap(Fixture::getId, f -> f));

        return bets.stream()
                .map(bet -> {
                    Fixture fixture = fixtureMap.get(bet.getFixtureId());
                    if (fixture == null) {
                        fixture = new Fixture();
                        fixture.setHomeTeamName("Ukjent");
                        fixture.setAwayTeamName("Lag");
                    }
                    return new PlacedBetDto(bet, fixture);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countPendingBets(Long portfolioId) {
        return placedBetRepository.countByPortfolioIdAndStatus(portfolioId, PlacedBet.BetStatus.PENDING);
    }
}