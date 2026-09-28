package com.org.shedlock.config;

import io.micrometer.core.instrument.MeterRegistry;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.micrometer.MicrometerLockingTaskExecutorListener;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.support.KeepAliveLockProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableSchedulerLock(
        defaultLockAtMostFor = "${shedlock.default-lock-at-most-for:30s}",
        defaultLockAtLeastFor = "${shedlock.default-lock-at-least-for:10s}",
        interceptMode = EnableSchedulerLock.InterceptMode.PROXY_METHOD   // fix 7: explicit — prevents silent breakage if AOP order changes
)
@EnableConfigurationProperties(ShedlockProperties.class)
public class ShedlockConfig {

    /**
     * Primary JDBC-backed lock provider.
     * usingDbTime() avoids clock-skew across nodes by using the DB server clock.
     */
    @Bean
    @Primary
    public JdbcTemplateLockProvider jdbcLockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .withTableName("shedlock")
                        .build()
        );
    }

    /**
     * Thread that renews KeepAlive locks. A bean of its own so the context shuts it down on close
     * (devtools restarts, cached test contexts) instead of leaking one thread per context.
     */
    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService shedlockKeepAliveExecutor() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "shedlock-keepalive");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * GoF Decorator — refreshes the lock every lockAtMostFor/2 so long-running
     * tasks never lose it mid-flight. Requires lockAtMostFor >= 30 seconds.
     * Its locks can't be extended by hand: LockExtender throws UnsupportedOperationException.
     */
    @Bean
    @Qualifier("keepAliveLockProvider")
    public LockProvider keepAliveLockProvider(
            JdbcTemplateLockProvider jdbcLockProvider,
            ScheduledExecutorService shedlockKeepAliveExecutor) {
        return new KeepAliveLockProvider(jdbcLockProvider, shedlockKeepAliveExecutor);
    }

    /**
     * fix 1: Micrometer listener — publishes 5 meters per lock:
     *   shedlock.lock.attempts, shedlock.lock.acquired, shedlock.lock.not.acquired,
     *   shedlock.execution.duration, shedlock.execution.active
     *
     * registerMetricsFor() pre-creates gauges so they appear in Prometheus
     * from startup, even before the first execution.
     *
     * ShedLock's @SchedulerLock advisor looks up a LockingTaskExecutorListener bean as well, so
     * this one listener measures the annotated schedulers and the programmatic executor below.
     */
    @Bean
    public MicrometerLockingTaskExecutorListener micrometerLockListener(MeterRegistry registry) {
        var listener = new MicrometerLockingTaskExecutorListener(registry);
        listener.registerMetricsFor(
                LockNames.REPORT,
                LockNames.CLEANUP,
                LockNames.NOTIFICATION,
                LockNames.CUSTOM_LOCK
        );
        return listener;
    }

    /**
     * fix 2: LockingTaskExecutor for programmatic locking.
     * Wraps jdbcLockProvider with the Micrometer listener so CustomLockScheduler
     * gets metrics without managing unlock() in a finally block.
     */
    @Bean
    public LockingTaskExecutor lockingTaskExecutor(
            JdbcTemplateLockProvider jdbcLockProvider,
            MicrometerLockingTaskExecutorListener micrometerLockListener) {
        return new DefaultLockingTaskExecutor(jdbcLockProvider, micrometerLockListener);
    }
}
