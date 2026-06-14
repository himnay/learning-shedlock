# Learning ShedLock

Production-grade Spring Boot demonstration of **ShedLock** — distributed scheduler locking with JDBC/PostgreSQL, KeepAlive, programmatic locking, Flyway, Prometheus, and TestContainers.

## Stack

| Component          | Version / Detail                              |
|--------------------|-----------------------------------------------|
| Java               | 25                                            |
| Spring Boot        | 4.1.0 (via super-pom)                        |
| ShedLock           | 6.9.2                                         |
| Lock Provider      | JdbcTemplateLockProvider (PostgreSQL)         |
| Database           | PostgreSQL 16                                 |
| Migrations         | Flyway                                        |
| Observability      | Micrometer + Prometheus + Grafana             |
| Tests              | JUnit 5 + TestContainers + Awaitility         |
| Build              | Maven 3.9+                                    |

---

## ShedLock Concepts Demonstrated

### 1. Standard `@SchedulerLock` (ReportScheduler)
```java
@Scheduled(cron = "0 */1 * * * *")
@SchedulerLock(name = "reportScheduler", lockAtMostFor = "30s", lockAtLeastFor = "10s")
public void runReportGeneration() { ... }
```
- Only one node executes per cron tick
- `LockAssert.assertLocked()` verifies lock ownership inside the task

### 2. KeepAliveLockProvider — Decorator Pattern (CleanupScheduler)
```java
@SchedulerLock(name = "cleanupScheduler", lockAtMostFor = "5m", lockAtLeastFor = "1m")
@LockProviderToUse("keepAliveLockProvider")
public void runDataCleanup() { ... }
```
- `KeepAliveLockProvider` wraps `JdbcTemplateLockProvider` (GoF Decorator)
- Refreshes the lock every `lockAtMostFor/2`, preventing premature expiry on long tasks
- Requires `lockAtMostFor >= 30s`

### 3. Programmatic Locking (CustomLockScheduler)
```java
Optional<SimpleLock> lock = lockProvider.lock(lockConfig);
if (lock.isEmpty()) return;  // another node holds it — skip
try {
    executeBusinessLogic();
} finally {
    lock.get().unlock();
}
```
- Full control over lock acquisition and release
- Non-blocking: skips execution if lock is unavailable

### 4. Cron Expressions (NotificationScheduler)
```
# Every 1 minute
0 */1 * * * *
# Disable a scheduler
shedlock.notification.cron=-
```

### 5. JdbcTemplateLockProvider Configuration
```java
JdbcTemplateLockProvider.Configuration.builder()
    .withJdbcTemplate(new JdbcTemplate(dataSource))
    .usingDbTime()        // use DB server clock — avoids cross-node clock skew
    .withTableName("shedlock")
    .build()
```

### 6. lockAtMostFor vs lockAtLeastFor
| Setting          | Purpose                                                        |
|------------------|----------------------------------------------------------------|
| `lockAtMostFor`  | Max lock hold time — prevents stuck locks if a node dies       |
| `lockAtLeastFor` | Min lock hold time — prevents race on the same cron tick       |
| `defaultLockAtMostFor` | @EnableSchedulerLock default applied when method doesn't specify |

### 7. Thread Pool for Schedulers
```java
@Bean
public ThreadPoolTaskScheduler taskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(5);
    return scheduler;
}
```
Spring's default scheduler is single-threaded — custom pool allows parallel task execution.

---

## Design Patterns

| Pattern         | Where Applied                                                              |
|-----------------|----------------------------------------------------------------------------|
| Template Method | `AbstractScheduler` — skeleton with `LockAssert` + timing, delegates to `performTask()` |
| Decorator       | `KeepAliveLockProvider` wraps `JdbcTemplateLockProvider`                   |
| Strategy        | `LockProvider` interface — swap JDBC / Redis / InMemory without changing callers |
| Factory Method  | `createLockConfiguration()` in `CustomLockScheduler`                      |

---

## ShedLock Table

Created automatically by Flyway (`V1__create_shedlock_table.sql`):

```sql
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,  -- scheduler name
    lock_until TIMESTAMP(3) NOT NULL,              -- when the lock expires
    locked_at  TIMESTAMP(3) NOT NULL,              -- when acquired
    locked_by  VARCHAR(255) NOT NULL               -- hostname:port of the holder
);
```

---

## Quick Start

### 1. Start infrastructure
```bash
docker-compose up -d
```

### 2. Run the application
```bash
./mvnw spring-boot:run
```

### 3. Open dashboards
| URL                         | Description               |
|-----------------------------|---------------------------|
| http://localhost:8080/actuator | Actuator endpoints      |
| http://localhost:8080/actuator/info | ShedLock table state |
| http://localhost:8080/api/v1/schedulers | Scheduler metadata |
| http://localhost:8080/api/v1/schedulers/locks | Live lock records |
| http://localhost:9091       | Prometheus                |
| http://localhost:3001       | Grafana (admin/admin)     |

---

## Running Tests

```bash
./mvnw test
```

Tests use TestContainers to spin up a real PostgreSQL container — no manual setup required.

---

## Key ShedLock Notes

> **IMPORTANT**: If ShedLock fails to start (e.g. DB unavailable), **none of the schedulers will start** and no logs will be written. Always ensure the database is healthy before starting the application.

- Locked by value format: `hostname:port` (auto-generated, unique per node)
- `lockAtMostFor` is your safety net for crashed nodes
- Use `usingDbTime()` in multi-AZ deployments where server clocks may drift
- `@LockProviderToUse` selects a specific `LockProvider` bean when multiple are defined
