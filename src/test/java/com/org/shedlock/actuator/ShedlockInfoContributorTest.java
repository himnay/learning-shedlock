package com.org.shedlock.actuator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ShedlockInfoContributor")
class ShedlockInfoContributorTest {

    @Test
    @DisplayName("an exception without a message still produces the info entry (Map.of rejects null)")
    void exceptionWithoutMessage() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString())).thenThrow(new IllegalStateException());

        Info.Builder builder = new Info.Builder();
        new ShedlockInfoContributor(jdbcTemplate).contribute(builder);

        @SuppressWarnings("unchecked")
        Map<String, Object> shedlock = (Map<String, Object>) builder.build().get("shedlock");
        assertThat(shedlock)
                .containsEntry("tableExists", false)
                .containsEntry("error", IllegalStateException.class.getName());
    }
}
