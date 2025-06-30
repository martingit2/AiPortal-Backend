package com.AiPortal.controller;

import com.AiPortal.entity.TestEntity;
import com.AiPortal.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * En REST Controller som håndterer API-forespørsler.
 * Inkluderer offentlige, beskyttede, og database-relaterte endepunkter.
 */
@RestController
@RequestMapping("/api/v1") // Setter base-stien for alle endepunkter i denne controlleren
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true") // Tillater kall fra din lokale React-app
public class HelloController {

    private final TestService testService;

    /**
     * Bruker "Constructor Injection" for å injisere TestService.
     * Dette er ansett som beste praksis i Spring.
     * @param testService Tjenesten som håndterer logikk for TestEntity.
     */
    @Autowired
    public HelloController(TestService testService) {
        this.testService = testService;
    }


    // --- OFFENTLIGE TEST-ENDEPUNKTER ---

    /**
     * Et offentlig endepunkt for å sjekke status.
     * Kan nås uten autentisering basert på SecurityConfig.
     */
    @GetMapping("/public/status")
    public Map<String, String> getStatus() {
        return Map.of("status", "Backend (demo1) er oppe og kjører!");
    }


    // --- ENDEPUNKTER FOR DATABASETESTING (OFFENTLIGE FOR ENKEL TESTING) ---

    /**
     * Offentlig endepunkt for å opprette en ny test-item i databasen.
     * Mottar en JSON-body. Eksempel: { "name": "Min første test" }
     * @param testEntityRequest JSON-objektet fra forespørselen, mappet til en TestEntity.
     * @return Den lagrede TestEntity med generert ID og en 200 OK status.
     */
    @PostMapping("/public/testitems")
    public ResponseEntity<TestEntity> createTestItem(@RequestBody TestEntity testEntityRequest) {
        // I en ekte app ville man ofte brukt en DTO (Data Transfer Object) her for sikkerhet og fleksibilitet.
        TestEntity savedEntity = testService.saveTestEntity(testEntityRequest.getName());
        return ResponseEntity.ok(savedEntity);
    }

    /**
     * Offentlig endepunkt for å hente alle test-items fra databasen.
     * @return En liste av alle TestEntity-objekter og en 200 OK status.
     */
    @GetMapping("/public/testitems")
    public ResponseEntity<List<TestEntity>> getAllTestItems() {
        List<TestEntity> entities = testService.getAllTestEntities();
        return ResponseEntity.ok(entities);
    }


    // --- BESKYTTET ENDEPUNKT FOR SIKKERHETSTESTING ---

    /**
     * Beskyttet endepunkt som krever et gyldig JWT-token (fra Clerk).
     * Hvis tokenet er gyldig, injiserer Spring Security det som et Jwt-objekt.
     * @param jwt Det validerte JWT-tokenet som inneholder brukerinformasjon (claims).
     * @return Et kart med informasjon hentet fra JWT-tokenet.
     */
    @GetMapping("/secure/me")
    public ResponseEntity<Object> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        // @AuthenticationPrincipal Jwt jwt er magien som gir oss tokenet.
        // Spring Security validerer tokenet (signatur, utløpsdato, issuer) FØR denne metoden kalles.

        // Hent claims (informasjon) fra JWT-tokenet
        String userId = jwt.getSubject(); // "sub" claim, som Clerk setter til brukerens ID.
        Map<String, Object> allClaims = jwt.getClaims();

        // Lag et map for å returnere relevant brukerinfo
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId_from_token", userId);
        userInfo.put("token_issued_at", jwt.getIssuedAt());
        userInfo.put("token_expires_at", jwt.getExpiresAt());
        userInfo.put("all_claims_from_token", allClaims); // Returnerer alle claims for enkel feilsøking/inspeksjon

        return ResponseEntity.ok(userInfo);
    }
}