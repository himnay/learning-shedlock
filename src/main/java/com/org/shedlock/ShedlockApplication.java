package com.org.shedlock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
class ShedlockApplication {

    /** Application entry point. */
    public static void main(String[] args) {
        SpringApplication.run(ShedlockApplication.class, args);
    }
}
