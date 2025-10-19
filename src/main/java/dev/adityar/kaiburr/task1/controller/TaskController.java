package dev.adityar.kaiburr.task1.controller;

import dev.adityar.kaiburr.task1.dto.*;
import dev.adityar.kaiburr.task1.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Task management
 * Author: Aditya R.
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management and execution API")
public class TaskController {
    
    private final TaskService taskService;
    
    @Operation(summary = "Create or update a task", 
               description = "Creates a new task or updates an existing one by ID. Command is validated before saving.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task created/updated successfully",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input or command validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping
    public ResponseEntity<TaskResponse> upsertTask(
            @Valid @RequestBody TaskRequest request) {
        
        TaskResponse response = taskService.upsertTask(request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get all tasks", 
               description = "Retrieve all tasks with pagination support")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getAllTasks(
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<TaskResponse> tasks = taskService.getAllTasks(pageable);
        return ResponseEntity.ok(tasks);
    }
    
    @Operation(summary = "Get task by ID", 
               description = "Retrieve a specific task by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task found",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
        @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(
            @Parameter(description = "Task ID")
            @PathVariable String id) {
        
        TaskResponse response = taskService.getTaskById(id);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Delete task", 
               description = "Delete a task by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @Parameter(description = "Task ID")
            @PathVariable String id) {
        
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "Search tasks by name", 
               description = "Search for tasks whose name contains the given substring (case-insensitive)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tasks found"),
        @ApiResponse(responseCode = "404", description = "No tasks found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<List<TaskResponse>> searchTasks(
            @Parameter(description = "Name substring to search for")
            @RequestParam String name) {
        
        List<TaskResponse> tasks = taskService.searchTasksByName(name);
        return ResponseEntity.ok(tasks);
    }
    
    @Operation(summary = "Execute task command", 
               description = "Execute the command associated with a task and record the execution")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Command executed successfully",
                    content = @Content(schema = @Schema(implementation = TaskExecutionResponse.class))),
        @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Command execution failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}/executions")
    public ResponseEntity<TaskExecutionResponse> executeTask(
            @Parameter(description = "Task ID")
            @PathVariable String id) {
        
        TaskExecutionResponse response = taskService.executeTask(id);
        return ResponseEntity.ok(response);
    }
}
