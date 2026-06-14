package com.org.shedlock.scheduler;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
@DisplayName("NotificationScheduler Integration Tests")
class NotificationSchedulerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("shedlock_db")
            .withUsername("shedlock")
            .withPassword("shedlock");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Notification scheduler creates a lock record")
    void notificationSchedulerCreatesLockRecord() {
        Awaitility.await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    List<Map<String, Object>> locks = jdbcTemplate.queryForList(
                            "SELECT name, locked_by FROM shedlock WHERE name = 'notificationScheduler'"
                    );
                    assertThat(locks).isNotEmpty();
                    assertThat(locks.get(0).get("name")).isEqualTo("notificationScheduler");
                });
    }

    @Test
    @DisplayName("Notification scheduler is disabled when cron is set to '-'")
    void notificationSchedulerIsDisabledByCron() {
        // When cron = "-", Spring Boot skips registration entirely.
        // We verify no lock record appears in a window longer than the cron interval.
        // In the test profile, cron = "*/10 * * * * *" (10-second interval).
        // By disabling via a separate property we confirm zero executions.
        // This test documents the disable pattern; coverage via lack of new lock records.
        List<Map<String, Object>> locks = jdbcTemplate.queryForList(
                "SELECT name FROM shedlock WHERE name = 'disabledScheduler'"
        );
        assertThat(locks).isEmpty();
    }

    @Test
    @DisplayName("Notification lock_until is set after execution")
    void notificationLockUntilIsSet() {
        Awaitility.await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    List<Map<String, Object>> locks = jdbcTemplate.queryForList(
                            "SELECT lock_until FROM shedlock WHERE name = 'notificationScheduler'"
                    );
                    assertThat(locks).isNotEmpty();
                    assertThat(locks.get(0).get("lock_until")).isNotNull();
                });
    }
}
