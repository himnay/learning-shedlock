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
 * Demonstrates the standard @SchedulerLock usage.
 *
 * Key points:
 *  - @SchedulerLock coordinates distributed locking: only one node executes this at a time.
 *  - LockAssert.assertLocked() (in parent) verifies lock is held — acts as a safety net.
 *  - lockAtMostFor: max time the lock is held if the process dies (prevents stuck locks).
 *  - lockAtLeastFor: min time the lock is held even if the task finishes early
 *    (prevents multiple nodes racing on the same cron tick due to clock skew).
 *  - LockExtender.extendActiveLock(): extends the held lock mid-task when the task itself
 *    finds out it needs longer than lockAtMostFor. Works here because the report runs on the
 *    plain JDBC provider; KeepAlive locks can't be extended by hand (see CleanupScheduler).
 */
@Slf4j
@Component
public class ReportScheduler extends AbstractScheduler {

    /** Runs report generation. */
    @Scheduled(cron = "${shedlock.report.cron:0 */1 * * * *}")
    @SchedulerLock(
            name = "${shedlock.report.lock-name:reportScheduler}",
            lockAtMostFor = "${shedlock.report.lock-at-most-for:30s}",
            lockAtLeastFor = "${shedlock.report.lock-at-least-for:10s}"
    )
    @LockProviderToUse("jdbcLockProvider")
    public void runReportGeneration() {
        executeScheduledTask();
    }

    @Override
    protected void performTask() {
        log.info("Generating daily report at {}", LocalDateTime.now());

        if (isLargeReport()) {
            // extend the lock when runtime conditions require more time than initially estimated
            LockExtender.extendActiveLock(Duration.ofMinutes(10), Duration.ZERO);
            log.info("Large report detected — lock extended to 10 minutes");
        }

        simulateReportWork();
        log.info("Daily report generation complete");
    }

    private boolean isLargeReport() {
        return false; // placeholder — replace with a real size check
    }

    private void simulateReportWork() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
