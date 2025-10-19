package dev.adityar.kaiburr.task2.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * TaskExecution represents a single execution instance of a Task's command.
 * 
 * In Kubernetes mode, each execution creates a Job and this record
 * captures the results including stdout, stderr, exit code, and timing.
 * 
 * @author Aditya R
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecution {
    
    private String id;
    
    private String jobName;
    
    private ExecutionStatus status;
    
    private Integer exitCode;
    
    private String stdout;
    
    private String stderr;
    
    private Long durationMs;
    
    private Instant startedAt;
    
    private Instant completedAt;
    
    /**
     * Execution status enum for async execution support.
     */
    public enum ExecutionStatus {
        PENDING,    // Job created, not yet scheduled
        RUNNING,    // Pod is executing
        SUCCEEDED,  // Exit code 0
        FAILED,     // Exit code != 0
        TIMEOUT     // activeDeadlineSeconds exceeded
    }
}
