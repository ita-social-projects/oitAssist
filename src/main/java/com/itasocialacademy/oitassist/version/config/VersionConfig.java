package com.itasocialacademy.oitassist.version.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:frontend-info.properties", ignoreResourceNotFound = true)
public class VersionConfig {
}
