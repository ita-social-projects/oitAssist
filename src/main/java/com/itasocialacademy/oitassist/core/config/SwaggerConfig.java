package com.itasocialacademy.oitassist.core.config;

import com.itasocialacademy.oitassist.core.properties.SwaggerProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class SwaggerConfig {
    private final SwaggerProperties swaggerProperties;

    /**
     * Customizing the OpenAPI bean.
     *
     * @return {@link OpenAPI}
     */
    @Bean
    public OpenAPI customizeOpenAPI() {
        final String securitySchemeName = "Bearer Authentication";

        List<Server> servers = swaggerProperties.servers()
            .stream()
            .map(s -> new Server()
                .url(s.url())
                .description(s.description()))
            .toList();

        return new OpenAPI()
            .info(new Info()
                .title("OITAssist API")
                .version("1.0.0"))
            .servers(servers)
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name("Authorization")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
