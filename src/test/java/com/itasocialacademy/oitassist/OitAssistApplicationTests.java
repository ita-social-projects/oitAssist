package com.itasocialacademy.oitassist;


import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;


class OitAssistApplicationTests {
    ApplicationModules modules = ApplicationModules.of(OitAssistApplication.class);

    @Test
    void shouldBeCompliant() {
        modules.verify();
    }
}
