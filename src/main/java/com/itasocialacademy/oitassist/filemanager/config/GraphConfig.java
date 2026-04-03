package com.itasocialacademy.oitassist.filemanager.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.itasocialacademy.oitassist.filemanager.properties.GraphProperties;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import java.util.List;
import okhttp3.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphConfig {
    private final GraphProperties graphProperties;

    public GraphConfig(GraphProperties graphProperties) {
        this.graphProperties = graphProperties;
    }

    @Bean
    public GraphServiceClient<Request> graphClient() {
        ClientSecretCredential credential =
            new ClientSecretCredentialBuilder()
                .clientId(graphProperties.getClientId())
                .clientSecret(graphProperties.getClientSecret())
                .tenantId(graphProperties.getTenantId())
                .build();

        TokenCredentialAuthProvider authProvider =
            new TokenCredentialAuthProvider(
                List.of("https://graph.microsoft.com/.default"),
                credential
            );

        return GraphServiceClient.builder()
            .authenticationProvider(authProvider)
            .buildClient();
    }
}
