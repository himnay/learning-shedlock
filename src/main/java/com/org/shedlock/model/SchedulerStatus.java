package com.org.shedlock.model;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SchedulerStatus(
        String name,
        String type,
        String schedule,
        String lockProvider,
        String lockAtMostFor,
        String lockAtLeastFor,
        String description,
        LocalDateTime retrievedAt
) {}
