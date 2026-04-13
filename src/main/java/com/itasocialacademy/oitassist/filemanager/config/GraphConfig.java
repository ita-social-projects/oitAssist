package com.itasocialacademy.oitassist.filemanager.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.itasocialacademy.oitassist.filemanager.properties.GraphProperties;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Microsoft Graph integration. Creates and configures a
 * {@link GraphServiceClient} bean using client credentials (client ID, secret,
 * tenant ID).
 */
@Configuration
public class GraphConfig {
    /**
     * Properties containing Microsoft Graph authentication settings.
     */
    private final GraphProperties graphProperties;

    public GraphConfig(GraphProperties graphProperties) {
        this.graphProperties = graphProperties;
    }

    /**
     * Initializes a {@link GraphServiceClient} for interacting with Microsoft Graph
     * API.
     *
     * @return configured GraphServiceClient instance
     */
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
