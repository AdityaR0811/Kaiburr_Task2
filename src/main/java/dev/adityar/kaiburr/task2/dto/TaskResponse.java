package dev.adityar.kaiburr.task2.dto;

import dev.adityar.kaiburr.task2.domain.TaskExecution;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for task operations.
 * 
 * @author Aditya R
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    
    private String id;
    private String name;
    private String command;
    private List<String> args;
    private String assignee;
    private List<TaskExecution> executions;
}
