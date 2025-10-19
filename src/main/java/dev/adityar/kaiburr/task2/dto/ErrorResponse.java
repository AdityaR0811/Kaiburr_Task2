package dev.adityar.kaiburr.task2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Error response DTO for API errors.
 * 
 * @author Aditya R
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String correlationId;
}
