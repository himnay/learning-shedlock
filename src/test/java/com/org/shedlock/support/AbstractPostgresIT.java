package com.org.shedlock.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Singleton Postgres container shared by every Spring Boot test in the JVM.
 *
 * Why not {@code @Testcontainers} + {@code @Container static} per class: JUnit stops that
 * container after each class, but Spring caches the application context — with its
 * {@code @Scheduled} jobs still firing — until the JVM exits. The schedulers then hammer a
 * dead database and Hikari's connection-timeout blocks shutdown (surefire: "kill self fork
 * JVM ... 30 seconds after System.exit(0)"). One container started once and reaped by Ryuk
 * outlives every cached context.
 */
public abstract class AbstractPostgresIT {

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("shedlock_db")
            .withUsername("shedlock")
            .withPassword("shedlock");

    static {
        POSTGRES.start();
    }
}
