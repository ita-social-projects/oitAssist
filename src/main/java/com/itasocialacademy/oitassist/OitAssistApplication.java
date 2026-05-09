package com.itasocialacademy.oitassist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulith;

@Modulith
@ConfigurationPropertiesScan()
public class OitAssistApplication {
    static void main(String[] args) {
        SpringApplication.run(OitAssistApplication.class, args);
    }
}
