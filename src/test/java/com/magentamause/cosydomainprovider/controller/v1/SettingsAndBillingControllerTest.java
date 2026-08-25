package com.magentamause.cosydomainprovider.controller.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.controller.v1.implementation.BillingController;
import com.magentamause.cosydomainprovider.controller.v1.implementation.SettingsController;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.admin.AdminSettingsDto;
import com.magentamause.cosydomainprovider.model.core.BillingPortalResponseDto;
import com.magentamause.cosydomainprovider.services.auth.SecurityContextService;
import com.magentamause.cosydomainprovider.services.billing.StripeService;
import com.magentamause.cosydomainprovider.services.core.GlobalSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SettingsAndBillingControllerTest {

    // ---- SettingsController ----

    @Mock private GlobalSettingsService globalSettingsService;
    @InjectMocks private SettingsController settingsController;

    @Test
    void getPublicSettings_returnsDomainCreationStatus() {
        when(globalSettingsService.isDomainCreationEnabled()).thenReturn(true);
        ResponseEntity<AdminSettingsDto> resp = settingsController.getPublicSettings();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().isDomainCreationEnabled()).isTrue();
    }

    // ---- BillingController ----

    @Mock private StripeService stripeService;
    @Mock private SecurityContextService securityContextService;

    @Test
    void getBillingPortalUrl_returnsUrl() {
        UserEntity user = UserEntity.builder().uuid("u1").username("a").email("a@a.com").build();
        when(securityContextService.requireUser()).thenReturn(user);
        when(stripeService.createBillingPortalSession(user)).thenReturn("https://billing.portal");

        BillingController billingController =
                new BillingController(stripeService, securityContextService);
        ResponseEntity<BillingPortalResponseDto> resp = billingController.getBillingPortalUrl();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getUrl()).isEqualTo("https://billing.portal");
    }

    @Test
    void getCheckoutUrl_unverifiedUser_throwsForbidden() {
        UserEntity user =
                UserEntity.builder()
                        .uuid("u1")
                        .username("a")
                        .email("a@a.com")
                        .isVerified(false)
                        .build();
        when(securityContextService.requireUser()).thenReturn(user);

        BillingController billingController =
                new BillingController(stripeService, securityContextService);
        assertThatThrownBy(() -> billingController.getCheckoutUrl())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e ->
                                assertThat(((ResponseStatusException) e).getStatusCode())
                                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void getCheckoutUrl_verifiedUser_returnsUrl() {
        UserEntity user =
                UserEntity.builder()
                        .uuid("u1")
                        .username("a")
                        .email("a@a.com")
                        .isVerified(true)
                        .build();
        when(securityContextService.requireUser()).thenReturn(user);
        when(stripeService.createCheckoutSession(user)).thenReturn("https://checkout");

        BillingController billingController =
                new BillingController(stripeService, securityContextService);
        ResponseEntity<BillingPortalResponseDto> resp = billingController.getCheckoutUrl();
        assertThat(resp.getBody().getUrl()).isEqualTo("https://checkout");
    }
}
