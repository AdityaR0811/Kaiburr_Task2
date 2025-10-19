package dev.adityar.kaiburr.task2.service;

import dev.adityar.kaiburr.task2.domain.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Local command runner using fork/exec for development.
 * 
 * Activated with profile=local.
 * 
 * @author Aditya R
 */
@Slf4j
@Service
@Profile("local")
public class LocalCommandRunner implements CommandRunner {
    
    private static final int TIMEOUT_SECONDS = 15;
    private static final int MAX_OUTPUT_BYTES = 131072; // 128 KiB
    
    @Override
    public ExecutionResult execute(Task task) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("Executing command locally: {} {}", task.getCommand(), task.getArgs());
            
            // Build command with full path
            List<String> commandList = new ArrayList<>();
            commandList.add("/usr/bin/" + task.getCommand());
            if (task.getArgs() != null) {
                commandList.addAll(task.getArgs());
            }
            
            ProcessBuilder pb = new ProcessBuilder(commandList);
            pb.redirectErrorStream(false);
            
            Process process = pb.start();
            
            // Read stdout and stderr
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            
            Thread stdoutReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null && stdout.length() < MAX_OUTPUT_BYTES) {
                        stdout.append(line).append("\n");
                    }
                } catch (Exception e) {
                    log.error("Error reading stdout", e);
                }
            });
            
            Thread stderrReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null && stderr.length() < MAX_OUTPUT_BYTES) {
                        stderr.append(line).append("\n");
                    }
                } catch (Exception e) {
                    log.error("Error reading stderr", e);
                }
            });
            
            stdoutReader.start();
            stderrReader.start();
            
            // Wait for completion with timeout
            boolean completed = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                stdoutReader.interrupt();
                stderrReader.interrupt();
                
                long duration = System.currentTimeMillis() - startTime;
                log.warn("Command timeout after {}ms", duration);
                
                return ExecutionResult.builder()
                    .jobName("local-" + System.currentTimeMillis())
                    .exitCode(-1)
                    .stdout(stdout.toString())
                    .stderr("Command timeout after " + TIMEOUT_SECONDS + " seconds")
                    .durationMs(duration)
                    .timeout(true)
                    .build();
            }
            
            stdoutReader.join();
            stderrReader.join();
            
            int exitCode = process.exitValue();
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("Command completed: exitCode={}, duration={}ms", exitCode, duration);
            
            return ExecutionResult.builder()
                .jobName("local-" + System.currentTimeMillis())
                .exitCode(exitCode)
                .stdout(stdout.toString())
                .stderr(stderr.toString())
                .durationMs(duration)
                .timeout(false)
                .build();
                
        } catch (Exception e) {
            log.error("Command execution failed", e);
            throw new CommandExecutionException("Local execution failed: " + e.getMessage(), e);
        }
    }
}
