// src/main/java/com/AiPortal/config/SecurityConfig.java
package com.AiPortal.config; // Pass på at denne matcher din faktiske pakkestruktur

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Konfigurer CORS ved hjelp av din custom bean.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. Deaktiver CSRF. Nødvendig for stateless API-er som bruker tokens.
                .csrf(csrf -> csrf.disable())

                // 3. Sett session management til STATELESS. Ingen server-side sessions vil bli opprettet.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Definer autorisasjonsregler for HTTP-forespørsler.
                .authorizeHttpRequests(authorize -> authorize
                        // KORRIGERT REGEL: Tillat ALLE metoder (GET, POST, PUT, etc.) til stier under /api/v1/public/
                        .requestMatchers("/api/v1/public/**").permitAll()

                        // Alle andre forespørsler som matcher /api/v1/** krever en gyldig autentisering (JWT-token).
                        .requestMatchers("/api/v1/**").authenticated()

                        // For alle andre forespørsler som ikke er dekket over, tillat dem.
                        // Dette kan være nyttig for f.eks. Spring Boot Actuator-endepunkter eller standard feilsider.
                        // Kan strammes inn til .denyAll() eller .authenticated() ved behov.
                        .anyRequest().permitAll()
                )
                // 5. Konfigurer serveren til å validere innkommende tokens som JWTs.
                // withDefaults() bruker konfigurasjonen fra application.properties (issuer-uri).
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));

        return http.build();
    }

    /**
     * Bean for å konfigurere CORS globalt for hele applikasjonen.
     * @return CorsConfigurationSource
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Definer hvilke origins (din React-apps URL) som er tillatt.
        // VIKTIG: Ikke bruk "*" i produksjon hvis du bruker allowCredentials(true).
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173", // For din lokale Vite dev server
                "https://aracanix-din-frontend.vercel.app" // Eksempel på din produksjons-URL
                // Legg til flere URL-er her om nødvendig
        ));

        // Definer hvilke HTTP-metoder som er tillatt.
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Definer hvilke HTTP-headere som kan sendes med forespørselen.
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "X-Requested-With"));

        // Tillat at nettleseren sender credentials (f.eks. cookies, authorization headers med tokens).
        configuration.setAllowCredentials(true);

        // Hvor lenge (i sekunder) resultatet av en pre-flight (OPTIONS) forespørsel kan caches.
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Bruk denne CORS-konfigurasjonen for alle stier ("/**") i applikasjonen.
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}