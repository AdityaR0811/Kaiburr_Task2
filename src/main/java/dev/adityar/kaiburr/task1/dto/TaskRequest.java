package dev.adityar.kaiburr.task1.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Request DTO for creating/updating a Task
 * Author: Aditya R.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {
    
    @NotBlank(message = "Task ID is required")
    @Size(min = 1, max = 100, message = "Task ID must be between 1 and 100 characters")
    private String id;
    
    @NotBlank(message = "Task name is required")
    @Size(min = 1, max = 200, message = "Task name must be between 1 and 200 characters")
    private String name;
    
    @NotBlank(message = "Owner is required")
    @Size(min = 1, max = 100, message = "Owner must be between 1 and 100 characters")
    private String owner;
    
    @NotBlank(message = "Command is required")
    @Size(min = 1, max = 200, message = "Command must be between 1 and 200 characters")
    private String command;
}
