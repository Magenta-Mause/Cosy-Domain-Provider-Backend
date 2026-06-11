package com.magentamause.cosydomainprovider.configuration.admin;

import com.magentamause.cosydomainprovider.security.adminauth.AdminAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class AdminAuthConfig {

    @Bean
    public FilterRegistrationBean<AdminAuthFilter> adminAuthFilter(AdminProperties properties) {
        FilterRegistrationBean<AdminAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AdminAuthFilter(properties));
        registration.addUrlPatterns("/api/v1/admin/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }
}
