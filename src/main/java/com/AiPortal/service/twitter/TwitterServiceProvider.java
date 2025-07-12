package com.AiPortal.service.twitter;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;
import java.util.List;

// Et felles grensesnitt som alle våre Twitter-tjenester skal implementere.
public interface TwitterServiceProvider {

    // Returnerer et unikt navn for denne leverandøren, f.eks. "OfficialTwitterApi" eller "TwttrApi"
    String getProviderName();

    // Metoden for å søke etter tweets. Den er lik for alle.
    Mono<String> searchRecentTweets(String query, String sinceId);

    // Hjelpemetoder for parsing kan også være en del av kontrakten
    List<JsonNode> parseTweetsFromResponse(String responseBody);
    String parseNewestTweetId(String responseBody);
    String findUsernameFromIncludes(String responseBody, String authorId);
}