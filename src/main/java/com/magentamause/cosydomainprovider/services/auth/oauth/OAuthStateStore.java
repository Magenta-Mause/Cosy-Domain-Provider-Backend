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
        return saveState(null);
    }

    String generateLinkState(String userId) {
        return saveState(userId);
    }

    private String saveState(String linkedUserId) {
        String state = UUID.randomUUID().toString();
        oAuthStateRepository.save(
                OAuthStateEntity.builder()
                        .state(state)
                        .issuedAt(Instant.now())
                        .linkedUserId(linkedUserId)
                        .build());
        return state;
    }

    @Transactional
    Optional<OAuthStateEntity> consumeState(String state) {
        Optional<OAuthStateEntity> entity = oAuthStateRepository.findById(state);
        if (entity.isEmpty()) {
            return Optional.empty();
        }
        oAuthStateRepository.deleteById(state);
        boolean valid = entity.get().getIssuedAt().plusMillis(STATE_TTL_MS).isAfter(Instant.now());
        return valid ? entity : Optional.empty();
    }

    String peekLinkedUserId(String state) {
        return oAuthStateRepository
                .findById(state)
                .map(OAuthStateEntity::getLinkedUserId)
                .orElse(null);
    }

    @Scheduled(fixedRateString = "PT10M")
    void evictExpired() {
        oAuthStateRepository.deleteExpired(Instant.now().minusMillis(STATE_TTL_MS));
    }
}
