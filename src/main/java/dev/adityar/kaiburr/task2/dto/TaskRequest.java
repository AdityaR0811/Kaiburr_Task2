package dev.adityar.kaiburr.task2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request DTO for creating or updating a task.
 * 
 * @author Aditya R
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {
    
    @NotBlank(message = "Task ID is required")
    @Size(max = 100, message = "Task ID must not exceed 100 characters")
    private String id;
    
    @NotBlank(message = "Task name is required")
    @Size(max = 200, message = "Task name must not exceed 200 characters")
    private String name;
    
    @NotBlank(message = "Command is required")
    @Size(max = 100, message = "Command must not exceed 100 characters")
    private String command;
    
    @Size(max = 8, message = "Maximum 8 arguments allowed")
    private List<String> args;
    
    @Size(max = 100, message = "Assignee must not exceed 100 characters")
    private String assignee;
}
