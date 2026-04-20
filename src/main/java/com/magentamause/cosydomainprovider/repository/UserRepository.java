package com.magentamause.cosydomainprovider.repository;

import com.magentamause.cosydomainprovider.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByUsernameIgnoreCase(String username);
}
