package com.org.shedlock.learningshedlock.config;

import com.datastax.oss.driver.api.core.CqlSession;
import net.javacrumbs.shedlock.provider.cassandra.CassandraLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableSchedulerLock (defaultLockAtMostFor = "60s")
public class CassandraShedlockConfigration {
    @Bean
    public CassandraLockProvider lockProvider(CqlSession cqlSession) {
        return new CassandraLockProvider(cqlSession);
    }
}
