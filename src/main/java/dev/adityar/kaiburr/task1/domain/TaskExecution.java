package dev.adityar.kaiburr.task1.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * TaskExecution embedded document
 * Author: Aditya R.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecution {
    
    private Instant startTime;
    
    private Instant endTime;
    
    private Long durationMs;
    
    private Integer exitCode;
    
    private String stdout;
    
    private String stderr;
    
    private String correlationId;
    
    /**
     * Calculate duration from start and end times
     */
    public void calculateDuration() {
        if (startTime != null && endTime != null) {
            this.durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();
        }
    }
}
