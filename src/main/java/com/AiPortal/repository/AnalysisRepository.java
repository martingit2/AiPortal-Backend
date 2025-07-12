// src/main/java/com/AiPortal/repository/AnalysisRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    /**
     * Finner alle analyser for en spesifikk bruker, sortert etter opprettelsestidspunkt synkende.
     * @param userId Brukerens unike ID.
     * @return En liste av brukerens analyser.
     */
    List<Analysis> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * NY METODE: Finner alle analyser med et spesifikt navn.
     * Dette brukes for å sjekke om en analyse for en bestemt tweet allerede eksisterer
     * for å unngå duplikate analysejobber.
     * @param name Navnet på analysen (f.eks. "Innsiktsanalyse for tweet fra @brukernavn").
     * @return En liste med analyser som matcher navnet.
     */
    List<Analysis> findByName(String name);
}