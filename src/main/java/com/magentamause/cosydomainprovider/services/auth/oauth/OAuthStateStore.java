package com.magentamause.cosydomainprovider.services.auth.oauth;

import com.magentamause.cosydomainprovider.entity.OAuthStateEntity;
import com.magentamause.cosydomainprovider.repository.OAuthStateRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class OAuthStateStore {

    private static final long STATE_TTL_MS = 10 * 60 * 1000L;

    private final OAuthStateRepository oAuthStateRepository;

    String generateState() {
        String state = UUID.randomUUID().toString();
        oAuthStateRepository.save(
                OAuthStateEntity.builder().state(state).issuedAt(Instant.now()).build());
        return state;
    }

    @Transactional
    boolean consumeState(String state) {
        Optional<OAuthStateEntity> entity = oAuthStateRepository.findById(state);
        if (entity.isEmpty()) {
            return false;
        }
        oAuthStateRepository.deleteById(state);
        return entity.get().getIssuedAt().plusMillis(STATE_TTL_MS).isAfter(Instant.now());
    }

    @Scheduled(fixedRateString = "PT10M")
    void evictExpired() {
        oAuthStateRepository.deleteExpired(Instant.now().minusMillis(STATE_TTL_MS));
    }
}
