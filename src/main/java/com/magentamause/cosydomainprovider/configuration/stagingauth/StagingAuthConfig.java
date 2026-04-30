package com.magentamause.cosydomainprovider.configuration.stagingauth;

import com.magentamause.cosydomainprovider.security.stagingauth.StagingAuthFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@EnableConfigurationProperties({StagingAuthProperties.class, StagingCaptchaBypassProperties.class})
public class StagingAuthConfig {

    @Bean
    public FilterRegistrationBean<StagingAuthFilter> stagingAuthFilter(
            StagingAuthProperties properties) {
        FilterRegistrationBean<StagingAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new StagingAuthFilter(properties));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
