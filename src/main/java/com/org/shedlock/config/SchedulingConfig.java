package com.org.shedlock.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Custom ThreadPoolTaskScheduler to allow parallel execution of multiple @Scheduled methods.
 *
 * Spring's default scheduler is single-threaded (ScheduledThreadPoolExecutor(1)), which means
 * all @Scheduled tasks share one thread. If one task blocks, others are delayed.
 */
@Configuration
public class SchedulingConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("shedlock-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
