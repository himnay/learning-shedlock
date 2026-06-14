package com.org.shedlock.scheduler;

import com.org.shedlock.scheduler.base.AbstractScheduler;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Demonstrates KeepAliveLockProvider usage for long-running tasks.
 *
 * GoF Decorator Pattern in action:
 *   keepAliveLockProvider wraps the jdbcLockProvider and refreshes the lock
 *   every lockAtMostFor/2 interval, so the task never loses the lock mid-execution.
 *
 * @LockProviderToUse selects the named bean over the @Primary default.
 * This is the GoF Strategy Pattern: swapping out the locking strategy at the method level.
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
