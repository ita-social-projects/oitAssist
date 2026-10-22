package com.itasocialacademy.oitassist;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Deliberately does NOT use {@code @Testcontainers}/{@code @Container}: those
 * tie the container's stop() to the JUnit lifecycle of whichever test class
 * triggers it first, which kills the container for every other IT class sharing
 * this singleton across the same JVM (see: Testcontainers "Singleton
 * Containers" pattern). The container is started once in this static
 * initializer and lives for the JVM's lifetime; cleanup is handled by the Ryuk
 * reaper container at JVM exit, not by JUnit.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Tag("integration")
public abstract class PostgresIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    static {
        POSTGRES.start();
    }
}