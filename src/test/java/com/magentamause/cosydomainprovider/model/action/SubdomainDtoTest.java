package com.magentamause.cosydomainprovider.model.action;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SubdomainDtoTest {

    // ---- SubdomainCreationDto ----

    @Test
    void creation_bothNull_returnsFalse() {
        SubdomainCreationDto dto = SubdomainCreationDto.builder().label("sub").build();
        assertThat(dto.isAtLeastOneIpProvided()).isFalse();
    }

    @Test
    void creation_onlyIpv4_returnsTrue() {
        SubdomainCreationDto dto =
                SubdomainCreationDto.builder().label("sub").targetIp("1.2.3.4").build();
        assertThat(dto.isAtLeastOneIpProvided()).isTrue();
    }

    @Test
    void creation_onlyIpv6_returnsTrue() {
        SubdomainCreationDto dto =
                SubdomainCreationDto.builder().label("sub").targetIpv6("::1").build();
        assertThat(dto.isAtLeastOneIpProvided()).isTrue();
    }

    @Test
    void creation_bothProvided_returnsTrue() {
        SubdomainCreationDto dto =
                SubdomainCreationDto.builder()
                        .label("sub")
                        .targetIp("1.2.3.4")
                        .targetIpv6("::1")
                        .build();
        assertThat(dto.isAtLeastOneIpProvided()).isTrue();
    }

    @Test
    void creation_blankIpv4_returnsFalse() {
        SubdomainCreationDto dto =
                SubdomainCreationDto.builder().label("sub").targetIp("   ").build();
        assertThat(dto.isAtLeastOneIpProvided()).isFalse();
    }

    // ---- SubdomainUpdateDto ----

    @Test
    void update_bothNull_returnsFalse() {
        SubdomainUpdateDto dto = new SubdomainUpdateDto();
        assertThat(dto.isAtLeastOneIpProvided()).isFalse();
    }

    @Test
    void update_onlyIpv4_returnsTrue() {
        SubdomainUpdateDto dto = SubdomainUpdateDto.builder().targetIp("1.2.3.4").build();
        assertThat(dto.isAtLeastOneIpProvided()).isTrue();
    }

    @Test
    void update_onlyIpv6_returnsTrue() {
        SubdomainUpdateDto dto = SubdomainUpdateDto.builder().targetIpv6("::1").build();
        assertThat(dto.isAtLeastOneIpProvided()).isTrue();
    }

    @Test
    void update_bothProvided_returnsTrue() {
        SubdomainUpdateDto dto =
                SubdomainUpdateDto.builder().targetIp("1.2.3.4").targetIpv6("::1").build();
        assertThat(dto.isAtLeastOneIpProvided()).isTrue();
    }

    @Test
    void update_blankIpv6_returnsFalse() {
        SubdomainUpdateDto dto = SubdomainUpdateDto.builder().targetIpv6("  ").build();
        assertThat(dto.isAtLeastOneIpProvided()).isFalse();
    }
}
