// src/main/java/com/AiPortal/repository/PendingFixtureChunkRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.PendingFixtureChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PendingFixtureChunkRepository extends JpaRepository<PendingFixtureChunk, Long> {

    Optional<PendingFixtureChunk> findFirstByStatusOrderByCreatedAtAsc(PendingFixtureChunk.ChunkStatus status);

    boolean existsByStatus(PendingFixtureChunk.ChunkStatus status);

    /**
     * NY METODE: Sjekker om det finnes aktive (pending eller processing) chunks
     * for en gitt kildeidentifikator. Dette forhindrer at vi starter dupliserte jobber.
     */
    boolean existsBySourceIdentifierAndStatusIn(String sourceIdentifier, List<PendingFixtureChunk.ChunkStatus> statuses);

    /**
     * NY METODE: Finner "foreldreløse" jobber som har status PROCESSING,
     * men som ikke har blitt oppdatert på en stund (indikator på at de har krasjet).
     */
    Optional<PendingFixtureChunk> findFirstByStatusAndCreatedAtBefore(PendingFixtureChunk.ChunkStatus status, Instant timeout);
}