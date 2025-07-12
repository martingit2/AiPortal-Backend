// src/main/java/com/AiPortal/service/TweetService.java
package com.AiPortal.service;

import com.AiPortal.dto.TweetDto;
import com.AiPortal.entity.RawTweetData;
import com.AiPortal.repository.RawTweetDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional // Gjør readOnly = false på klassenivå for å tillate sletting
public class TweetService {

    private final RawTweetDataRepository tweetRepository;

    @Autowired
    public TweetService(RawTweetDataRepository tweetRepository) {
        this.tweetRepository = tweetRepository;
    }

    /**
     * Henter en paginert liste av de sist lagrede tweets som en DTO.
     * Denne metoden er fortsatt read-only.
     * @param page Sidenummer (0-basert).
     * @param size Antall elementer per side.
     * @return En Page med TweetDto.
     */
    @Transactional(readOnly = true)
    public Page<TweetDto> getLatestTweets(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<RawTweetData> rawDataPage = tweetRepository.findAll(pageable);

        return rawDataPage.map(tweet -> new TweetDto(
                tweet.getId(),
                tweet.getAuthorUsername(),
                tweet.getContent(),
                tweet.getTweetedAt(),
                tweet.getSourceBot() != null ? tweet.getSourceBot().getName() : "Ukjent Kilde"
        ));
    }

    /**
     * NY METODE: Sletter en tweet basert på dens database-ID.
     * En forbedring her ville vært å legge til en sikkerhetssjekk for å
     * verifisere at brukeren som gjør kallet eier boten som hentet tweeten.
     * @param id Den primære ID-en til RawTweetData-entiteten.
     * @return true hvis tweeten ble funnet og slettet, false hvis den ikke fantes.
     */
    public boolean deleteTweetById(Long id) {
        if (tweetRepository.existsById(id)) {
            tweetRepository.deleteById(id);
            return true;
        }
        return false;
    }
}