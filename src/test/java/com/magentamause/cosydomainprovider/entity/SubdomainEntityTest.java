package com.magentamause.cosydomainprovider.entity;

import static org.assertj.core.api.Assertions.*;

import com.magentamause.cosydomainprovider.model.core.LabelMode;
import com.magentamause.cosydomainprovider.model.core.SubdomainDto;
import com.magentamause.cosydomainprovider.model.core.SubdomainStatus;
import org.junit.jupiter.api.Test;

class SubdomainEntityTest {

    private UserEntity owner() {
        return UserEntity.builder().uuid("u1").username("alice").email("a@a.com").build();
    }

    @Test
    void toDto_withStoredFqdn_usesFqdn() {
        SubdomainEntity s =
                SubdomainEntity.builder()
                        .uuid("s1")
                        .label("swift-hawk")
                        .fqdn("swift-hawk.example.com")
                        .owner(owner())
                        .targetIp("1.2.3.4")
                        .targetIpv6("::1")
                        .status(SubdomainStatus.ACTIVE)
                        .labelMode(LabelMode.RANDOM)
                        .build();
        SubdomainDto dto = s.toDto("example.com", 300L);
        assertThat(dto.getFqdn()).isEqualTo("swift-hawk.example.com");
        assertThat(dto.getDnsEntries()).hasSize(2);
    }

    @Test
    void toDto_noFqdn_fallsBackToLabelPlusDomain() {
        SubdomainEntity s =
                SubdomainEntity.builder()
                        .uuid("s1")
                        .label("my-sub")
                        .fqdn(null)
                        .owner(owner())
                        .targetIp("1.2.3.4")
                        .status(SubdomainStatus.ACTIVE)
                        .labelMode(LabelMode.CUSTOM)
                        .build();
        SubdomainDto dto = s.toDto("example.com", 300L);
        assertThat(dto.getFqdn()).isEqualTo("my-sub.example.com");
    }

    @Test
    void toDto_noIps_emptyDnsEntries() {
        SubdomainEntity s =
                SubdomainEntity.builder()
                        .uuid("s1")
                        .label("foo")
                        .fqdn("foo.example.com")
                        .owner(owner())
                        .status(SubdomainStatus.PENDING)
                        .labelMode(LabelMode.RANDOM)
                        .build();
        SubdomainDto dto = s.toDto("example.com", 300L);
        assertThat(dto.getDnsEntries()).isEmpty();
    }

    @Test
    void toDto_onlyIpv4_oneEntry() {
        SubdomainEntity s =
                SubdomainEntity.builder()
                        .uuid("s1")
                        .label("foo")
                        .fqdn("foo.example.com")
                        .owner(owner())
                        .targetIp("1.2.3.4")
                        .status(SubdomainStatus.ACTIVE)
                        .labelMode(LabelMode.RANDOM)
                        .build();
        SubdomainDto dto = s.toDto("example.com", 300L);
        assertThat(dto.getDnsEntries()).hasSize(1);
        assertThat(dto.getDnsEntries().get(0).type()).isEqualTo("A");
    }
}
