package com.org.shedlock.config;

import com.org.shedlock.support.AbstractPostgresIT;

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockExtender;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LockExtender works on locks that support extension (the JDBC provider) and not under
 * KeepAliveLockProvider, which is why ReportScheduler extends by hand and CleanupScheduler doesn't.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("LockExtender per lock provider")
class LockExtenderIT extends AbstractPostgresIT {

    @Autowired
    private JdbcTemplateLockProvider jdbcLockProvider;

    @Autowired
    @Qualifier("keepAliveLockProvider")
    private LockProvider keepAliveLockProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("extendActiveLock() pushes lock_until out on the JDBC provider")
    void extendsLockOnJdbcProvider() throws Throwable {
        LockConfiguration config = new LockConfiguration(
                Instant.now(), "lockExtenderJdbc", Duration.ofSeconds(30), Duration.ZERO);

        new DefaultLockingTaskExecutor(jdbcLockProvider).executeWithLock((LockingTaskExecutor.Task) () -> {
            LockExtender.extendActiveLock(Duration.ofMinutes(10), Duration.ZERO);

            // lock_until is UTC wall-clock time from the database clock (usingDbTime())
            Boolean extended = jdbcTemplate.queryForObject(
                    "SELECT lock_until > timezone('utc', now()) + interval '5 minutes' FROM shedlock WHERE name = 'lockExtenderJdbc'",
                    Boolean.class
            );
            assertThat(extended).isTrue();
        }, config);
    }

    @Test
    @DisplayName("extendActiveLock() is unsupported under KeepAliveLockProvider")
    void keepAliveLockCannotBeExtendedByHand() {
        LockConfiguration config = new LockConfiguration(
                Instant.now(), "lockExtenderKeepAlive", Duration.ofSeconds(30), Duration.ZERO);

        assertThatThrownBy(() -> new DefaultLockingTaskExecutor(keepAliveLockProvider).executeWithLock(
                (LockingTaskExecutor.Task) () -> LockExtender.extendActiveLock(Duration.ofMinutes(10), Duration.ZERO),
                config))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
