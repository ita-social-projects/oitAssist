package com.itasocialacademy.oitassist.filemanager.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "graph")
public class GraphProperties {
    private String tenantId;
    private String clientId;
    private String clientSecret;
    private String driveId;
}
