package com.org.shedlock.scheduler;

import com.org.shedlock.config.ShedlockProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Demonstrates programmatic locking via LockingTaskExecutor (fix 2).
 *
 * LockingTaskExecutor vs raw LockProvider.lock():
 *  - Handles unlock() automatically — no risk of forgetting the finally block.
 *  - Integrates with LockingTaskExecutorListener (Micrometer metrics).
 *  - Skips silently when lock is unavailable; metrics capture lock.not.acquired.
 *
 * GoF Factory Method Pattern: createLockConfiguration() encapsulates
 * LockConfiguration construction details.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLockScheduler {

    private final LockingTaskExecutor lockingTaskExecutor;
    private final ShedlockProperties properties;

    @Scheduled(fixedRateString = "${shedlock.custom-lock.fixed-rate-ms:90000}")
    public void runWithProgrammaticLock() {
        ShedlockProperties.CustomLock config = properties.getCustomLock();
        try {
            lockingTaskExecutor.executeWithLock(
                    (LockingTaskExecutor.Task) this::executeBusinessLogic,
                    createLockConfiguration(config)
            );
        } catch (Throwable ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                return;
            }
            log.error("[{}] Task execution failed", config.getLockName(), ex);
        }
    }

    private LockConfiguration createLockConfiguration(ShedlockProperties.CustomLock config) {
        return new LockConfiguration(
                Instant.now(),
                config.getLockName(),
                config.getLockAtMostFor(),
                config.getLockAtLeastFor()
        );
    }

    private void executeBusinessLogic() throws InterruptedException {
        log.info("[{}] Acquired lock, executing task", properties.getCustomLock().getLockName());
        Thread.sleep(300);
        log.info("[{}] Task complete", properties.getCustomLock().getLockName());
    }
}
