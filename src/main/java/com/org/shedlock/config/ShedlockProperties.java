package com.org.shedlock.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Validated
@ConfigurationProperties(prefix = "shedlock")
public class ShedlockProperties {

    @NotNull
    private Duration defaultLockAtMostFor = Duration.ofSeconds(30);

    @NotNull
    private Duration defaultLockAtLeastFor = Duration.ofSeconds(10);

    private boolean keepAliveEnabled = true;

    @Valid
    private Report report = new Report();

    @Valid
    private Cleanup cleanup = new Cleanup();

    @Valid
    private Notification notification = new Notification();

    @Valid
    private CustomLock customLock = new CustomLock();

    @Data
    public static class Report {
        private String lockName = "reportScheduler";
        private Duration lockAtMostFor = Duration.ofSeconds(30);
        private Duration lockAtLeastFor = Duration.ofSeconds(10);
        private String cron = "0 */1 * * * *";
    }

    @Data
    public static class Cleanup {
        private String lockName = "cleanupScheduler";
        private Duration lockAtMostFor = Duration.ofMinutes(5);
        private Duration lockAtLeastFor = Duration.ofMinutes(1);
        private long fixedRateMs = 60000;
    }

    @Data
    public static class Notification {
        private String lockName = "notificationScheduler";
        private Duration lockAtMostFor = Duration.ofSeconds(30);
        private Duration lockAtLeastFor = Duration.ofSeconds(10);
        private String cron = "0 */2 * * * *";
        private boolean enabled = true;
    }

    @Data
    public static class CustomLock {
        private String lockName = "customLockScheduler";
        private Duration lockAtMostFor = Duration.ofMinutes(1);
        private Duration lockAtLeastFor = Duration.ofMinutes(1);
        private long fixedRateMs = 90000;
    }
}
