package com.org.shedlock.scheduler.base;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import org.springframework.util.StopWatch;

/**
 * GoF Template Method Pattern — defines the skeleton of the scheduled task execution.
 *
 * The algorithm is:
 *   1. Assert the ShedLock is held (fail-fast if called outside of a locked context)
 *   2. Time the execution
 *   3. Delegate to the concrete subclass via performTask()
 *
 * Subclasses override performTask() to supply the business logic.
 */
@Slf4j
public abstract class AbstractScheduler {

    public void executeScheduledTask() {
        LockAssert.assertLocked();

        String taskName = getClass().getSimpleName();
        StopWatch stopWatch = new StopWatch(taskName);
        stopWatch.start(taskName);

        log.info("[{}] Starting scheduled task", taskName);
        try {
            performTask();
            stopWatch.stop();
            log.info("[{}] Completed in {} ms", taskName, stopWatch.getLastTaskTimeMillis());
        } catch (Exception ex) {
            stopWatch.stop();
            log.error("[{}] Failed after {} ms: {}", taskName, stopWatch.getLastTaskTimeMillis(), ex.getMessage(), ex);
            throw ex;
        }
    }

    protected abstract void performTask();
}
