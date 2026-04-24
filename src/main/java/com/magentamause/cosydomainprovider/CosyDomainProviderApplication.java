package com.magentamause.cosydomainprovider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CosyDomainProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(CosyDomainProviderApplication.class, args);
    }
}
