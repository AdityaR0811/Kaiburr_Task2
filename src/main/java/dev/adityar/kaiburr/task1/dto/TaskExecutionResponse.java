package dev.adityar.kaiburr.task1.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for TaskExecution
 * Author: Aditya R.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionResponse {
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant startTime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant endTime;
    
    private Long durationMs;
    private Integer exitCode;
    private String stdout;
    private String stderr;
    private String correlationId;
}
