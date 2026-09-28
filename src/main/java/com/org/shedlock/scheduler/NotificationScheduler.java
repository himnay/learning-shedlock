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
 *   - Set cron = "-" (Scheduled.CRON_DISABLED, Spring Framework 5.1+), e.g.
 *     shedlock.notification.cron=- : the task is then never registered.
 *   - Quartz-style tricks such as "59 59 23 31 12 ? 2099" don't work: Spring's cron takes
 *     exactly 6 fields (no year) and fails at startup on a 7th.
 *
 * This scheduler does NOT extend AbstractScheduler to demonstrate inline LockAssert usage.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    /** Sends notifications. */
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

    private void processNotifications() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
