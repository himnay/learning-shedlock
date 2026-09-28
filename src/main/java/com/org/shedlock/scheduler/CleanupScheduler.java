package com.org.shedlock.scheduler;

import com.org.shedlock.scheduler.base.AbstractScheduler;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Demonstrates KeepAliveLockProvider for long-running tasks (GoF Decorator Pattern).
 *
 * KeepAliveLockProvider vs LockExtender — pick one per task, they don't combine:
 *  - KeepAliveLockProvider: automatic background renewal — set-and-forget.
 *  - LockExtender: manual, called when the task itself detects it needs more time
 *    (see ReportScheduler). Under KeepAliveLockProvider it throws
 *    UnsupportedOperationException, because a KeepAlive lock can't be extended by hand.
 *
 * Note: KeepAliveLockProvider requires lockAtMostFor >= 30 seconds.
 */
@Slf4j
@Component
public class CleanupScheduler extends AbstractScheduler {

    /** Runs data cleanup. */
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
        // No LockExtender here: KeepAliveLockProvider already renews the lock every
        // lockAtMostFor/2 for as long as the task runs.
        simulateLongRunningCleanup();
        log.info("Data cleanup complete");
    }

    private void simulateLongRunningCleanup() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
