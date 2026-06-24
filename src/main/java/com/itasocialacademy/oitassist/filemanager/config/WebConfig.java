package com.itasocialacademy.oitassist.filemanager.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for serving uploaded files. Configures resource handlers to
 * map HTTP requests to uploaded file locations on the file system.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${app.storage.local.root:./uploads}")
    private String uploadDir;

    /**
     * Registers a resource handler for serving uploaded files. Maps requests to
     * /uploads/** to the configured upload directory.
     *
     * @param registry the resource handler registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + uploadPath + "/");
    }
}
