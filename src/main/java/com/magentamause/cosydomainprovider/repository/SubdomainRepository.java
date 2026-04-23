package com.magentamause.cosydomainprovider.repository;

import com.magentamause.cosydomainprovider.entity.SubdomainEntity;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubdomainRepository extends JpaRepository<SubdomainEntity, String> {
    Optional<SubdomainEntity> findByLabelIgnoreCase(String label);

    List<SubdomainEntity> findAllByOwner(UserEntity owner);

    long countByOwner(UserEntity owner);

    List<SubdomainEntity> findAllByOwner_Uuid(String ownerUuid);
}
