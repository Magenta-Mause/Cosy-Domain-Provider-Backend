package com.magentamause.cosydomainprovider.services.core;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.entity.GlobalSettingsEntity;
import com.magentamause.cosydomainprovider.repository.GlobalSettingsRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GlobalSettingsServiceTest {

    @Mock private GlobalSettingsRepository globalSettingsRepository;

    private GlobalSettingsService service;

    @BeforeEach
    void setUp() {
        service = new GlobalSettingsService(globalSettingsRepository);
    }

    @Test
    void init_settingsNotExist_savesDefault() {
        when(globalSettingsRepository.existsById("global")).thenReturn(false);
        service.init();
        verify(globalSettingsRepository).save(any(GlobalSettingsEntity.class));
    }

    @Test
    void init_settingsExist_doesNotSave() {
        when(globalSettingsRepository.existsById("global")).thenReturn(true);
        service.init();
        verify(globalSettingsRepository, never()).save(any());
    }

    @Test
    void isDomainCreationEnabled_returnsTrue() {
        GlobalSettingsEntity settings = new GlobalSettingsEntity();
        settings.setDomainCreationEnabled(true);
        when(globalSettingsRepository.findById("global")).thenReturn(Optional.of(settings));
        assertThat(service.isDomainCreationEnabled()).isTrue();
    }

    @Test
    void isDomainCreationEnabled_returnsFalse() {
        GlobalSettingsEntity settings = new GlobalSettingsEntity();
        settings.setDomainCreationEnabled(false);
        when(globalSettingsRepository.findById("global")).thenReturn(Optional.of(settings));
        assertThat(service.isDomainCreationEnabled()).isFalse();
    }

    @Test
    void isDomainCreationEnabled_notFound_defaultsTrue() {
        when(globalSettingsRepository.findById("global")).thenReturn(Optional.empty());
        assertThat(service.isDomainCreationEnabled()).isTrue();
    }

    @Test
    void setDomainCreationEnabled_existingSettings_updatesAndSaves() {
        GlobalSettingsEntity existing = new GlobalSettingsEntity();
        existing.setDomainCreationEnabled(true);
        when(globalSettingsRepository.findById("global")).thenReturn(Optional.of(existing));
        when(globalSettingsRepository.save(any())).thenReturn(existing);

        service.setDomainCreationEnabled(false);
        assertThat(existing.isDomainCreationEnabled()).isFalse();
        verify(globalSettingsRepository).save(existing);
    }

    @Test
    void setDomainCreationEnabled_noExistingSettings_createsNew() {
        when(globalSettingsRepository.findById("global")).thenReturn(Optional.empty());
        when(globalSettingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GlobalSettingsEntity result = service.setDomainCreationEnabled(false);
        assertThat(result.isDomainCreationEnabled()).isFalse();
    }
}
