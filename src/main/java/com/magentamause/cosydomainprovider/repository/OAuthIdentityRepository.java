package com.magentamause.cosydomainprovider.repository;

import com.magentamause.cosydomainprovider.entity.OAuthIdentityEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentityEntity, String> {
    Optional<OAuthIdentityEntity> findByProviderAndProviderSubject(
            String provider, String providerSubject);

    void deleteAllByUser_Uuid(String userUuid);

    long countByProvider(String provider);
}
