package com.magentamause.cosydomainprovider.repository;

import com.magentamause.cosydomainprovider.entity.OAuthIdentityEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentityEntity, String> {
    Optional<OAuthIdentityEntity> findByProviderAndProviderSubject(
            String provider, String providerSubject);

    Optional<OAuthIdentityEntity> findByProviderAndUser_Uuid(String provider, String userUuid);

    java.util.List<OAuthIdentityEntity> findAllByUser_Uuid(String userUuid);

    void deleteAllByUser_Uuid(String userUuid);

    long countByProvider(String provider);

    long countByUser_Uuid(String userUuid);
}
