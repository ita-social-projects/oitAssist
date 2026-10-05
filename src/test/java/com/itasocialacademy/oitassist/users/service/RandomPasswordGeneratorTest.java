package com.itasocialacademy.oitassist.users.service;

import com.itasocialacademy.oitassist.user.service.RandomPasswordGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit test for RandomPasswordGenerator")
class RandomPasswordGeneratorTest {
    private final RandomPasswordGenerator generator = new RandomPasswordGenerator();

    @Test
    @DisplayName("Unit test for RandomPasswordGenerator")
    void generate_ShouldReturnUrlSafeStringOf43Chars_WhenCalled() {
        String result = generator.generate();

        assertThat(result)
            .isNotNull()
            .hasSize(43)
            .matches("^[A-Za-z0-9_-]+$");
    }

    @Test
    @DisplayName("Generate should return different value on each call")
    void generate_ShouldReturnDifferentValues_WhenCalledTwice() {
        String first = generator.generate();
        String second = generator.generate();

        assertThat(first).isNotEqualTo(second);
    }
}