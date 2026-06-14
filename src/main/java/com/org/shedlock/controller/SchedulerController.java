package com.org.shedlock.controller;

import com.org.shedlock.model.SchedulerStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST API to inspect and monitor ShedLock scheduler state.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/schedulers")
@RequiredArgsConstructor
public class SchedulerController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public ResponseEntity<List<SchedulerStatus>> getAllSchedulers() {
        return ResponseEntity.ok(List.of(
                SchedulerStatus.builder()
                        .name("reportScheduler")
                        .type("CRON")
                        .schedule("0 */1 * * * *")
                        .lockProvider("jdbcLockProvider (Primary)")
                        .lockAtMostFor("30s")
                        .lockAtLeastFor("10s")
                        .description("Generates reports every minute — basic @SchedulerLock with LockAssert")
                        .retrievedAt(LocalDateTime.now())
                        .build(),
                SchedulerStatus.builder()
                        .name("cleanupScheduler")
                        .type("FIXED_RATE")
                        .schedule("every 60s")
                        .lockProvider("keepAliveLockProvider (Decorator)")
                        .lockAtMostFor("5m")
                        .lockAtLeastFor("1m")
                        .description("Long-running data cleanup — uses KeepAliveLockProvider via @LockProviderToUse")
                        .retrievedAt(LocalDateTime.now())
                        .build(),
                SchedulerStatus.builder()
                        .name("notificationScheduler")
                        .type("CRON")
                        .schedule("0 */2 * * * *")
                        .lockProvider("jdbcLockProvider (Primary)")
                        .lockAtMostFor("30s")
                        .lockAtLeastFor("10s")
                        .description("Email notifications every 2 minutes — inline LockAssert pattern")
                        .retrievedAt(LocalDateTime.now())
                        .build(),
                SchedulerStatus.builder()
                        .name("customLockScheduler")
                        .type("FIXED_RATE")
                        .schedule("every 90s")
                        .lockProvider("jdbcLockProvider (Programmatic)")
                        .lockAtMostFor("1m")
                        .lockAtLeastFor("1m")
                        .description("Custom programmatic lock — uses LockProvider.lock() directly")
                        .retrievedAt(LocalDateTime.now())
                        .build()
        ));
    }

    @GetMapping("/locks")
    public ResponseEntity<List<Map<String, Object>>> getActiveLocks() {
        List<Map<String, Object>> locks = jdbcTemplate.queryForList(
                "SELECT name, lock_until, locked_at, locked_by FROM shedlock ORDER BY locked_at DESC"
        );
        return ResponseEntity.ok(locks);
    }
}
