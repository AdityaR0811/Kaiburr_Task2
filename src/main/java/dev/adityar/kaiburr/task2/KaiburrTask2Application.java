package dev.adityar.kaiburr.task2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Kaiburr Task 2 — Kubernetes Job Execution
 * 
 * Main application entry point for Task 2, which extends Task 1's REST API
 * with Kubernetes-native command execution.
 * 
 * Key Features:
 * - Dual-mode execution: local (dev) or Kubernetes Jobs (prod)
 * - Policy-as-data validation with hot-reload
 * - Distroless executor containers with security hardening
 * - Full observability with metrics and structured logging
 * 
 * @author Aditya R
 * @version 1.0.0
 */
@SpringBootApplication
@EnableMongoRepositories
@EnableAsync
@EnableScheduling
public class KaiburrTask2Application {

    public static void main(String[] args) {
        SpringApplication.run(KaiburrTask2Application.class, args);
    }
}
