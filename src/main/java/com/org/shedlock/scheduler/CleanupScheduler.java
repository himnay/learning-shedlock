package com.org.shedlock.scheduler;

import com.org.shedlock.scheduler.base.AbstractScheduler;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockExtender;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Demonstrates KeepAliveLockProvider for long-running tasks (GoF Decorator Pattern)
 * and LockExtender for manual mid-task lock extension (fix 3).
 *
 * KeepAliveLockProvider vs LockExtender:
 *  - KeepAliveLockProvider: automatic background renewal — set-and-forget.
 *  - LockExtender: manual, called when the task itself detects it needs more time.
 *    Use when renewal logic depends on runtime state (e.g. record count remaining).
 *
 * Note: KeepAliveLockProvider requires lockAtMostFor >= 30 seconds.
 */
@Slf4j
@Component
public class CleanupScheduler extends AbstractScheduler {

    @Scheduled(fixedRateString = "${shedlock.cleanup.fixed-rate-ms:60000}")
    @SchedulerLock(
            name = "${shedlock.cleanup.lock-name:cleanupScheduler}",
            lockAtMostFor = "${shedlock.cleanup.lock-at-most-for:5m}",
            lockAtLeastFor = "${shedlock.cleanup.lock-at-least-for:1m}"
    )
    @LockProviderToUse("keepAliveLockProvider")
    public void runDataCleanup() {
        executeScheduledTask();
    }

    @Override
    protected void performTask() {
        log.info("Starting data cleanup at {}", LocalDateTime.now());

        if (isLargeDatasetDetected()) {
            // fix 3: extend the lock when runtime conditions require more time than initially estimated
            LockExtender.extendActiveLock(Duration.ofMinutes(10), Duration.ZERO);
            log.info("Large dataset detected — lock extended by 10 minutes");
        }

        simulateLongRunningCleanup();
        log.info("Data cleanup complete");
    }

    private boolean isLargeDatasetDetected() {
        return false; // placeholder — replace with real record-count check
    }

    private void simulateLongRunningCleanup() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
