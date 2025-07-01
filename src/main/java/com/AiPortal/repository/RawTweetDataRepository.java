package com.AiPortal.repository;

import com.AiPortal.entity.RawTweetData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RawTweetDataRepository extends JpaRepository<RawTweetData, Long> {
    // Denne metoden lar oss enkelt sjekke om en tweet allerede finnes i databasen
    boolean existsByTweetId(String tweetId);
}