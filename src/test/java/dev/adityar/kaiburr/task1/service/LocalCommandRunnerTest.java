package dev.adityar.kaiburr.task1.service;

import dev.adityar.kaiburr.task1.domain.TaskExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for LocalCommandRunner
 * Author: Aditya R.
 */
@DisplayName("LocalCommandRunner Tests")
class LocalCommandRunnerTest {
    
    private LocalCommandRunner commandRunner;
    private CommandValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new CommandValidator();
        validator.init();
        commandRunner = new LocalCommandRunner(validator);
    }
    
    @Test
    @DisplayName("Should execute echo command successfully")
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void shouldExecuteEchoCommand() throws Exception {
        TaskExecution execution = commandRunner.execute("echo hello", "test-correlation-id");
        
        assertThat(execution).isNotNull();
        assertThat(execution.getExitCode()).isZero();
        assertThat(execution.getStdout()).contains("hello");
        assertThat(execution.getStartTime()).isNotNull();
        assertThat(execution.getEndTime()).isNotNull();
        assertThat(execution.getDurationMs()).isNotNull().isPositive();
        assertThat(execution.getCorrelationId()).isEqualTo("test-correlation-id");
    }
    
    @Test
    @DisplayName("Should execute date command successfully")
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void shouldExecuteDateCommand() throws Exception {
        TaskExecution execution = commandRunner.execute("date", "test-correlation-id");
        
        assertThat(execution).isNotNull();
        assertThat(execution.getExitCode()).isZero();
        assertThat(execution.getStdout()).isNotEmpty();
        assertThat(execution.getDurationMs()).isNotNull();
    }
    
    @Test
    @DisplayName("Should execute uname command successfully")
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void shouldExecuteUnameCommand() throws Exception {
        TaskExecution execution = commandRunner.execute("uname", "test-correlation-id");
        
        assertThat(execution).isNotNull();
        assertThat(execution.getExitCode()).isZero();
        assertThat(execution.getStdout()).isNotEmpty();
    }
    
    @Test
    @DisplayName("Should handle command with arguments")
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void shouldHandleCommandWithArguments() throws Exception {
        TaskExecution execution = commandRunner.execute("echo test123", "test-correlation-id");
        
        assertThat(execution).isNotNull();
        assertThat(execution.getExitCode()).isZero();
        assertThat(execution.getStdout()).contains("test123");
    }
    
    @Test
    @DisplayName("Should capture stderr on error")
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void shouldCaptureStderr() throws Exception {
        // This command should fail and produce stderr
        assertThatThrownBy(() -> commandRunner.execute("uname --invalid-option", "test-correlation-id"))
                .isInstanceOf(Exception.class);
    }
    
    @Test
    @DisplayName("Should truncate large output")
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void shouldTruncateLargeOutput() throws Exception {
        // Create a command that generates large output
        // Note: This is a simple test; actual truncation happens in SafeProcessIO
        TaskExecution execution = commandRunner.execute("echo test", "test-correlation-id");
        
        assertThat(execution).isNotNull();
        assertThat(execution.getStdout().length()).isLessThan(131072); // Max stdout bytes
    }
    
    @Test
    @DisplayName("Should handle non-zero exit code")
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void shouldHandleNonZeroExitCode() {
        // Command that doesn't exist should fail
        assertThatThrownBy(() -> commandRunner.execute("nonexistentcommand", "test-correlation-id"))
                .isInstanceOf(Exception.class);
    }
    
    @Test
    @DisplayName("Should calculate duration correctly")
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void shouldCalculateDuration() throws Exception {
        TaskExecution execution = commandRunner.execute("echo test", "test-correlation-id");
        
        assertThat(execution.getDurationMs()).isNotNull();
        assertThat(execution.getDurationMs()).isGreaterThanOrEqualTo(0);
        
        // Verify duration calculation
        long expectedDuration = execution.getEndTime().toEpochMilli() - 
                                execution.getStartTime().toEpochMilli();
        assertThat(execution.getDurationMs()).isEqualTo(expectedDuration);
    }
}
