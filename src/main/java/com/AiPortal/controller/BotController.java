package com.AiPortal.controller;

import com.AiPortal.entity.BotConfiguration;
import com.AiPortal.service.BotConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bots") // Base-sti for alle bot-relaterte endepunkter
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true") // Tillater kall fra React-appen
public class BotController {

    private final BotConfigurationService botService;

    @Autowired
    public BotController(BotConfigurationService botService) {
        this.botService = botService;
    }

    /**
     * Henter alle bot-konfigurasjoner for den innloggede brukeren.
     * @param jwt Injisert av Spring Security, inneholder brukerens ID (sub).
     * @return En liste av brukerens bot-konfigurasjoner.
     */
    @GetMapping
    public ResponseEntity<List<BotConfiguration>> getUserBots(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject(); // Hent bruker-ID fra tokenet
        List<BotConfiguration> bots = botService.getBotsForUser(userId);
        return ResponseEntity.ok(bots);
    }

    /**
     * Oppretter en ny bot-konfigurasjon.
     * @param botConfiguration Data for den nye boten fra request body.
     * @param jwt Injisert for å hente eierens ID.
     * @return Den nyopprettede bot-konfigurasjonen.
     */
    @PostMapping
    public ResponseEntity<BotConfiguration> createBot(@RequestBody BotConfiguration botConfiguration, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        BotConfiguration createdBot = botService.createBot(botConfiguration, userId);
        return new ResponseEntity<>(createdBot, HttpStatus.CREATED);
    }

    /**
     * Oppdaterer statusen på en eksisterende bot.
     * @param id ID-en til boten fra URL-stien.
     * @param statusMap En map som inneholder den nye statusen, f.eks. { "status": "ACTIVE" }.
     * @param jwt Injisert for å verifisere eierskap.
     * @return Den oppdaterte bot-konfigurasjonen, eller 404/403 hvis ikke funnet/ikke eier.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<BotConfiguration> updateBotStatus(@PathVariable Long id,
                                                            @RequestBody Map<String, String> statusMap,
                                                            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        try {
            // Konverter strengen fra request body til BotStatus enum
            BotConfiguration.BotStatus newStatus = BotConfiguration.BotStatus.valueOf(statusMap.get("status").toUpperCase());

            return botService.updateBotStatus(id, newStatus, userId)
                    .map(ResponseEntity::ok) // Hvis boten ble funnet og oppdatert, returner 200 OK
                    .orElse(ResponseEntity.notFound().build()); // Ellers, returner 404 Not Found
        } catch (IllegalArgumentException e) {
            // Håndterer tilfellet der status-strengen er ugyldig
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Sletter en bot-konfigurasjon.
     * @param id ID-en til boten fra URL-stien.
     * @param jwt Injisert for å verifisere eierskap.
     * @return En 204 No Content respons hvis slettingen var vellykket, ellers 404.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBot(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        boolean deleted = botService.deleteBot(id, userId);
        if (deleted) {
            return ResponseEntity.noContent().build(); // 204 No Content - standard for vellykket sletting
        } else {
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
    }
}