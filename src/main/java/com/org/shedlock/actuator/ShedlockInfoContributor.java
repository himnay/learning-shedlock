package com.org.shedlock.actuator;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Exposes ShedLock table state at /actuator/info.
 */
@Component
@RequiredArgsConstructor
public class ShedlockInfoContributor implements InfoContributor {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void contribute(Info.Builder builder) {
        try {
            // AT TIME ZONE 'UTC': the columns hold UTC wall-clock time, see SchedulerController
            List<Map<String, Object>> locks = jdbcTemplate.queryForList("""
                    SELECT name,
                           lock_until AT TIME ZONE 'UTC' AS lock_until,
                           locked_at  AT TIME ZONE 'UTC' AS locked_at,
                           locked_by
                    FROM shedlock ORDER BY name
                    """);
            builder.withDetail("shedlock", Map.of(
                    "tableExists", true,
                    "lockCount", locks.size(),
                    "locks", locks
            ));
        } catch (Exception ex) {
            builder.withDetail("shedlock", Map.of(
                    "tableExists", false,
                    "error", Objects.toString(ex.getMessage(), ex.getClass().getName())
            ));
        }
    }
}
