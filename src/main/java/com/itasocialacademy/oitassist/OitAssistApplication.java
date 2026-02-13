package com.itasocialacademy.oitassist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulith;

@Modulith
@SpringBootApplication
@ConfigurationPropertiesScan()
public class OitAssistApplication {
    public static void main(String[] args) {
        SpringApplication.run(OitAssistApplication.class, args);
    }
}
