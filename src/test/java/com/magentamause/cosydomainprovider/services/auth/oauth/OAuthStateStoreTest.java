package com.magentamause.cosydomainprovider.services.auth.oauth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.entity.OAuthStateEntity;
import com.magentamause.cosydomainprovider.repository.OAuthStateRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuthStateStoreTest {

    @Mock private OAuthStateRepository repository;

    private OAuthStateStore store;

    @BeforeEach
    void setUp() {
        store = new OAuthStateStore(repository);
    }

    @Test
    void generateState_savesAndReturnsUuid() {
        when(repository.save(any(OAuthStateEntity.class))).thenAnswer(i -> i.getArgument(0));
        String state = store.generateState();
        assertThat(state).isNotBlank().matches("[0-9a-f-]{36}");
        verify(repository).save(any(OAuthStateEntity.class));
    }

    @Test
    void consumeState_unknownState_returnsFalse() {
        when(repository.findById("unknown")).thenReturn(Optional.empty());
        assertThat(store.consumeState("unknown")).isEmpty();
        verify(repository, never()).deleteById(any());
    }

    @Test
    void consumeState_validRecentState_returnsTrue() {
        OAuthStateEntity entity =
                OAuthStateEntity.builder().state("abc").issuedAt(Instant.now()).build();
        when(repository.findById("abc")).thenReturn(Optional.of(entity));

        assertThat(store.consumeState("abc")).isPresent();
        verify(repository).deleteById("abc");
    }

    @Test
    void consumeState_expiredState_returnsFalse() {
        OAuthStateEntity entity =
                OAuthStateEntity.builder()
                        .state("old")
                        .issuedAt(Instant.now().minusSeconds(3600))
                        .build();
        when(repository.findById("old")).thenReturn(Optional.of(entity));

        assertThat(store.consumeState("old")).isEmpty();
        verify(repository).deleteById("old");
    }

    @Test
    void generateLinkState_savesStateWithLinkedUserId() {
        when(repository.save(any(OAuthStateEntity.class))).thenAnswer(i -> i.getArgument(0));
        String state = store.generateLinkState("user-123");
        assertThat(state).isNotBlank().matches("[0-9a-f-]{36}");
        verify(repository)
                .save(
                        argThat(
                                e ->
                                        e instanceof OAuthStateEntity
                                                && "user-123"
                                                        .equals(
                                                                ((OAuthStateEntity) e)
                                                                        .getLinkedUserId())));
    }

    @Test
    void peekLinkedUserId_existingState_returnsLinkedUserId() {
        OAuthStateEntity entity =
                OAuthStateEntity.builder()
                        .state("abc")
                        .issuedAt(Instant.now())
                        .linkedUserId("user-42")
                        .build();
        when(repository.findById("abc")).thenReturn(Optional.of(entity));
        assertThat(store.peekLinkedUserId("abc")).isEqualTo("user-42");
    }

    @Test
    void peekLinkedUserId_missingState_returnsNull() {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThat(store.peekLinkedUserId("missing")).isNull();
    }

    @Test
    void evictExpired_deletesOldEntries() {
        store.evictExpired();
        verify(repository).deleteExpired(any(Instant.class));
    }
}
