// src/main/java/com/AiPortal/repository/InjuryRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.Injury;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InjuryRepository extends JpaRepository<Injury, Long> {

    /**
     * Sjekker om en skade for en spesifikk spiller i en spesifikk kamp allerede er registrert.
     * Dette forhindrer at vi lagrer den samme skadeinformasjonen flere ganger
     * hvis datainnhentings-boten kjøres på nytt.
     *
     * @param fixtureId ID-en til kampen.
     * @param playerId ID-en til spilleren.
     * @return true hvis en slik skadeoppføring allerede finnes, ellers false.
     */
    boolean existsByFixtureIdAndPlayerId(Long fixtureId, Integer playerId);

}