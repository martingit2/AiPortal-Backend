// src/main/java/com/AiPortal/controller/PortfolioController.java
package com.AiPortal.controller;

import com.AiPortal.dto.PlacedBetDto;
import com.AiPortal.dto.PortfolioDto;
import com.AiPortal.dto.VirtualPortfolioDto;
import com.AiPortal.service.PortfolioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public ResponseEntity<List<VirtualPortfolioDto>> getAllPortfolios() {
        return ResponseEntity.ok(portfolioService.getAllPortfolios());
    }

    @PostMapping
    public ResponseEntity<VirtualPortfolioDto> createPortfolio(@RequestBody PortfolioDto dto) {
        VirtualPortfolioDto createdPortfolio = portfolioService.createPortfolio(dto);
        return new ResponseEntity<>(createdPortfolio, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/toggle-active")
    public ResponseEntity<VirtualPortfolioDto> toggleActive(@PathVariable Long id) {
        return portfolioService.toggleActiveStatus(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolio(@PathVariable Long id) {
        portfolioService.deletePortfolio(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/bets")
    public ResponseEntity<List<PlacedBetDto>> getPortfolioBets(@PathVariable Long id) {
        return ResponseEntity.ok(portfolioService.getBetsForPortfolio(id));
    }

    @GetMapping("/{id}/bets/count-pending")
    public ResponseEntity<Long> countPendingBetsForPortfolio(@PathVariable Long id) {
        long count = portfolioService.countPendingBets(id);
        return ResponseEntity.ok(count);
    }
}