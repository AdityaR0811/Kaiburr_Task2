package dev.adityar.kaiburr.task2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for command validation.
 * 
 * @author Aditya R
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandValidationResponse {
    
    private boolean valid;
    private List<String> reasons;
}
