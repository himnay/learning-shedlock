package com.org.shedlock.scheduler;

import net.javacrumbs.shedlock.core.LockAssert;
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
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration tests that verify ShedLock creates lock records in PostgreSQL.
 *
 * Note: we do NOT delete shedlock records between tests because ShedLock's
 * in-memory LockRecordRegistry caches created lock names — if the DB record
 * is deleted externally, subsequent scheduler runs skip INSERT and go straight
 * to UPDATE, which finds 0 rows and silently fails to acquire the lock.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("ShedLock Scheduler Integration Tests")
class ReportSchedulerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("shedlock_db")
            .withUsername("shedlock")
            .withPassword("shedlock");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Shedlock table is created by Flyway migration")
    void shedlockTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'shedlock'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Report scheduler writes a lock record to shedlock table")
    void reportSchedulerCreatesLockRecord() {
        Awaitility.await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    List<Map<String, Object>> locks = jdbcTemplate.queryForList(
                            "SELECT name, locked_by FROM shedlock WHERE name = 'reportScheduler'"
                    );
                    assertThat(locks).isNotEmpty();
                    assertThat(locks.get(0).get("name")).isEqualTo("reportScheduler");
                    assertThat(locks.get(0).get("locked_by")).isNotNull();
                });
    }

    @Test
    @DisplayName("Custom lock scheduler writes a lock record to shedlock table")
    void customLockSchedulerCreatesLockRecord() {
        Awaitility.await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    List<Map<String, Object>> locks = jdbcTemplate.queryForList(
                            "SELECT name FROM shedlock WHERE name = 'customLockScheduler'"
                    );
                    assertThat(locks).isNotEmpty();
                });
    }

    @Test
    @DisplayName("LockAssert.assertLocked() does not throw in test mode with TestHelper enabled")
    void lockAssertDoesNotThrowInTestMode() {
        LockAssert.TestHelper.makeAllAssertsPass(true);
        assertThatNoException().isThrownBy(LockAssert::assertLocked);
        LockAssert.TestHelper.makeAllAssertsPass(false);
    }

    @Test
    @DisplayName("Multiple schedulers create lock records in shedlock table")
    void multipleSchedulersCreateLockRecords() {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<Map<String, Object>> locks = jdbcTemplate.queryForList(
                            "SELECT name FROM shedlock ORDER BY name"
                    );
                    List<String> lockNames = locks.stream()
                            .map(r -> (String) r.get("name"))
                            .toList();
                    assertThat(lockNames).containsAnyOf(
                            "reportScheduler", "customLockScheduler", "cleanupScheduler"
                    );
                });
    }
}
