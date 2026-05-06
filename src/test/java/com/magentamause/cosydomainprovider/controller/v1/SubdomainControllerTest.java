package com.magentamause.cosydomainprovider.controller.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.controller.v1.implementation.SubdomainController;
import com.magentamause.cosydomainprovider.entity.SubdomainEntity;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.action.SubdomainCreationDto;
import com.magentamause.cosydomainprovider.model.action.SubdomainUpdateDto;
import com.magentamause.cosydomainprovider.model.core.*;
import com.magentamause.cosydomainprovider.services.auth.SecurityContextService;
import com.magentamause.cosydomainprovider.services.core.SubdomainService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SubdomainControllerTest {

    @Mock private SubdomainService subdomainService;
    @Mock private SecurityContextService securityContextService;

    private SubdomainController controller;

    @BeforeEach
    void setUp() {
        controller = new SubdomainController(subdomainService, securityContextService);
    }

    private UserEntity owner() {
        return UserEntity.builder()
                .uuid("u1")
                .username("alice")
                .email("a@a.com")
                .isVerified(true)
                .build();
    }

    private SubdomainEntity subdomain(String label) {
        return SubdomainEntity.builder()
                .uuid("s1")
                .label(label)
                .fqdn(label + ".example.com")
                .owner(owner())
                .targetIp("1.2.3.4")
                .status(SubdomainStatus.ACTIVE)
                .labelMode(LabelMode.RANDOM)
                .build();
    }

    @Test
    void checkLabelAvailability_returnsResult() {
        LabelAvailabilityDto dto = LabelAvailabilityDto.available();
        when(subdomainService.checkLabelAvailability("foo")).thenReturn(dto);
        ResponseEntity<LabelAvailabilityDto> resp = controller.checkLabelAvailability("foo");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(dto);
    }

    @Test
    void listMySubdomains_returnsMappedList() {
        UserEntity user = owner();
        when(securityContextService.getUser()).thenReturn(user);
        when(subdomainService.getParentDomain()).thenReturn("example.com");
        when(subdomainService.getDefaultTtl()).thenReturn(300L);
        when(subdomainService.getSubdomainsForOwner(user)).thenReturn(List.of(subdomain("foo")));

        ResponseEntity<List<SubdomainDto>> resp = controller.listMySubdomains();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
    }

    @Test
    void getSubdomain_returnsMapped() {
        UserEntity user = owner();
        when(securityContextService.getUser()).thenReturn(user);
        when(subdomainService.getParentDomain()).thenReturn("example.com");
        when(subdomainService.getDefaultTtl()).thenReturn(300L);
        when(subdomainService.getOwnedSubdomain("s1", user)).thenReturn(subdomain("foo"));

        ResponseEntity<SubdomainDto> resp = controller.getSubdomain("s1");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getLabel()).isEqualTo("foo");
    }

    @Test
    void createSubdomain_success() {
        UserEntity user = owner();
        when(securityContextService.getUser()).thenReturn(user);
        when(subdomainService.getParentDomain()).thenReturn("example.com");
        when(subdomainService.getDefaultTtl()).thenReturn(300L);
        SubdomainCreationDto dto = SubdomainCreationDto.builder().targetIp("1.2.3.4").build();
        when(subdomainService.createSubdomain(dto, user)).thenReturn(subdomain("swift-hawk"));

        ResponseEntity<SubdomainDto> resp = controller.createSubdomain(dto);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void createSubdomain_labelTooShort_throws() {
        SubdomainCreationDto dto =
                SubdomainCreationDto.builder().label("ab").targetIp("1.2.3.4").build();
        assertThatThrownBy(() -> controller.createSubdomain(dto))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e ->
                                assertThat(((ResponseStatusException) e).getStatusCode())
                                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createSubdomain_labelTooLong_throws() {
        SubdomainCreationDto dto =
                SubdomainCreationDto.builder().label("a".repeat(46)).targetIp("1.2.3.4").build();
        assertThatThrownBy(() -> controller.createSubdomain(dto))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e ->
                                assertThat(((ResponseStatusException) e).getStatusCode())
                                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void updateSubdomain_returnsUpdated() {
        UserEntity user = owner();
        when(securityContextService.getUser()).thenReturn(user);
        when(subdomainService.getParentDomain()).thenReturn("example.com");
        when(subdomainService.getDefaultTtl()).thenReturn(300L);
        SubdomainUpdateDto dto = SubdomainUpdateDto.builder().targetIp("5.6.7.8").build();
        when(subdomainService.updateTargetIp("s1", dto, user)).thenReturn(subdomain("foo"));

        ResponseEntity<SubdomainDto> resp = controller.updateSubdomain("s1", dto);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteSubdomain_returnsNoContent() {
        UserEntity user = owner();
        when(securityContextService.getUser()).thenReturn(user);
        ResponseEntity<Void> resp = controller.deleteSubdomain("s1");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(subdomainService).deleteSubdomain("s1", user);
    }
}
