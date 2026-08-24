package com.magentamause.cosydomainprovider.services.core;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.configuration.aws.Route53Properties;
import com.magentamause.cosydomainprovider.configuration.subdomain.SubdomainProperties;
import com.magentamause.cosydomainprovider.entity.SubdomainEntity;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.action.SubdomainCreationDto;
import com.magentamause.cosydomainprovider.model.action.SubdomainUpdateDto;
import com.magentamause.cosydomainprovider.model.core.LabelAvailabilityDto;
import com.magentamause.cosydomainprovider.model.core.LabelMode;
import com.magentamause.cosydomainprovider.model.core.Plan;
import com.magentamause.cosydomainprovider.model.core.SubdomainStatus;
import com.magentamause.cosydomainprovider.model.exception.LabelConflictException;
import com.magentamause.cosydomainprovider.model.exception.SubdomainNotFoundException;
import com.magentamause.cosydomainprovider.model.exception.SubdomainQuotaExceededException;
import com.magentamause.cosydomainprovider.repository.SubdomainRepository;
import com.magentamause.cosydomainprovider.repository.WatchtowerScanRepository;
import com.magentamause.cosydomainprovider.services.aws.Route53Service;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubdomainServiceTest {

    @Mock private SubdomainRepository subdomainRepository;
    @Mock private WatchtowerScanRepository watchtowerScanRepository;
    @Mock private Route53Service route53Service;
    @Mock private SubdomainProperties subdomainProperties;
    @Mock private Route53Properties route53Properties;
    @Mock private SubdomainNameGenerator nameGenerator;
    @Mock private GlobalSettingsService globalSettingsService;

    private SubdomainService service;

    @BeforeEach
    void setUp() {
        service =
                new SubdomainService(
                        subdomainRepository,
                        watchtowerScanRepository,
                        route53Service,
                        subdomainProperties,
                        route53Properties,
                        nameGenerator,
                        globalSettingsService);
        when(route53Properties.getDomain()).thenReturn("example.com");
        when(route53Properties.getDefaultTtl()).thenReturn(300L);
        when(subdomainProperties.getReservedLabels()).thenReturn(List.of("www", "api", "admin"));
    }

    private UserEntity verifiedPlusUser() {
        return UserEntity.builder()
                .uuid("owner-1")
                .username("alice")
                .email("alice@example.com")
                .isVerified(true)
                .plan(Plan.PLUS)
                .build();
    }

    private UserEntity verifiedFreeUser() {
        return UserEntity.builder()
                .uuid("owner-1")
                .username("alice")
                .email("alice@example.com")
                .isVerified(true)
                .plan(Plan.FREE)
                .build();
    }

    private SubdomainEntity subdomain(String uuid, String label, UserEntity owner) {
        return SubdomainEntity.builder()
                .uuid(uuid)
                .label(label)
                .fqdn(label + ".example.com")
                .owner(owner)
                .targetIp("1.2.3.4")
                .status(SubdomainStatus.ACTIVE)
                .build();
    }

    // ---- getSubdomainsForOwner ----

    @Test
    void getSubdomainsForOwner_returnsList() {
        UserEntity owner = verifiedFreeUser();
        List<SubdomainEntity> list = List.of(subdomain("s1", "foo", owner));
        when(subdomainRepository.findAllByOwner(owner)).thenReturn(list);
        assertThat(service.getSubdomainsForOwner(owner)).hasSize(1);
    }

    // ---- getOwnedSubdomain ----

    @Test
    void getOwnedSubdomain_success() {
        UserEntity owner = verifiedFreeUser();
        SubdomainEntity s = subdomain("s1", "foo", owner);
        when(subdomainRepository.findById("s1")).thenReturn(Optional.of(s));
        assertThat(service.getOwnedSubdomain("s1", owner)).isSameAs(s);
    }

    @Test
    void getOwnedSubdomain_notFound_throws() {
        UserEntity owner = verifiedFreeUser();
        when(subdomainRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getOwnedSubdomain("missing", owner))
                .isInstanceOf(SubdomainNotFoundException.class);
    }

    @Test
    void getOwnedSubdomain_wrongOwner_throws() {
        UserEntity owner = verifiedFreeUser();
        UserEntity other =
                UserEntity.builder().uuid("other").username("b").email("b@b.com").build();
        SubdomainEntity s = subdomain("s1", "foo", other);
        when(subdomainRepository.findById("s1")).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> service.getOwnedSubdomain("s1", owner))
                .isInstanceOf(SubdomainNotFoundException.class);
    }

    // ---- createSubdomain ----

    @Test
    void createSubdomain_domainCreationDisabled_throws() {
        when(globalSettingsService.isDomainCreationEnabled()).thenReturn(false);
        SubdomainCreationDto dto = SubdomainCreationDto.builder().targetIp("1.2.3.4").build();
        assertThatThrownBy(() -> service.createSubdomain(dto, verifiedFreeUser()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void createSubdomain_unverifiedUser_throws() {
        when(globalSettingsService.isDomainCreationEnabled()).thenReturn(true);
        UserEntity unverified =
                UserEntity.builder()
                        .uuid("u1")
                        .username("x")
                        .email("x@x.com")
                        .isVerified(false)
                        .build();
        SubdomainCreationDto dto = SubdomainCreationDto.builder().targetIp("1.2.3.4").build();
        assertThatThrownBy(() -> service.createSubdomain(dto, unverified))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void createSubdomain_randomLabel_success() {
        when(globalSettingsService.isDomainCreationEnabled()).thenReturn(true);
        UserEntity owner = verifiedFreeUser();
        when(subdomainProperties.getMaxPerFreeUser()).thenReturn(1);
        when(subdomainProperties.getMaxPerPlusUser()).thenReturn(5);
        when(subdomainRepository.countByOwner(owner)).thenReturn(0L);
        when(nameGenerator.generate()).thenReturn("swift-hawk");
        when(subdomainRepository.findByLabelIgnoreCase("swift-hawk")).thenReturn(Optional.empty());

        SubdomainEntity saved =
                SubdomainEntity.builder()
                        .uuid("new")
                        .label("swift-hawk")
                        .fqdn("swift-hawk.example.com")
                        .owner(owner)
                        .targetIp("1.2.3.4")
                        .status(SubdomainStatus.ACTIVE)
                        .build();
        when(subdomainRepository.save(any())).thenReturn(saved);

        SubdomainCreationDto dto =
                SubdomainCreationDto.builder().targetIp("1.2.3.4").label(null).build();
        SubdomainEntity result = service.createSubdomain(dto, owner);
        assertThat(result.getLabel()).isEqualTo("swift-hawk");
    }

    @Test
    void createSubdomain_customLabel_plusUser_success() {
        when(globalSettingsService.isDomainCreationEnabled()).thenReturn(true);
        UserEntity owner = verifiedPlusUser();
        when(subdomainProperties.getMaxPerFreeUser()).thenReturn(1);
        when(subdomainProperties.getMaxPerPlusUser()).thenReturn(5);
        when(subdomainRepository.countByOwner(owner)).thenReturn(0L);
        when(subdomainRepository.findByLabelIgnoreCase("mycustom")).thenReturn(Optional.empty());

        SubdomainEntity saved =
                SubdomainEntity.builder()
                        .uuid("new")
                        .label("mycustom")
                        .fqdn("mycustom.example.com")
                        .owner(owner)
                        .targetIp("1.2.3.4")
                        .status(SubdomainStatus.ACTIVE)
                        .labelMode(LabelMode.CUSTOM)
                        .build();
        when(subdomainRepository.save(any())).thenReturn(saved);

        SubdomainCreationDto dto =
                SubdomainCreationDto.builder().targetIp("1.2.3.4").label("mycustom").build();
        SubdomainEntity result = service.createSubdomain(dto, owner);
        assertThat(result.getLabelMode()).isEqualTo(LabelMode.CUSTOM);
    }

    @Test
    void createSubdomain_quotaExceeded_throws() {
        when(globalSettingsService.isDomainCreationEnabled()).thenReturn(true);
        UserEntity owner = verifiedFreeUser();
        when(subdomainProperties.getMaxPerFreeUser()).thenReturn(1);
        when(subdomainProperties.getMaxPerPlusUser()).thenReturn(5);
        when(subdomainRepository.countByOwner(owner)).thenReturn(1L);
        when(nameGenerator.generate()).thenReturn("swift-hawk");
        when(subdomainRepository.findByLabelIgnoreCase("swift-hawk")).thenReturn(Optional.empty());

        SubdomainCreationDto dto = SubdomainCreationDto.builder().targetIp("1.2.3.4").build();
        assertThatThrownBy(() -> service.createSubdomain(dto, owner))
                .isInstanceOf(SubdomainQuotaExceededException.class);
    }

    @Test
    void createSubdomain_reservedLabel_throws() {
        when(globalSettingsService.isDomainCreationEnabled()).thenReturn(true);
        UserEntity owner = verifiedPlusUser();
        when(subdomainProperties.getMaxPerFreeUser()).thenReturn(1);
        when(subdomainProperties.getMaxPerPlusUser()).thenReturn(5);
        when(subdomainRepository.countByOwner(owner)).thenReturn(0L);

        SubdomainCreationDto dto =
                SubdomainCreationDto.builder().targetIp("1.2.3.4").label("www").build();
        assertThatThrownBy(() -> service.createSubdomain(dto, owner))
                .isInstanceOf(LabelConflictException.class);
    }

    @Test
    void createSubdomain_takenLabel_throws() {
        when(globalSettingsService.isDomainCreationEnabled()).thenReturn(true);
        UserEntity owner = verifiedPlusUser();
        when(subdomainProperties.getMaxPerFreeUser()).thenReturn(1);
        when(subdomainProperties.getMaxPerPlusUser()).thenReturn(5);
        when(subdomainRepository.countByOwner(owner)).thenReturn(0L);
        UserEntity other =
                UserEntity.builder().uuid("other").username("b").email("b@b.com").build();
        when(subdomainRepository.findByLabelIgnoreCase("taken"))
                .thenReturn(Optional.of(subdomain("s99", "taken", other)));

        SubdomainCreationDto dto =
                SubdomainCreationDto.builder().targetIp("1.2.3.4").label("taken").build();
        assertThatThrownBy(() -> service.createSubdomain(dto, owner))
                .isInstanceOf(LabelConflictException.class);
    }

    // ---- updateTargetIp ----

    @Test
    void updateTargetIp_success() {
        UserEntity owner = verifiedFreeUser();
        SubdomainEntity s = subdomain("s1", "foo", owner);
        when(subdomainRepository.findById("s1")).thenReturn(Optional.of(s));
        when(subdomainRepository.save(any())).thenReturn(s);

        SubdomainUpdateDto dto =
                SubdomainUpdateDto.builder().targetIp("5.6.7.8").targetIpv6(null).build();
        service.updateTargetIp("s1", dto, owner);
        assertThat(s.getTargetIp()).isEqualTo("5.6.7.8");
    }

    @Test
    void updateTargetIp_clearedIpv6_deletesStaleAaaaRecord() {
        UserEntity owner = verifiedFreeUser();
        SubdomainEntity s = subdomain("s1", "foo", owner);
        s.setTargetIpv6("2001:db8::1");
        when(subdomainRepository.findById("s1")).thenReturn(Optional.of(s));
        when(subdomainRepository.save(any())).thenReturn(s);

        SubdomainUpdateDto dto =
                SubdomainUpdateDto.builder().targetIp("5.6.7.8").targetIpv6(null).build();
        service.updateTargetIp("s1", dto, owner);

        verify(route53Service).deleteAAAARecord("foo.example.com", "2001:db8::1");
        verify(route53Service).upsertARecord("foo.example.com", "5.6.7.8");
        verify(route53Service, never()).upsertAAAARecord(any(), any());
    }

    @Test
    void updateTargetIp_keptIpv6_doesNotDeleteRecord() {
        UserEntity owner = verifiedFreeUser();
        SubdomainEntity s = subdomain("s1", "foo", owner);
        s.setTargetIpv6("2001:db8::1");
        when(subdomainRepository.findById("s1")).thenReturn(Optional.of(s));
        when(subdomainRepository.save(any())).thenReturn(s);

        SubdomainUpdateDto dto =
                SubdomainUpdateDto.builder().targetIp("5.6.7.8").targetIpv6("2001:db8::2").build();
        service.updateTargetIp("s1", dto, owner);

        verify(route53Service, never()).deleteAAAARecord(any(), any());
        verify(route53Service).upsertAAAARecord("foo.example.com", "2001:db8::2");
    }

    // ---- deleteSubdomain ----

    @Test
    void deleteSubdomain_byUuid_success() {
        UserEntity owner = verifiedFreeUser();
        SubdomainEntity s = subdomain("s1", "foo", owner);
        when(subdomainRepository.findById("s1")).thenReturn(Optional.of(s));

        service.adminDeleteSubdomain("s1");
        verify(route53Service).deleteARecord("foo.example.com", "1.2.3.4");
        verify(subdomainRepository).delete(s);
    }

    @Test
    void deleteSubdomain_notFound_throws() {
        when(subdomainRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.adminDeleteSubdomain("missing"))
                .isInstanceOf(SubdomainNotFoundException.class);
    }

    @Test
    void deleteSubdomain_withOwner_success() {
        UserEntity owner = verifiedFreeUser();
        SubdomainEntity s = subdomain("s1", "foo", owner);
        s.setTargetIpv6(null);
        when(subdomainRepository.findById("s1")).thenReturn(Optional.of(s));

        service.deleteSubdomain("s1", owner);
        verify(subdomainRepository).delete(s);
    }

    @Test
    void deleteSubdomain_route53Failure_marksFailedAndThrows() {
        UserEntity owner = verifiedFreeUser();
        SubdomainEntity s = subdomain("s1", "foo", owner);
        when(subdomainRepository.findById("s1")).thenReturn(Optional.of(s));
        doThrow(new RuntimeException("AWS error")).when(route53Service).deleteARecord(any(), any());
        when(subdomainRepository.save(any())).thenReturn(s);

        assertThatThrownBy(() -> service.adminDeleteSubdomain("s1"))
                .isInstanceOf(ResponseStatusException.class);
        verify(subdomainRepository).save(s);
    }

    @Test
    void deleteSubdomain_removesWatchtowerScansBeforeSubdomain() {
        UserEntity owner = verifiedFreeUser();
        SubdomainEntity s = subdomain("s1", "foo", owner);
        when(subdomainRepository.findById("s1")).thenReturn(Optional.of(s));

        service.adminDeleteSubdomain("s1");

        InOrder inOrder = inOrder(watchtowerScanRepository, subdomainRepository);
        inOrder.verify(watchtowerScanRepository).deleteAllBySubdomain_Uuid("s1");
        inOrder.verify(subdomainRepository).delete(s);
    }

    @Test
    void deleteSubdomain_databaseFailure_isNotReportedAsDnsFailure() {
        UserEntity owner = verifiedFreeUser();
        SubdomainEntity s = subdomain("s1", "foo", owner);
        when(subdomainRepository.findById("s1")).thenReturn(Optional.of(s));
        doThrow(new DataIntegrityViolationException("FK violation"))
                .when(subdomainRepository)
                .delete(s);

        assertThatThrownBy(() -> service.adminDeleteSubdomain("s1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(s.getStatus()).isNotEqualTo(SubdomainStatus.FAILED);
        verify(subdomainRepository, never()).save(any());
    }

    // ---- checkLabelAvailability ----

    @Test
    void checkLabelAvailability_tooShort() {
        LabelAvailabilityDto result = service.checkLabelAvailability("ab");
        assertThat(result.isAvailable()).isFalse();
    }

    @Test
    void checkLabelAvailability_reserved() {
        LabelAvailabilityDto result = service.checkLabelAvailability("www");
        assertThat(result.isAvailable()).isFalse();
    }

    @Test
    void checkLabelAvailability_taken() {
        UserEntity other = UserEntity.builder().uuid("o").username("o").email("o@o.com").build();
        when(subdomainRepository.findByLabelIgnoreCase("taken"))
                .thenReturn(Optional.of(subdomain("s1", "taken", other)));
        LabelAvailabilityDto result = service.checkLabelAvailability("taken");
        assertThat(result.isAvailable()).isFalse();
    }

    @Test
    void checkLabelAvailability_available() {
        when(subdomainRepository.findByLabelIgnoreCase("free")).thenReturn(Optional.empty());
        LabelAvailabilityDto result = service.checkLabelAvailability("free");
        assertThat(result.isAvailable()).isTrue();
    }

    // ---- admin methods ----

    @Test
    void adminGetAllSubdomains_returnsList() {
        UserEntity owner = verifiedFreeUser();
        when(subdomainRepository.findAll()).thenReturn(List.of(subdomain("s1", "foo", owner)));
        assertThat(service.adminGetAllSubdomains()).hasSize(1);
    }

    @Test
    void adminGetSubdomain_found() {
        UserEntity owner = verifiedFreeUser();
        SubdomainEntity s = subdomain("s1", "foo", owner);
        when(subdomainRepository.findById("s1")).thenReturn(Optional.of(s));
        assertThat(service.adminGetSubdomain("s1")).isSameAs(s);
    }

    @Test
    void adminGetSubdomain_notFound_throws() {
        when(subdomainRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.adminGetSubdomain("x"))
                .isInstanceOf(SubdomainNotFoundException.class);
    }

    @Test
    void getCountByOwner_delegates() {
        UserEntity owner = verifiedFreeUser();
        when(subdomainRepository.countByOwner(owner)).thenReturn(3L);
        assertThat(service.getCountByOwner(owner)).isEqualTo(3L);
    }

    @Test
    void getParentDomain_returnsDomain() {
        assertThat(service.getParentDomain()).isEqualTo("example.com");
    }

    @Test
    void getDefaultTtl_returnsTtl() {
        assertThat(service.getDefaultTtl()).isEqualTo(300L);
    }

    @Test
    void adminUpdateTargetIp_success() {
        UserEntity owner = verifiedFreeUser();
        SubdomainEntity s = subdomain("s1", "foo", owner);
        when(subdomainRepository.findById("s1")).thenReturn(Optional.of(s));
        when(subdomainRepository.save(any())).thenReturn(s);

        SubdomainUpdateDto dto =
                SubdomainUpdateDto.builder().targetIp("9.9.9.9").targetIpv6(null).build();
        service.adminUpdateTargetIp("s1", dto);
        assertThat(s.getTargetIp()).isEqualTo("9.9.9.9");
    }

    @Test
    void adminRelabelSubdomain_sameLabel_returnsUnchanged() {
        UserEntity owner = verifiedFreeUser();
        SubdomainEntity s = subdomain("s1", "foo", owner);
        when(subdomainRepository.findById("s1")).thenReturn(Optional.of(s));

        SubdomainEntity result = service.adminRelabelSubdomain("s1", "FOO");
        assertThat(result).isSameAs(s);
    }

    @Test
    void adminRelabelSubdomain_reserved_throws() {
        UserEntity owner = verifiedFreeUser();
        SubdomainEntity s = subdomain("s1", "foo", owner);
        when(subdomainRepository.findById("s1")).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.adminRelabelSubdomain("s1", "www"))
                .isInstanceOf(LabelConflictException.class);
    }

    @Test
    void adminRelabelSubdomain_taken_throws() {
        UserEntity owner = verifiedFreeUser();
        SubdomainEntity s = subdomain("s1", "foo", owner);
        when(subdomainRepository.findById("s1")).thenReturn(Optional.of(s));
        UserEntity other = UserEntity.builder().uuid("o").username("o").email("o@o.com").build();
        when(subdomainRepository.findByLabelIgnoreCase("bar"))
                .thenReturn(Optional.of(subdomain("s2", "bar", other)));

        assertThatThrownBy(() -> service.adminRelabelSubdomain("s1", "bar"))
                .isInstanceOf(LabelConflictException.class);
    }

    @Test
    void adminRelabelSubdomain_success() {
        UserEntity owner = verifiedFreeUser();
        SubdomainEntity s = subdomain("s1", "foo", owner);
        when(subdomainRepository.findById("s1")).thenReturn(Optional.of(s));
        when(subdomainRepository.findByLabelIgnoreCase("newlabel")).thenReturn(Optional.empty());
        when(subdomainRepository.save(any())).thenReturn(s);

        service.adminRelabelSubdomain("s1", "newlabel");
        assertThat(s.getLabel()).isEqualTo("newlabel");
        assertThat(s.getFqdn()).isEqualTo("newlabel.example.com");
    }

    @Test
    void adminRelabelSubdomain_route53DeleteFails_throws() {
        UserEntity owner = verifiedFreeUser();
        SubdomainEntity s = subdomain("s1", "foo", owner);
        when(subdomainRepository.findById("s1")).thenReturn(Optional.of(s));
        when(subdomainRepository.findByLabelIgnoreCase("newlabel")).thenReturn(Optional.empty());
        doThrow(new RuntimeException("AWS")).when(route53Service).deleteARecord(any(), any());

        assertThatThrownBy(() -> service.adminRelabelSubdomain("s1", "newlabel"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void deleteSubdomainsByOwner_deletesAll() {
        UserEntity owner = verifiedFreeUser();
        SubdomainEntity s1 = subdomain("s1", "foo", owner);
        SubdomainEntity s2 = subdomain("s2", "bar", owner);
        s1.setTargetIpv6(null);
        s2.setTargetIpv6(null);
        when(subdomainRepository.findAllByOwner_Uuid("owner-1")).thenReturn(List.of(s1, s2));
        when(subdomainRepository.findById("s1")).thenReturn(Optional.of(s1));
        when(subdomainRepository.findById("s2")).thenReturn(Optional.of(s2));

        service.deleteSubdomainsByOwner("owner-1");
        verify(subdomainRepository).delete(s1);
        verify(subdomainRepository).delete(s2);
    }
}
