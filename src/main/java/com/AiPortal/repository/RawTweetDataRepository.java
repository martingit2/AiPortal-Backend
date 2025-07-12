// src/main/java/com/AiPortal/repository/RawTweetDataRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.RawTweetData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant; // Importer Instant
import java.util.List;    // Importer List

@Repository
public interface RawTweetDataRepository extends JpaRepository<RawTweetData, Long> {

    /**
     * Sjekker om en tweet med en gitt, unik tweet-ID allerede finnes i databasen.
     * Brukes for å unngå duplikater under datainnhenting.
     * @param tweetId Den unike ID-en fra Twitter.
     * @return true hvis tweeten finnes, ellers false.
     */
    boolean existsByTweetId(String tweetId);

    /**
     * NY METODE: Finner alle tweets som inneholder et gitt nøkkelord (uavhengig av store/små bokstaver)
     * og som er postet etter et gitt tidspunkt.
     *
     * Denne metoden er kjernen for å koble tweets til kamper. Vi kan søke etter et lags navn
     * i tidsrommet før kampstart.
     *
     * @param keyword Søkeordet vi leter etter i tweet-innholdet (f.eks. "Manchester United").
     * @param afterDate Et tidspunkt. Metoden vil kun returnere tweets som er nyere enn dette.
     * @return En liste med RawTweetData-entiteter som matcher kriteriene.
     */
    List<RawTweetData> findByContentContainingIgnoreCaseAndTweetedAtAfter(String keyword, Instant afterDate);
}