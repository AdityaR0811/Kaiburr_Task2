package dev.adityar.kaiburr.task1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for command validation
 * Author: Aditya R.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandValidationResponse {
    
    private Boolean valid;
    private String command;
    private List<String> violations;
    private String message;
}
