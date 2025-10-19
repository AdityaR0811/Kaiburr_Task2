package dev.adityar.kaiburr.task2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for task execution results.
 * 
 * @author Aditya R
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionResponse {
    
    private String taskId;
    private String executionId;
    private String jobName;
    private String status;
    private Integer exitCode;
    private String stdout;
    private String stderr;
    private Long durationMs;
    private Instant startedAt;
    private Instant completedAt;
}
