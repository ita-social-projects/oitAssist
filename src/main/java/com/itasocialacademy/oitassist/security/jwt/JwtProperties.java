package com.itasocialacademy.oitassist.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String encryptedKey;
    private String signKey;
    private long validity;
    private long refreshValidity;
}