package com.magentamause.cosydomainprovider.metrics;

import com.magentamause.cosydomainprovider.configuration.oauth.OAuthProperties;
import com.magentamause.cosydomainprovider.model.core.Plan;
import com.magentamause.cosydomainprovider.repository.OAuthIdentityRepository;
import com.magentamause.cosydomainprovider.repository.SubdomainRepository;
import com.magentamause.cosydomainprovider.repository.UserRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class ApplicationMetricsService {

    private final MeterRegistry registry;
    private final UserRepository userRepository;
    private final SubdomainRepository subdomainRepository;
    private final OAuthIdentityRepository oAuthIdentityRepository;
    private final OAuthProperties oAuthProperties;
    private final WebClient.Builder webClientBuilder;

    @Value("${frontend.url}")
    private String frontendUrl;

    private final AtomicInteger frontendUp = new AtomicInteger(0);

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("cosy.users.total", userRepository, UserRepository::count)
                .description("Total registered users")
                .register(registry);

        Gauge.builder("cosy.users.email_unverified", userRepository, UserRepository::countEmailUnverified)
                .description("Users with unverified email")
                .register(registry);

        Gauge.builder("cosy.users.mfa_not_enabled", userRepository, UserRepository::countMfaNotEnabled)
                .description("Users without MFA enabled")
                .register(registry);

        for (Plan plan : Plan.values()) {
            Plan p = plan;
            Gauge.builder("cosy.users.by_plan", userRepository, r -> r.countByPlan(p))
                    .tag("plan", p.name().toLowerCase())
                    .description("User count by subscription plan")
                    .register(registry);
        }

        Gauge.builder("cosy.domains.total", subdomainRepository, SubdomainRepository::count)
                .description("Total registered domains")
                .register(registry);

        oAuthProperties
                .getProviders()
                .keySet()
                .forEach(
                        provider ->
                                Gauge.builder(
                                                "cosy.oauth.connections",
                                                oAuthIdentityRepository,
                                                r -> r.countByProvider(provider))
                                        .tag("provider", provider)
                                        .description("OAuth connections by provider")
                                        .register(registry));

        Gauge.builder("cosy.frontend.up", frontendUp, AtomicInteger::get)
                .description("Frontend reachability (1=up, 0=down)")
                .register(registry);
    }

    @Scheduled(fixedDelay = 30_000)
    public void checkFrontendHealth() {
        try {
            webClientBuilder
                    .build()
                    .get()
                    .uri(frontendUrl)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(5))
                    .block();
            frontendUp.set(1);
        } catch (Exception e) {
            frontendUp.set(0);
        }
    }
}
