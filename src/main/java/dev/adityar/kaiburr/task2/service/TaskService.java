package dev.adityar.kaiburr.task2.service;

import dev.adityar.kaiburr.task2.domain.Task;
import dev.adityar.kaiburr.task2.domain.TaskExecution;
import dev.adityar.kaiburr.task2.repo.TaskRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for Task operations including command execution.
 * 
 * @author Aditya R
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final CommandRunner commandRunner;
    private final CommandValidator commandValidator;
    private final MeterRegistry meterRegistry;
    
    /**
     * Create or update a task with validation.
     */
    public Task upsertTask(Task task) {
        // Validate command before saving
        CommandValidator.ValidationResult validation = 
            commandValidator.validate(task.getCommand(), task.getArgs());
        
        if (!validation.isValid()) {
            String reasons = String.join("; ", validation.getReasons());
            throw new IllegalArgumentException("Command validation failed: " + reasons);
        }
        
        log.info("Upserting task: id={}, command={}", task.getId(), task.getCommand());
        return taskRepository.save(task);
    }
    
    /**
     * Find all tasks.
     */
    public List<Task> findAll() {
        return taskRepository.findAll();
    }
    
    /**
     * Find task by ID.
     */
    public Optional<Task> findById(String id) {
        return taskRepository.findById(id);
    }
    
    /**
     * Search tasks by name substring.
     */
    public List<Task> searchByName(String nameSubstring) {
        return taskRepository.findByNameContaining(nameSubstring);
    }
    
    /**
     * Delete task by ID.
     */
    public boolean deleteById(String id) {
        if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id);
            log.info("Deleted task: {}", id);
            return true;
        }
        return false;
    }
    
    /**
     * Execute a task's command and append the execution result.
     */
    public TaskExecution executeTask(String taskId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException("Task not found: " + taskId));
        
        // Validate command again before execution (policy may have changed)
        CommandValidator.ValidationResult validation = 
            commandValidator.validate(task.getCommand(), task.getArgs());
        
        if (!validation.isValid()) {
            incrementCounter("validation_error");
            String reasons = String.join("; ", validation.getReasons());
            throw new IllegalArgumentException("Command validation failed: " + reasons);
        }
        
        // Execute with timing
        Timer.Sample sample = Timer.start(meterRegistry);
        TaskExecution execution = null;
        
        try {
            log.info("Executing task: id={}, command={}", taskId, task.getCommand());
            
            CommandRunner.ExecutionResult result = commandRunner.execute(task);
            
            // Build execution record
            execution = TaskExecution.builder()
                .id(UUID.randomUUID().toString())
                .jobName(result.getJobName())
                .status(result.isTimeout() ? TaskExecution.ExecutionStatus.TIMEOUT : 
                       (result.getExitCode() == 0 ? TaskExecution.ExecutionStatus.SUCCEEDED : 
                        TaskExecution.ExecutionStatus.FAILED))
                .exitCode(result.getExitCode())
                .stdout(result.getStdout())
                .stderr(result.getStderr())
                .durationMs(result.getDurationMs())
                .startedAt(Instant.now().minusMillis(result.getDurationMs()))
                .completedAt(Instant.now())
                .build();
            
            // Append to task
            task.getExecutions().add(execution);
            taskRepository.save(task);
            
            // Record metrics
            sample.stop(meterRegistry.timer("kaiburr.executor.duration"));
            
            if (result.isTimeout()) {
                incrementCounter("timeout");
            } else if (result.getExitCode() == 0) {
                incrementCounter("success");
            } else {
                incrementCounter("runtime_error");
            }
            
            log.info("Execution completed: taskId={}, execId={}, exitCode={}, duration={}ms",
                taskId, execution.getId(), execution.getExitCode(), execution.getDurationMs());
            
            return execution;
            
        } catch (CommandRunner.CommandExecutionException e) {
            incrementCounter("runtime_error");
            sample.stop(meterRegistry.timer("kaiburr.executor.duration"));
            throw e;
        }
    }
    
    /**
     * Increment execution counter by result type.
     */
    private void incrementCounter(String result) {
        Counter.builder("kaiburr.executions.total")
            .tag("result", result)
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Exception for task not found.
     */
    public static class TaskNotFoundException extends RuntimeException {
        public TaskNotFoundException(String message) {
            super(message);
        }
    }
}
