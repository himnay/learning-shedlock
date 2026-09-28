package com.org.shedlock.controller;

import com.org.shedlock.actuator.ShedlockInfoContributor;
import com.org.shedlock.model.SchedulerStatus;
import com.org.shedlock.support.AbstractPostgresIT;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("SchedulerController and ShedlockInfoContributor Integration Tests")
class SchedulerControllerIT extends AbstractPostgresIT {

    @Autowired
    private SchedulerController controller;

    @Autowired
    private ShedlockInfoContributor infoContributor;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Scheduler metadata follows the active shedlock.* configuration (test profile)")
    void schedulerMetadataFollowsConfiguration() {
        assertThat(controller.getAllSchedulers().getBody())
                .extracting(SchedulerStatus::name, SchedulerStatus::schedule,
                        SchedulerStatus::lockAtMostFor, SchedulerStatus::lockAtLeastFor)
                .containsExactly(
                        tuple("reportScheduler", "*/5 * * * * *", "30s", "10s"),
                        tuple("cleanupScheduler", "every 5s", "30s", "2s"),
                        tuple("notificationScheduler", "*/10 * * * * *", "30s", "10s"),
                        tuple("customLockScheduler", "every 7s", "30s", "2s"));
    }

    @Test
    @DisplayName("Lock times come back as the right instants, whatever the JVM time zone")
    void lockTimesAreUtcInstants() {
        // Written the way ShedLock writes it with usingDbTime(): UTC wall-clock time.
        jdbcTemplate.update("""
                INSERT INTO shedlock (name, lock_until, locked_at, locked_by)
                VALUES ('timeZoneProbe', timezone('utc', now()) + interval '1 hour', timezone('utc', now()), 'probe')
                ON CONFLICT (name) DO UPDATE
                  SET lock_until = EXCLUDED.lock_until, locked_at = EXCLUDED.locked_at
                """);
        Instant expectedLockUntil = Instant.now().plus(1, ChronoUnit.HOURS);

        assertThat(instant(probe(controller.getActiveLocks().getBody()).get("lock_until")))
                .isCloseTo(expectedLockUntil, within(1, ChronoUnit.MINUTES));

        Info.Builder info = new Info.Builder();
        infoContributor.contribute(info);
        @SuppressWarnings("unchecked")
        Map<String, Object> shedlock = (Map<String, Object>) info.build().get("shedlock");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> locks = (List<Map<String, Object>>) shedlock.get("locks");
        assertThat(instant(probe(locks).get("lock_until")))
                .isCloseTo(expectedLockUntil, within(1, ChronoUnit.MINUTES));
    }

    private static Map<String, Object> probe(List<Map<String, Object>> locks) {
        return locks.stream()
                .filter(row -> "timeZoneProbe".equals(row.get("name")))
                .findFirst()
                .orElseThrow();
    }

    private static Instant instant(Object value) {
        return switch (value) {
            case Timestamp timestamp -> timestamp.toInstant();
            case OffsetDateTime dateTime -> dateTime.toInstant();
            default -> throw new AssertionError("unexpected lock time type " + value.getClass());
        };
    }
}
