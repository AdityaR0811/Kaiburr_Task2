package dev.adityar.kaiburr.task1.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for command validation
 * Author: Aditya R.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandValidationRequest {
    
    @NotBlank(message = "Command is required")
    private String command;
}
