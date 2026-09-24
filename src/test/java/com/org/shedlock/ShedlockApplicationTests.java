package com.org.shedlock;

import com.org.shedlock.support.AbstractPostgresIT;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ShedlockApplicationTests extends AbstractPostgresIT {

    @Test
    @DisplayName("Spring application context loads successfully with Testcontainers Postgres")
    void contextLoads() {
    }
}
