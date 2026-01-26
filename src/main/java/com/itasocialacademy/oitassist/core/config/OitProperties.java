package com.itasocialacademy.oitassist.core.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "oit")
@Data
public class OitProperties {
    private Integer count;
    private BigDecimal price;
    private Boolean enabled;
    private List<String> colors;


    private List<Route> routes;
    private Person person;
    private String description;
    private String message;
    private int number;
    private String code;


    private Duration timeout;
    private Duration cache;
    private LocalDate created;
    private Instant event;


    @Data
    public static class Route {
        private String path;
        private List<String> methods;
    }


    @Data
    public static class Person {
        private String name;
        private Integer age;
    }
}
