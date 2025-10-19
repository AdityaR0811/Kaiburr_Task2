package dev.adityar.kaiburr.task1.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CommandValidator
 * Author: Aditya R.
 */
@DisplayName("CommandValidator Tests")
class CommandValidatorTest {
    
    private CommandValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new CommandValidator();
        validator.init();
    }
    
    @Test
    @DisplayName("Should accept valid echo command")
    void shouldAcceptValidEchoCommand() {
        CommandValidator.ValidationResult result = validator.validate("echo hello");
        
        assertThat(result.isValid()).isTrue();
        assertThat(result.getViolations()).isEmpty();
    }
    
    @Test
    @DisplayName("Should accept valid date command")
    void shouldAcceptValidDateCommand() {
        CommandValidator.ValidationResult result = validator.validate("date");
        
        assertThat(result.isValid()).isTrue();
        assertThat(result.getViolations()).isEmpty();
    }
    
    @Test
    @DisplayName("Should accept valid uname command")
    void shouldAcceptValidUnameCommand() {
        CommandValidator.ValidationResult result = validator.validate("uname -a");
        
        assertThat(result.isValid()).isTrue();
        assertThat(result.getViolations()).isEmpty();
    }
    
    @Test
    @DisplayName("Should reject command with rm token")
    void shouldRejectCommandWithRmToken() {
        CommandValidator.ValidationResult result = validator.validate("rm file.txt");
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> v.contains("rm"));
    }
    
    @Test
    @DisplayName("Should reject command with sudo token")
    void shouldRejectCommandWithSudoToken() {
        CommandValidator.ValidationResult result = validator.validate("sudo echo hello");
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> v.contains("sudo"));
    }
    
    @Test
    @DisplayName("Should reject command with semicolon metacharacter")
    void shouldRejectCommandWithSemicolon() {
        CommandValidator.ValidationResult result = validator.validate("echo hello; echo world");
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> v.contains(";"));
    }
    
    @Test
    @DisplayName("Should reject command with pipe metacharacter")
    void shouldRejectCommandWithPipe() {
        CommandValidator.ValidationResult result = validator.validate("echo hello | grep h");
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> v.contains("|"));
    }
    
    @Test
    @DisplayName("Should reject command with ampersand metacharacter")
    void shouldRejectCommandWithAmpersand() {
        CommandValidator.ValidationResult result = validator.validate("echo hello & echo world");
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> v.contains("&"));
    }
    
    @Test
    @DisplayName("Should reject command with dollar sign")
    void shouldRejectCommandWithDollarSign() {
        CommandValidator.ValidationResult result = validator.validate("echo $PATH");
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> v.contains("$"));
    }
    
    @Test
    @DisplayName("Should reject command with backticks")
    void shouldRejectCommandWithBackticks() {
        CommandValidator.ValidationResult result = validator.validate("echo `date`");
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> v.contains("`"));
    }
    
    @Test
    @DisplayName("Should reject command with double ampersand sequence")
    void shouldRejectCommandWithDoubleAmpersand() {
        CommandValidator.ValidationResult result = validator.validate("echo hello && echo world");
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> v.contains("&&"));
    }
    
    @Test
    @DisplayName("Should reject command with directory traversal")
    void shouldRejectCommandWithDirectoryTraversal() {
        CommandValidator.ValidationResult result = validator.validate("echo ../file");
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> v.contains("../"));
    }
    
    @Test
    @DisplayName("Should reject command with quotes")
    void shouldRejectCommandWithQuotes() {
        CommandValidator.ValidationResult result = validator.validate("echo \"hello world\"");
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> v.contains("quote"));
    }
    
    @Test
    @DisplayName("Should reject command with newlines")
    void shouldRejectCommandWithNewlines() {
        CommandValidator.ValidationResult result = validator.validate("echo hello\necho world");
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> v.contains("newline"));
    }
    
    @Test
    @DisplayName("Should reject command exceeding length limit")
    void shouldRejectCommandExceedingLength() {
        String longCommand = "echo " + "a".repeat(200);
        CommandValidator.ValidationResult result = validator.validate(longCommand);
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> v.contains("maximum length"));
    }
    
    @Test
    @DisplayName("Should reject command with too many arguments")
    void shouldRejectCommandWithTooManyArgs() {
        CommandValidator.ValidationResult result = validator.validate("echo a b c d e f g h i j");
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> v.contains("too many arguments"));
    }
    
    @Test
    @DisplayName("Should reject command not in allowlist")
    void shouldRejectCommandNotInAllowlist() {
        CommandValidator.ValidationResult result = validator.validate("cat file.txt");
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> v.contains("not in allowlist"));
    }
    
    @Test
    @DisplayName("Should reject empty command")
    void shouldRejectEmptyCommand() {
        CommandValidator.ValidationResult result = validator.validate("");
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> v.contains("empty"));
    }
    
    @Test
    @DisplayName("Should reject null command")
    void shouldRejectNullCommand() {
        CommandValidator.ValidationResult result = validator.validate(null);
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> v.contains("empty"));
    }
    
    @Test
    @DisplayName("Should accept command with valid special characters in args")
    void shouldAcceptCommandWithValidSpecialChars() {
        CommandValidator.ValidationResult result = validator.validate("echo test-value_123");
        
        assertThat(result.isValid()).isTrue();
    }
    
    @Test
    @DisplayName("Should reject command with invalid characters in args")
    void shouldRejectCommandWithInvalidChars() {
        CommandValidator.ValidationResult result = validator.validate("echo test@value");
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> v.contains("invalid characters"));
    }
}
