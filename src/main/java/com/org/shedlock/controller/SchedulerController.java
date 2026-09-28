package com.org.shedlock.controller;

import com.org.shedlock.config.ShedlockProperties;
import com.org.shedlock.model.SchedulerStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST API to inspect and monitor ShedLock scheduler state.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/schedulers")
public class SchedulerController {

    /**
     * lock_until/locked_at are TIMESTAMP columns holding UTC wall-clock time (usingDbTime() writes
     * timezone('utc', CURRENT_TIMESTAMP)). Read bare, the JDBC driver would place them in the JVM's
     * time zone and shift every instant by its UTC offset; AT TIME ZONE 'UTC' returns the real instant.
     */
    private static final String LOCKS_SQL = """
            SELECT name,
                   lock_until AT TIME ZONE 'UTC' AS lock_until,
                   locked_at  AT TIME ZONE 'UTC' AS locked_at,
                   locked_by
            FROM shedlock
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ShedlockProperties properties;

    /** Scheduler metadata, read from the same shedlock.* properties the schedulers run with. */
    @GetMapping
    public ResponseEntity<List<SchedulerStatus>> getAllSchedulers() {
        LocalDateTime now = LocalDateTime.now();
        ShedlockProperties.Report report = properties.getReport();
        ShedlockProperties.Cleanup cleanup = properties.getCleanup();
        ShedlockProperties.Notification notification = properties.getNotification();
        ShedlockProperties.CustomLock customLock = properties.getCustomLock();
        return ResponseEntity.ok(List.of(
                SchedulerStatus.builder()
                        .name(report.getLockName())
                        .type("CRON")
                        .schedule(report.getCron())
                        .lockProvider("jdbcLockProvider (Primary)")
                        .lockAtMostFor(format(report.getLockAtMostFor()))
                        .lockAtLeastFor(format(report.getLockAtLeastFor()))
                        .description("Generates reports — basic @SchedulerLock with LockAssert and LockExtender")
                        .retrievedAt(now)
                        .build(),
                SchedulerStatus.builder()
                        .name(cleanup.getLockName())
                        .type("FIXED_RATE")
                        .schedule("every " + format(Duration.ofMillis(cleanup.getFixedRateMs())))
                        .lockProvider("keepAliveLockProvider (Decorator)")
                        .lockAtMostFor(format(cleanup.getLockAtMostFor()))
                        .lockAtLeastFor(format(cleanup.getLockAtLeastFor()))
                        .description("Long-running data cleanup — uses KeepAliveLockProvider via @LockProviderToUse")
                        .retrievedAt(now)
                        .build(),
                SchedulerStatus.builder()
                        .name(notification.getLockName())
                        .type("CRON")
                        .schedule(notification.getCron())
                        .lockProvider("jdbcLockProvider (Primary)")
                        .lockAtMostFor(format(notification.getLockAtMostFor()))
                        .lockAtLeastFor(format(notification.getLockAtLeastFor()))
                        .description("Email notifications — inline LockAssert pattern; cron \"-\" disables it")
                        .retrievedAt(now)
                        .build(),
                SchedulerStatus.builder()
                        .name(customLock.getLockName())
                        .type("FIXED_RATE")
                        .schedule("every " + format(Duration.ofMillis(customLock.getFixedRateMs())))
                        .lockProvider("jdbcLockProvider (Programmatic)")
                        .lockAtMostFor(format(customLock.getLockAtMostFor()))
                        .lockAtLeastFor(format(customLock.getLockAtLeastFor()))
                        .description("Programmatic lock — LockingTaskExecutor.executeWithLock()")
                        .retrievedAt(now)
                        .build()
        ));
    }

    @GetMapping("/locks")
    public ResponseEntity<List<Map<String, Object>>> getActiveLocks() {
        List<Map<String, Object>> locks = jdbcTemplate.queryForList(LOCKS_SQL + "ORDER BY locked_at DESC");
        return ResponseEntity.ok(locks);
    }

    /** Formats a duration the way application.yml writes it: 30s, 5m, 1500ms. */
    static String format(Duration duration) {
        long ms = duration.toMillis();
        if (ms != 0 && ms % 60_000 == 0) {
            return ms / 60_000 + "m";
        }
        return ms % 1000 == 0 ? ms / 1000 + "s" : ms + "ms";
    }
}
