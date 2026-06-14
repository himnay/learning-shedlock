package com.org.shedlock.scheduler;

import com.org.shedlock.config.ShedlockProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Demonstrates programmatic (manual) locking using LockProvider directly.
 *
 * Use this approach when:
 *  - You need fine-grained control over lock acquisition and release.
 *  - You want to skip execution if the lock is unavailable (non-blocking tryLock).
 *  - The task must run in a different thread than the scheduler thread.
 *
 * GoF Factory Method Pattern: createLockConfiguration() encapsulates
 * LockConfiguration construction details.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLockScheduler {

    private final LockProvider lockProvider;
    private final ShedlockProperties properties;

    @Scheduled(fixedRateString = "${shedlock.custom-lock.fixed-rate-ms:90000}")
    public void runWithProgrammaticLock() {
        ShedlockProperties.CustomLock config = properties.getCustomLock();
        LockConfiguration lockConfig = createLockConfiguration(config);

        Optional<SimpleLock> lock = lockProvider.lock(lockConfig);
        if (lock.isEmpty()) {
            log.debug("[{}] Lock not available — another node is running this task, skipping", config.getLockName());
            return;
        }

        try {
            log.info("[{}] Acquired programmatic lock, executing task", config.getLockName());
            executeBusinessLogic();
            log.info("[{}] Task complete, releasing lock", config.getLockName());
        } finally {
            lock.get().unlock();
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

    private void executeBusinessLogic() {
        try {
            Thread.sleep(300);
            log.info("Custom lock business logic executed successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
