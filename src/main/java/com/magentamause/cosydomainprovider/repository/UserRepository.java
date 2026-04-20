package com.magentamause.cosydomainprovider.repository;

import com.magentamause.cosydomainprovider.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {
}
