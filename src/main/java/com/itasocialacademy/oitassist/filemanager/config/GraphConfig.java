package com.itasocialacademy.oitassist.filemanager.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.itasocialacademy.oitassist.filemanager.properties.GraphProperties;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphConfig {
    private final GraphProperties graphProperties;

    public GraphConfig(GraphProperties graphProperties) {
        this.graphProperties = graphProperties;
    }

    @Bean
    public GraphServiceClient graphClient() {
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
            .clientId(graphProperties.getClientId())
            .clientSecret(graphProperties.getClientSecret())
            .tenantId(graphProperties.getTenantId())
            .build();

        String[] scopes = new String[] {"https://graph.microsoft.com/.default"};

        return new GraphServiceClient(credential, scopes);
    }
}
