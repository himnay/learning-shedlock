package com.org.shedlock.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The disable pattern for cron jobs: {@code shedlock.notification.cron=-} ({@code Scheduled.CRON_DISABLED})
 * keeps Spring from registering the task at all. Needs no database, so a plain context is enough.
 */
@DisplayName("NotificationScheduler cron configuration")
class NotificationSchedulerCronTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(SchedulingOnly.class)
            .withBean(NotificationScheduler.class);

    @Test
    @DisplayName("cron = \"-\" leaves the notification job unregistered")
    void dashDisablesTheJob() {
        runner.withPropertyValues("shedlock.notification.cron=-")
                .run(context -> assertThat(context.getBean(ScheduledTaskHolder.class).getScheduledTasks())
                        .isEmpty());
    }

    @Test
    @DisplayName("a cron expression registers the notification job with it")
    void cronRegistersTheJob() {
        runner.withPropertyValues("shedlock.notification.cron=0 */2 * * * *")
                .run(context -> assertThat(context.getBean(ScheduledTaskHolder.class).getScheduledTasks())
                        .singleElement()
                        .satisfies(task -> assertThat(((CronTask) task.getTask()).getExpression())
                                .isEqualTo("0 */2 * * * *")));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    static class SchedulingOnly {
    }
}
