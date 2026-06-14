package com.org.shedlock.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.support.KeepAliveLockProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.concurrent.Executors;

/**
 * ShedLock configuration.
 *
 * Two lock providers are registered:
 *   1. jdbcLockProvider  — standard JDBC-backed provider (Primary, used by most schedulers)
 *   2. keepAliveLockProvider — Decorator (GoF Decorator pattern) wrapping the JDBC provider;
 *      periodically refreshes the lock so long-running tasks don't lose it mid-flight.
 *
 * @EnableSchedulerLock activates AOP-based locking via MethodProxyScheduledLockAdvisor.
 */
@Configuration
@EnableSchedulerLock(
        defaultLockAtMostFor = "${shedlock.default-lock-at-most-for:30s}",
        defaultLockAtLeastFor = "${shedlock.default-lock-at-least-for:10s}"
)
@EnableConfigurationProperties(ShedlockProperties.class)
public class ShedlockConfig {

    /**
     * Primary JDBC-backed lock provider.
     *
     * Configuration notes:
     *  - usingDbTime()  : uses the DB server clock instead of the application clock to
     *                     avoid clock-skew issues across nodes (recommended for production).
     *  - withTableName(): explicit table name (default is already "shedlock").
     */
    @Bean
    @Primary
    public JdbcTemplateLockProvider jdbcLockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .withTableName("shedlock")
                        .build()
        );
    }

    /**
     * GoF Decorator Pattern — wraps the JDBC provider and keeps the lock alive by
     * periodically extending it. Use for tasks that may run longer than lockAtMostFor.
     *
     * Minimum lockAtMostFor supported by KeepAliveLockProvider is 30 seconds.
     */
    @Bean
    @Qualifier("keepAliveLockProvider")
    public LockProvider keepAliveLockProvider(JdbcTemplateLockProvider jdbcLockProvider) {
        return new KeepAliveLockProvider(
                jdbcLockProvider,
                Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "shedlock-keepalive");
                    t.setDaemon(true);
                    return t;
                })
        );
    }
}
