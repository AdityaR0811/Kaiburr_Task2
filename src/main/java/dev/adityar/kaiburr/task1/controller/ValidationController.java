package dev.adityar.kaiburr.task1.controller;

import dev.adityar.kaiburr.task1.dto.CommandValidationRequest;
import dev.adityar.kaiburr.task1.dto.CommandValidationResponse;
import dev.adityar.kaiburr.task1.dto.ErrorResponse;
import dev.adityar.kaiburr.task1.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for command validation
 * Author: Aditya R.
 */
@Slf4j
@RestController
@RequestMapping("/api/validation")
@RequiredArgsConstructor
@Tag(name = "Validation", description = "Command validation API (dry-run)")
public class ValidationController {
    
    private final TaskService taskService;
    
    @Operation(summary = "Validate command", 
               description = "Dry-run validation of a command without executing it. Returns validation result and any violations.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validation completed",
                    content = @Content(schema = @Schema(implementation = CommandValidationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/command")
    public ResponseEntity<CommandValidationResponse> validateCommand(
            @Valid @RequestBody CommandValidationRequest request) {
        
        CommandValidationResponse response = taskService.validateCommand(request);
        return ResponseEntity.ok(response);
    }
}
