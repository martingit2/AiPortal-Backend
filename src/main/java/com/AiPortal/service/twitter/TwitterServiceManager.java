// src/main/java/com/AiPortal/service/twitter/TwitterServiceManager.java
package com.AiPortal.service.twitter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TwitterServiceManager {

    private static final Logger log = LoggerFactory.getLogger(TwitterServiceManager.class);
    private final List<TwitterServiceProvider> providers;
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    public TwitterServiceManager(List<TwitterServiceProvider> providers) {
        this.providers = providers != null ? providers : Collections.emptyList();
        if (this.providers.isEmpty()) {
            log.warn("Ingen TwitterServiceProvider-bønner funnet. Twitter-funksjonalitet vil være deaktivert.");
        } else {
            log.info("Laster inn {} Twitter-leverandører: {}", this.providers.size(), this.providers.stream().map(TwitterServiceProvider::getProviderName).toList());
        }
    }

    /**
     * Velger neste tilgjengelige leverandør i en round-robin-sekvens.
     * @return En TwitterServiceProvider, eller null hvis ingen er konfigurert.
     */
    public TwitterServiceProvider getNextProvider() {
        if (providers.isEmpty()) {
            return null;
        }
        int index = currentIndex.getAndIncrement() % providers.size();
        TwitterServiceProvider provider = providers.get(index);
        log.info("Velger Twitter-leverandør: {}", provider.getProviderName());
        return provider;
    }
}