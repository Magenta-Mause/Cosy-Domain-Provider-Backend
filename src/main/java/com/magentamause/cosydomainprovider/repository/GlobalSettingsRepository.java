package com.magentamause.cosydomainprovider.repository;

import com.magentamause.cosydomainprovider.entity.GlobalSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalSettingsRepository extends JpaRepository<GlobalSettingsEntity, String> {}
