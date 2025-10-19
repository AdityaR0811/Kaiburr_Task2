package dev.adityar.kaiburr.task1.service;

import dev.adityar.kaiburr.task1.domain.TaskExecution;

/**
 * Interface for command execution
 * Author: Aditya R.
 */
public interface CommandRunner {
    
    /**
     * Execute a command and return the execution details
     * 
     * @param command The command to execute
     * @param correlationId The correlation ID for tracking
     * @return TaskExecution with execution details
     * @throws Exception if execution fails
     */
    TaskExecution execute(String command, String correlationId) throws Exception;
}
