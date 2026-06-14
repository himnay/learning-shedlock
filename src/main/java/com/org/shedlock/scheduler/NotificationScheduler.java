package com.org.shedlock.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Demonstrates cron-based ShedLock scheduling with a disable pattern.
 *
 * Cron format (Spring 6-field):
 *   second minute hour day-of-month month day-of-week
 *
 * Disabling a cron job:
 *   - Set cron = "-"  (Spring Boot 2.1+)
 *   - Or set cron = "59 59 23 31 12 ? 2099"  (run far in the future)
 *
 * This scheduler does NOT extend AbstractScheduler to demonstrate inline LockAssert usage.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    @Scheduled(cron = "${shedlock.notification.cron:0 */2 * * * *}")
    @SchedulerLock(
            name = "${shedlock.notification.lock-name:notificationScheduler}",
            lockAtMostFor = "${shedlock.notification.lock-at-most-for:30s}",
            lockAtLeastFor = "${shedlock.notification.lock-at-least-for:10s}"
    )
    @LockProviderToUse("jdbcLockProvider")
    public void sendNotifications() {
        LockAssert.assertLocked();

        log.info("Sending email notifications at {}", LocalDateTime.now());
        processNotifications();
        log.info("Notifications sent successfully");
    }

    /**
     * Disabled scheduler example — cron = "-" disables the job entirely.
     * Activate by overriding: shedlock.notification.disabled-cron=-
     *
     *   @Scheduled(cron = "${shedlock.notification.disabled-cron:59 59 23 31 12 ? 2099}")
     *   @SchedulerLock(name = "disabledScheduler")
     *   public void disabledScheduler() { ... }
     */
    private void processNotifications() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
