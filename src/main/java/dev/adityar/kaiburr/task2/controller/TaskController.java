package dev.adityar.kaiburr.task2.controller;

import dev.adityar.kaiburr.task2.domain.Task;
import dev.adityar.kaiburr.task2.domain.TaskExecution;
import dev.adityar.kaiburr.task2.dto.*;
import dev.adityar.kaiburr.task2.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for Task operations.
 * 
 * Maintains identical API contract as Task 1.
 * 
 * @author Aditya R
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management and execution API")
public class TaskController {
    
    private final TaskService taskService;
    
    @Operation(summary = "Create or update a task")
    @ApiResponse(responseCode = "200", description = "Task created/updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or validation failure")
    @PutMapping
    public ResponseEntity<TaskResponse> upsertTask(@Valid @RequestBody TaskRequest request) {
        Task task = Task.builder()
            .id(request.getId())
            .name(request.getName())
            .command(request.getCommand())
            .args(request.getArgs())
            .assignee(request.getAssignee())
            .build();
        
        Task saved = taskService.upsertTask(task);
        return ResponseEntity.ok(toResponse(saved));
    }
    
    @Operation(summary = "Get all tasks")
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        List<TaskResponse> tasks = taskService.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(tasks);
    }
    
    @Operation(summary = "Get task by ID")
    @ApiResponse(responseCode = "200", description = "Task found")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable String id) {
        return taskService.findById(id)
            .map(this::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @Operation(summary = "Search tasks by name")
    @ApiResponse(responseCode = "200", description = "Tasks found")
    @ApiResponse(responseCode = "404", description = "No tasks found")
    @GetMapping("/search")
    public ResponseEntity<List<TaskResponse>> searchTasks(@RequestParam String name) {
        List<TaskResponse> tasks = taskService.searchByName(name).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        
        if (tasks.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(tasks);
    }
    
    @Operation(summary = "Delete task by ID")
    @ApiResponse(responseCode = "204", description = "Task deleted")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        boolean deleted = taskService.deleteById(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
    
    @Operation(summary = "Execute task command")
    @ApiResponse(responseCode = "200", description = "Execution completed")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @PutMapping("/{id}/executions")
    public ResponseEntity<TaskExecutionResponse> executeTask(@PathVariable String id) {
        TaskExecution execution = taskService.executeTask(id);
        
        TaskExecutionResponse response = TaskExecutionResponse.builder()
            .taskId(id)
            .executionId(execution.getId())
            .jobName(execution.getJobName())
            .status(execution.getStatus().name())
            .exitCode(execution.getExitCode())
            .stdout(execution.getStdout())
            .stderr(execution.getStderr())
            .durationMs(execution.getDurationMs())
            .startedAt(execution.getStartedAt())
            .completedAt(execution.getCompletedAt())
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Convert Task entity to response DTO.
     */
    private TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
            .id(task.getId())
            .name(task.getName())
            .command(task.getCommand())
            .args(task.getArgs())
            .assignee(task.getAssignee())
            .executions(task.getExecutions())
            .build();
    }
}
