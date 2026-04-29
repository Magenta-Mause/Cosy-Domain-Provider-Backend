package com.magentamause.cosydomainprovider.services.core;

import com.magentamause.cosydomainprovider.entity.GlobalSettingsEntity;
import com.magentamause.cosydomainprovider.repository.GlobalSettingsRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GlobalSettingsService {

    private final GlobalSettingsRepository globalSettingsRepository;

    @PostConstruct
    public void init() {
        if (!globalSettingsRepository.existsById("global")) {
            globalSettingsRepository.save(new GlobalSettingsEntity());
        }
    }

    public boolean isDomainCreationEnabled() {
        return globalSettingsRepository
                .findById("global")
                .map(GlobalSettingsEntity::isDomainCreationEnabled)
                .orElse(true);
    }

    public GlobalSettingsEntity setDomainCreationEnabled(boolean enabled) {
        GlobalSettingsEntity settings =
                globalSettingsRepository
                        .findById("global")
                        .orElse(new GlobalSettingsEntity());
        settings.setDomainCreationEnabled(enabled);
        return globalSettingsRepository.save(settings);
    }
}
