package com.magentamause.cosydomainprovider.repository;

import com.magentamause.cosydomainprovider.entity.OAuthStateEntity;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface OAuthStateRepository extends JpaRepository<OAuthStateEntity, String> {

    @Modifying
    @Transactional
    @Query("DELETE FROM OAuthStateEntity s WHERE s.issuedAt < :expiredBefore")
    void deleteExpired(Instant expiredBefore);
}
