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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that CustomLockScheduler skips execution when the lock is already held
 * by another node — the core non-blocking tryLock guarantee.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("CustomLockScheduler Integration Tests")
class CustomLockSchedulerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("shedlock_db")
            .withUsername("shedlock")
            .withPassword("shedlock");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Custom lock scheduler creates a lock record via LockingTaskExecutor")
    void customLockSchedulerCreatesLockRecord() {
        Awaitility.await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    List<Map<String, Object>> locks = jdbcTemplate.queryForList(
                            "SELECT name, locked_by FROM shedlock WHERE name = 'customLockScheduler'"
                    );
                    assertThat(locks).isNotEmpty();
                    assertThat(locks.get(0).get("name")).isEqualTo("customLockScheduler");
                });
    }

    @Test
    @DisplayName("CustomLockScheduler skips execution when lock is already held by another node")
    void skipsExecutionWhenLockAlreadyHeld() throws InterruptedException {
        // Simulate another node holding the lock for 5 minutes
        Timestamp lockUntil = Timestamp.from(Instant.now().plus(5, ChronoUnit.MINUTES));
        Timestamp lockedAt  = Timestamp.from(Instant.now());

        jdbcTemplate.update(
                """
                INSERT INTO shedlock (name, lock_until, locked_at, locked_by)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (name) DO UPDATE
                  SET lock_until = EXCLUDED.lock_until,
                      locked_at  = EXCLUDED.locked_at,
                      locked_by  = EXCLUDED.locked_by
                """,
                "customLockScheduler", lockUntil, lockedAt, "other-node:8080"
        );

        // Wait 2 scheduler cycles (fixed-rate = 7 s in test profile)
        Thread.sleep(15_000);

        // Lock must still be held by other-node — our scheduler must have skipped
        Map<String, Object> lock = jdbcTemplate.queryForMap(
                "SELECT locked_by FROM shedlock WHERE name = 'customLockScheduler'"
        );
        assertThat(lock.get("locked_by")).isEqualTo("other-node:8080");
    }
}
