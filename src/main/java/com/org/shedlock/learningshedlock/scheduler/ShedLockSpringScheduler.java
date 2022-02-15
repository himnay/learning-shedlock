package com.org.shedlock.learningshedlock.scheduler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class ShedLockSpringScheduler {

  private static final Logger log = LoggerFactory.getLogger(ShedLockSpringScheduler.class);

  @Scheduled (fixedRateString = "${contentService.db-config.fileSchedulerTrigger.schedulerTriggerMilliSeconds:60000}")
  @SchedulerLock (name = "fileLocalizationSchedulerTrigger", lockAtMostFor = "60s", lockAtLeastFor = "60s")
  public void fileLocalizationSchedulerTrigger() throws IOException {

    LockAssert.assertLocked();

    CompletableFuture.runAsync(() -> {
      // add your business logic here
    });
  }
}
