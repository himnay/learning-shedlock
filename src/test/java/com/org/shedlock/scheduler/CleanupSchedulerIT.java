package com.org.shedlock.scheduler;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("CleanupScheduler Integration Tests")
class CleanupSchedulerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("shedlock_db")
            .withUsername("shedlock")
            .withPassword("shedlock");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Cleanup scheduler creates a lock record via KeepAliveLockProvider")
    void cleanupSchedulerCreatesLockRecord() {
        Awaitility.await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    List<Map<String, Object>> locks = jdbcTemplate.queryForList(
                            "SELECT name, locked_by FROM shedlock WHERE name = 'cleanupScheduler'"
                    );
                    assertThat(locks).isNotEmpty();
                    assertThat(locks.get(0).get("name")).isEqualTo("cleanupScheduler");
                    assertThat(locks.get(0).get("locked_by")).isNotNull();
                });
    }

    @Test
    @DisplayName("Cleanup lock_until is in the future after acquisition (KeepAlive proof)")
    void cleanupLockUntilIsInFuture() {
        Awaitility.await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    List<Map<String, Object>> locks = jdbcTemplate.queryForList(
                            "SELECT lock_until > NOW() AS future FROM shedlock WHERE name = 'cleanupScheduler'"
                    );
                    assertThat(locks).isNotEmpty();
                    assertThat(locks.get(0).get("future")).isEqualTo(true);
                });
    }
}
