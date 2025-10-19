package dev.adityar.kaiburr.task1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.adityar.kaiburr.task1.domain.Task;
import dev.adityar.kaiburr.task1.domain.TaskExecution;
import dev.adityar.kaiburr.task1.dto.*;
import dev.adityar.kaiburr.task1.repo.TaskRepository;
import dev.adityar.kaiburr.task1.util.CorrelationId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing tasks and executions
 * Author: Aditya R.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final CommandValidator validator;
    private final CommandRunner commandRunner;
    private final ObjectMapper objectMapper;
    
    @Value("${app.audit.log-file:audit.log.jsonl}")
    private String auditLogFile;
    
    /**
     * Create or update a task
     */
    public TaskResponse upsertTask(TaskRequest request) {
        log.info("Upserting task: id={}, name={}", request.getId(), request.getName());
        
        // Validate command
        CommandValidator.ValidationResult validationResult = validator.validate(request.getCommand());
        if (!validationResult.isValid()) {
            String violations = String.join("; ", validationResult.getViolations());
            log.warn("Command validation failed for task {}: {}", request.getId(), violations);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Command validation failed: " + violations
            );
        }
        
        // Check if task exists
        Task task = taskRepository.findById(request.getId())
                .orElse(null);
        
        Instant now = Instant.now();
        
        if (task == null) {
            // Create new task
            task = Task.builder()
                    .id(request.getId())
                    .name(request.getName())
                    .owner(request.getOwner())
                    .command(request.getCommand())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            log.info("Creating new task: {}", request.getId());
        } else {
            // Update existing task
            task.setName(request.getName());
            task.setOwner(request.getOwner());
            task.setCommand(request.getCommand());
            task.setUpdatedAt(now);
            log.info("Updating existing task: {}", request.getId());
        }
        
        task = taskRepository.save(task);
        
        return toTaskResponse(task);
    }
    
    /**
     * Get all tasks with pagination
     */
    public Page<TaskResponse> getAllTasks(Pageable pageable) {
        log.info("Fetching all tasks: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return taskRepository.findAll(pageable)
                .map(this::toTaskResponse);
    }
    
    /**
     * Get task by ID
     */
    public TaskResponse getTaskById(String id) {
        log.info("Fetching task by id: {}", id);
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Task not found with id: " + id
                ));
        return toTaskResponse(task);
    }
    
    /**
     * Delete task by ID
     */
    public void deleteTask(String id) {
        log.info("Deleting task: {}", id);
        if (!taskRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Task not found with id: " + id
            );
        }
        taskRepository.deleteById(id);
        log.info("Task deleted: {}", id);
    }
    
    /**
     * Search tasks by name
     */
    public List<TaskResponse> searchTasksByName(String name) {
        log.info("Searching tasks by name: {}", name);
        List<Task> tasks = taskRepository.findByNameContainingIgnoreCase(name);
        
        if (tasks.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No tasks found with name containing: " + name
            );
        }
        
        return tasks.stream()
                .map(this::toTaskResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Execute task command and record execution
     */
    public TaskExecutionResponse executeTask(String id) {
        log.info("Executing task: {}", id);
        
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Task not found with id: " + id
                ));
        
        String correlationId = CorrelationId.get();
        
        try {
            // Execute command
            TaskExecution execution = commandRunner.execute(task.getCommand(), correlationId);
            
            // Add execution to task
            task.addExecution(execution);
            task.setUpdatedAt(Instant.now());
            taskRepository.save(task);
            
            // Write audit log
            writeAuditLog(task, execution);
            
            log.info("Task execution completed: taskId={}, exitCode={}, correlationId={}", 
                    id, execution.getExitCode(), correlationId);
            
            return toTaskExecutionResponse(execution);
            
        } catch (Exception e) {
            log.error("Task execution failed: taskId={}, correlationId={}", id, correlationId, e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Task execution failed: " + e.getMessage()
            );
        }
    }
    
    /**
     * Validate command without executing
     */
    public CommandValidationResponse validateCommand(CommandValidationRequest request) {
        log.info("Validating command: {}", request.getCommand());
        
        CommandValidator.ValidationResult result = validator.validate(request.getCommand());
        
        return CommandValidationResponse.builder()
                .valid(result.isValid())
                .command(request.getCommand())
                .violations(result.getViolations())
                .message(result.isValid() ? "Command is valid" : "Command validation failed")
                .build();
    }
    
    /**
     * Write audit log entry
     */
    private void writeAuditLog(Task task, TaskExecution execution) {
        try {
            Map<String, Object> auditEntry = new HashMap<>();
            auditEntry.put("timestamp", Instant.now().toString());
            auditEntry.put("correlationId", execution.getCorrelationId());
            auditEntry.put("taskId", task.getId());
            auditEntry.put("owner", task.getOwner());
            auditEntry.put("commandHash", hashCommand(task.getCommand()));
            auditEntry.put("exitCode", execution.getExitCode());
            auditEntry.put("durationMs", execution.getDurationMs());
            auditEntry.put("startTime", execution.getStartTime().toString());
            auditEntry.put("endTime", execution.getEndTime().toString());
            
            String jsonLine = objectMapper.writeValueAsString(auditEntry);
            
            Files.writeString(
                    Paths.get(auditLogFile),
                    jsonLine + "\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
            
        } catch (IOException e) {
            log.error("Failed to write audit log", e);
        }
    }
    
    /**
     * Hash command for audit trail
     */
    private String hashCommand(String command) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(command.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 16);
        } catch (Exception e) {
            return "hash-error";
        }
    }
    
    /**
     * Convert Task entity to TaskResponse DTO
     */
    private TaskResponse toTaskResponse(Task task) {
        List<TaskExecutionResponse> executionResponses = null;
        if (task.getTaskExecutions() != null) {
            executionResponses = task.getTaskExecutions().stream()
                    .map(this::toTaskExecutionResponse)
                    .collect(Collectors.toList());
        }
        
        return TaskResponse.builder()
                .id(task.getId())
                .name(task.getName())
                .owner(task.getOwner())
                .command(task.getCommand())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .taskExecutions(executionResponses)
                .version(task.getVersion())
                .build();
    }
    
    /**
     * Convert TaskExecution to TaskExecutionResponse DTO
     */
    private TaskExecutionResponse toTaskExecutionResponse(TaskExecution execution) {
        return TaskExecutionResponse.builder()
                .startTime(execution.getStartTime())
                .endTime(execution.getEndTime())
                .durationMs(execution.getDurationMs())
                .exitCode(execution.getExitCode())
                .stdout(execution.getStdout())
                .stderr(execution.getStderr())
                .correlationId(execution.getCorrelationId())
                .build();
    }
}
