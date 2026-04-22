package com.magentamause.cosydomainprovider.services.auth.oauth;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
class OAuthStateStore {

    private static final long STATE_TTL_MS = 10 * 60 * 1000L;
    private final ConcurrentHashMap<String, Long> pending = new ConcurrentHashMap<>();

    String generateState() {
        evictExpired();
        String state = UUID.randomUUID().toString();
        pending.put(state, System.currentTimeMillis());
        return state;
    }

    boolean consumeState(String state) {
        Long issuedAt = pending.remove(state);
        return issuedAt != null && System.currentTimeMillis() - issuedAt <= STATE_TTL_MS;
    }

    private void evictExpired() {
        long now = System.currentTimeMillis();
        pending.entrySet().removeIf(e -> now - e.getValue() > STATE_TTL_MS);
    }
}
