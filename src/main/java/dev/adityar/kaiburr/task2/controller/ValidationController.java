package dev.adityar.kaiburr.task2.controller;

import dev.adityar.kaiburr.task2.dto.CommandValidationRequest;
import dev.adityar.kaiburr.task2.dto.CommandValidationResponse;
import dev.adityar.kaiburr.task2.service.CommandValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for command validation (dry-run).
 * 
 * Allows clients to test command safety before execution.
 * 
 * @author Aditya R
 */
@RestController
@RequestMapping("/api/validation")
@RequiredArgsConstructor
@Tag(name = "Validation", description = "Command validation API")
public class ValidationController {
    
    private final CommandValidator commandValidator;
    
    @Operation(summary = "Validate command without executing", description = "Dry-run validation to check if a command would be allowed")
    @PostMapping("/command")
    public ResponseEntity<CommandValidationResponse> validateCommand(
            @Valid @RequestBody CommandValidationRequest request) {
        
        CommandValidator.ValidationResult result = 
            commandValidator.validate(request.getCommand(), request.getArgs());
        
        CommandValidationResponse response = CommandValidationResponse.builder()
            .valid(result.isValid())
            .reasons(result.getReasons())
            .build();
        
        return ResponseEntity.ok(response);
    }
}
