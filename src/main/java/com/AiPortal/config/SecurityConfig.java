// src/main/java/com/AiPortal/config/SecurityConfig.java
package com.AiPortal.config;

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
                        // TILLAT offentlige, admin, og ml-data-endepunkter uten token
                        .requestMatchers(
                                "/api/v1/public/**",
                                "/api/v1/admin/**",
                                "/api/v1/ml-data/**" // Tillater kall fra treningsskriptet
                        ).permitAll()

                        // Alle andre forespørsler som matcher /api/v1/** krever et gyldig autentisering (JWT-token).
                        .requestMatchers("/api/v1/**").authenticated()

                        // For alle andre forespørsler som ikke er dekket over, tillat dem.
                        .anyRequest().permitAll()
                )
                // 5. Konfigurer serveren til å validere innkommende tokens som JWTs.
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

        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173", // For din lokale Vite dev server
                "https://aracanix-din-frontend.vercel.app" // Eksempel på din produksjons-URL
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}