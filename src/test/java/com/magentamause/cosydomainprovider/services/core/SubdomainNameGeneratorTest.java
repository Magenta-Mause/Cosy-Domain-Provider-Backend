package com.magentamause.cosydomainprovider.services.core;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SubdomainNameGeneratorTest {

    private final SubdomainNameGenerator generator = new SubdomainNameGenerator();

    @Test
    void generate_returnsHyphenatedName() {
        String name = generator.generate();
        assertThat(name).contains("-");
        assertThat(name.split("-")).hasSize(2);
    }

    @Test
    void generate_returnsLowercaseOnly() {
        for (int i = 0; i < 20; i++) {
            assertThat(generator.generate()).isLowerCase();
        }
    }

    @Test
    void poolSize_isPositive() {
        assertThat(generator.poolSize()).isGreaterThan(100);
    }
}
