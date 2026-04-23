package com.magentamause.cosydomainprovider.repository;

import com.magentamause.cosydomainprovider.entity.SubdomainEntity;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubdomainRepository extends JpaRepository<SubdomainEntity, String> {
    Optional<SubdomainEntity> findByLabelIgnoreCase(String label);

    List<SubdomainEntity> findAllByOwner(UserEntity owner);

    long countByOwner(UserEntity owner);

    List<SubdomainEntity> findAllByOwner_Uuid(String ownerUuid);
}
