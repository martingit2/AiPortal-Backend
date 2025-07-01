package com.AiPortal.service;

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
@Transactional(readOnly = true) // De fleste metoder her leser bare data
public class TweetService {

    private final RawTweetDataRepository tweetRepository;

    @Autowired
    public TweetService(RawTweetDataRepository tweetRepository) {
        this.tweetRepository = tweetRepository;
    }

    /**
     * Henter en paginert liste av de sist lagrede tweets.
     * @param page Sidenummer (0-basert).
     * @param size Antall elementer per side.
     * @return En Page med RawTweetData.
     */
    public Page<RawTweetData> getLatestTweets(int page, int size) {
        // Sorterer synkende på 'createdAt' for å få de nyeste først
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return tweetRepository.findAll(pageable);
    }
}