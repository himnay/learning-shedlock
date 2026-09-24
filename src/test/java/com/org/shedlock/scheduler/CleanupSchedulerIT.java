package com.org.shedlock.scheduler;

import com.org.shedlock.support.AbstractPostgresIT;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("CleanupScheduler Integration Tests")
class CleanupSchedulerIT extends AbstractPostgresIT {

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
                    // shedlock.lock_until is TIMESTAMP (no tz) and is always written in UTC
                    // wall-clock (PostgresSqlServerTimeStatementsSource uses
                    // timezone('utc', CURRENT_TIMESTAMP) for every read/write). A bare NOW()
                    // here would instead resolve in the JDBC session's local timezone, which
                    // on a non-UTC host offsets it by the zone difference and makes lock_until
                    // look permanently in the past. Normalize NOW() to UTC to compare like-for-like.
                    List<Map<String, Object>> locks = jdbcTemplate.queryForList(
                            "SELECT lock_until > (NOW() AT TIME ZONE 'utc') AS future FROM shedlock WHERE name = 'cleanupScheduler'"
                    );
                    assertThat(locks).isNotEmpty();
                    assertThat(locks.get(0).get("future")).isEqualTo(true);
                });
    }
}
