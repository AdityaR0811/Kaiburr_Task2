package dev.adityar.kaiburr.task2.service;

import dev.adityar.kaiburr.task2.domain.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CommandRunner interface for abstracting command execution backends.
 * 
 * Implementations:
 * - LocalCommandRunner: fork/exec for local development
 * - KubernetesCommandRunner: Kubernetes Jobs for production
 * 
 * @author Aditya R
 */
public interface CommandRunner {
    
    /**
     * Execute a task's command and return the execution result.
     * 
     * @param task The task to execute
     * @return ExecutionResult with exit code, stdout, stderr, and timing
     * @throws CommandExecutionException if execution fails
     */
    ExecutionResult execute(Task task);
    
    /**
     * Result of command execution.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ExecutionResult {
        private String jobName;
        private int exitCode;
        private String stdout;
        private String stderr;
        private long durationMs;
        private boolean timeout;
    }
    
    /**
     * Exception thrown when command execution fails.
     */
    class CommandExecutionException extends RuntimeException {
        public CommandExecutionException(String message) {
            super(message);
        }
        
        public CommandExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
