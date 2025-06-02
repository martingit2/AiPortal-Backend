// src/main/java/com/AiPortal/config/SecurityConfig.java
package com.AiPortal.config; // Juster til din faktiske pakke

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Konfigurer CORS - Viktig for kommunikasjon med frontend på en annen port/domene
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Deaktiver CSRF siden vi bruker token-basert autentisering (JWTs er stateless)
                .csrf(csrf -> csrf.disable())

                // Konfigurer session management til STATELESS siden JWTs er stateless
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(authorize -> authorize
                        // Eksempel på offentlige endepunkter (krever ikke autentisering)
                        // Tilpass disse til dine faktiske offentlige stier
                        .requestMatchers(HttpMethod.GET, "/api/v1/public/**").permitAll()
                        .requestMatchers("/api/v1/hello-public").permitAll() // Hvis du har en slik for testing

                        // Alle andre forespørsler til /api/v1/** krever autentisering
                        .requestMatchers("/api/v1/**").authenticated()

                        // For alle andre forespørsler som ikke er definert over (f.eks. for statiske filer, feilsider)
                        // kan du velge å tillate dem eller kreve autentisering.
                        // For en ren API-backend, kan du ofte sette .anyRequest().denyAll() eller .anyRequest().authenticated()
                        // Her tillater vi andre forespørsler, men du kan stramme inn dette.
                        .anyRequest().permitAll()
                )
                // Konfigurer OAuth2 Resource Server til å validere JWTs
                // withDefaults() vil bruke konfigurasjonen fra application.properties (issuer-uri)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Definer hvilke origins (din React-apps URL) som er tillatt
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173", // Din Vite dev server
                "https://aracanix-din-frontend.vercel.app" // Eksempel på produksjons-URL for frontend
        ));
        // Definer hvilke metoder som er tillatt
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // Definer hvilke headere som er tillatt
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "X-Requested-With"));
        // Tillat credentials (f.eks. cookies, authorization headers)
        configuration.setAllowCredentials(true);
        // Hvor lenge pre-flight OPTIONS-forespørselen kan caches (i sekunder)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Bruk denne konfigurasjonen for alle stier
        return source;
    }
}