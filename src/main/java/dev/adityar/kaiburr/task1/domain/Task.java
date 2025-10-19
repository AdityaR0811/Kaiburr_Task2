package dev.adityar.kaiburr.task1.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Task domain entity
 * Author: Aditya R.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tasks")
public class Task {
    
    @Id
    private String id;
    
    @Indexed
    private String name;
    
    private String owner;
    
    private String command;
    
    @Builder.Default
    private List<TaskExecution> taskExecutions = new ArrayList<>();
    
    private Instant createdAt;
    
    private Instant updatedAt;
    
    @Version
    private Long version;
    
    public void addExecution(TaskExecution execution) {
        if (this.taskExecutions == null) {
            this.taskExecutions = new ArrayList<>();
        }
        this.taskExecutions.add(execution);
    }
}
