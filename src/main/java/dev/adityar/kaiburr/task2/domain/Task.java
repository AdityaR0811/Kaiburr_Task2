package dev.adityar.kaiburr.task2.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Task entity representing a command execution template.
 * 
 * Each task defines a command with arguments that can be executed
 * multiple times, creating TaskExecution records.
 * 
 * @author Aditya R
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tasks")
public class Task {
    
    @Id
    private String id;
    
    private String name;
    
    private String command;
    
    @Builder.Default
    private List<String> args = new ArrayList<>();
    
    private String assignee;
    
    @Builder.Default
    private List<TaskExecution> executions = new ArrayList<>();
}
