package dev.adityar.kaiburr.task2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request DTO for command validation (dry-run).
 * 
 * @author Aditya R
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandValidationRequest {
    
    @NotBlank(message = "Command is required")
    private String command;
    
    @Size(max = 8, message = "Maximum 8 arguments allowed")
    private List<String> args;
}
