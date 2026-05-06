package com.magentamause.cosydomainprovider.entity;

import static org.assertj.core.api.Assertions.*;

import com.magentamause.cosydomainprovider.model.core.Plan;
import com.magentamause.cosydomainprovider.model.core.UserDto;
import org.junit.jupiter.api.Test;

class UserEntityTest {

    private UserEntity user(Plan plan) {
        return UserEntity.builder()
                .uuid("u1")
                .username("alice")
                .email("alice@example.com")
                .plan(plan)
                .isVerified(true)
                .build();
    }

    @Test
    void computeMaxSubdomainCount_freePlan_returnsFreeLimit() {
        assertThat(user(Plan.FREE).computeMaxSubdomainCount(1, 5)).isEqualTo(1);
    }

    @Test
    void computeMaxSubdomainCount_plusPlan_returnsPlusLimit() {
        assertThat(user(Plan.PLUS).computeMaxSubdomainCount(1, 5)).isEqualTo(5);
    }

    @Test
    void computeMaxSubdomainCount_override_returnsOverride() {
        UserEntity u = user(Plan.FREE);
        u.setMaxSubdomainCountOverride(10);
        assertThat(u.computeMaxSubdomainCount(1, 5)).isEqualTo(10);
    }

    @Test
    void toDto_mapsAllFields() {
        UserEntity u = user(Plan.PLUS);
        UserDto dto = u.toDto(5);
        assertThat(dto.getUuid()).isEqualTo("u1");
        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getEmail()).isEqualTo("alice@example.com");
        assertThat(dto.isVerified()).isTrue();
        assertThat(dto.getTier()).isEqualTo(Plan.PLUS);
        assertThat(dto.getMaxSubdomainCount()).isEqualTo(5);
    }
}
