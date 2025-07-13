// src/main/java/com/AiPortal/repository/PendingFixtureChunkRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.PendingFixtureChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PendingFixtureChunkRepository extends JpaRepository<PendingFixtureChunk, Long> {

    /**
     * Finner den første tilgjengelige chunken som har status PENDING.
     * "findFirstBy..." sikrer at vi bare henter én rad.
     * "OrderByCreatedAtAsc" sikrer at vi prosesserer de eldste jobbene først (First-In, First-Out).
     *
     * @return En Optional som inneholder den neste jobben som skal kjøres.
     */
    Optional<PendingFixtureChunk> findFirstByStatusOrderByCreatedAtAsc(PendingFixtureChunk.ChunkStatus status);

    /**
     * Sjekker om det finnes noen ubehandlede chunks i databasen.
     * Nyttig for å avgjøre om "consumer"-jobben trenger å kjøre.
     *
     * @param status Statusen vi skal sjekke for (typisk PENDING).
     * @return true hvis det finnes minst én rad med den gitte statusen.
     */
    boolean existsByStatus(PendingFixtureChunk.ChunkStatus status);

}