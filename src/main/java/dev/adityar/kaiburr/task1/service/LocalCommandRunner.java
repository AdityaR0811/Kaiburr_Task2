package dev.adityar.kaiburr.task1.service;

import dev.adityar.kaiburr.task1.domain.TaskExecution;
import dev.adityar.kaiburr.task1.util.SafeProcessIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;

/**
 * Local command runner implementation
 * Executes commands directly on the host system without using shell
 * Author: Aditya R.
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class LocalCommandRunner implements CommandRunner {
    
    private final CommandValidator validator;
    
    @Override
    public TaskExecution execute(String command, String correlationId) throws Exception {
        log.info("Executing command locally: {} (correlationId={})", command, correlationId);
        
        Instant startTime = Instant.now();
        
        try {
            // Tokenize command safely - no shell involved
            String[] parts = command.trim().split("\\s+");
            
            log.debug("Command parts: {}", Arrays.toString(parts));
            
            // Build process without shell
            ProcessBuilder pb = new ProcessBuilder(parts);
            pb.redirectErrorStream(false);
            
            // Start process
            Process process = pb.start();
            
            // Execute with timeout and capture output
            CommandValidator.SecurityPolicy policy = validator.getPolicy();
            SafeProcessIO.ProcessResult result = SafeProcessIO.executeWithTimeout(
                    process,
                    policy.getLimits().getTimeoutSeconds(),
                    policy.getLimits().getMaxStdoutBytes(),
                    policy.getLimits().getMaxStderrBytes()
            );
            
            Instant endTime = Instant.now();
            
            TaskExecution execution = TaskExecution.builder()
                    .startTime(startTime)
                    .endTime(endTime)
                    .exitCode(result.getExitCode())
                    .stdout(result.getStdout())
                    .stderr(result.getStderr())
                    .correlationId(correlationId)
                    .build();
            
            execution.calculateDuration();
            
            log.info("Command executed: exitCode={}, duration={}ms, correlationId={}", 
                    execution.getExitCode(), 
                    execution.getDurationMs(), 
                    correlationId);
            
            return execution;
            
        } catch (SafeProcessIO.TimeoutException e) {
            log.error("Command execution timed out: {}", command, e);
            
            Instant endTime = Instant.now();
            TaskExecution execution = TaskExecution.builder()
                    .startTime(startTime)
                    .endTime(endTime)
                    .exitCode(-1)
                    .stdout("")
                    .stderr("Execution timed out after " + 
                            validator.getPolicy().getLimits().getTimeoutSeconds() + " seconds")
                    .correlationId(correlationId)
                    .build();
            
            execution.calculateDuration();
            
            throw new RuntimeException("Command execution timed out", e);
            
        } catch (Exception e) {
            log.error("Command execution failed: {}", command, e);
            
            Instant endTime = Instant.now();
            TaskExecution execution = TaskExecution.builder()
                    .startTime(startTime)
                    .endTime(endTime)
                    .exitCode(-1)
                    .stdout("")
                    .stderr("Execution failed: " + e.getMessage())
                    .correlationId(correlationId)
                    .build();
            
            execution.calculateDuration();
            
            throw e;
        }
    }
}
