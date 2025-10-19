package dev.adityar.kaiburr.task2.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CommandValidator.
 * 
 * Tests denylist, allowlist, metacharacters, sequences, and limits.
 * 
 * @author Aditya R
 */
class CommandValidatorTest {
    
    private CommandValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new CommandValidator();
        validator.init();
    }
    
    @Test
    @DisplayName("Should allow valid command with safe args")
    void testValidCommand() {
        CommandValidator.ValidationResult result = validator.validate("echo", List.of("hello", "world"));
        
        assertThat(result.isValid()).isTrue();
        assertThat(result.getReasons()).isEmpty();
    }
    
    @Test
    @DisplayName("Should reject command in denylist")
    void testDenylistedCommand() {
        CommandValidator.ValidationResult result = validator.validate("rm", List.of("-rf", "/"));
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasons()).anyMatch(r -> r.contains("denied by policy"));
    }
    
    @Test
    @DisplayName("Should reject command not in allowlist")
    void testNotAllowlistedCommand() {
        CommandValidator.ValidationResult result = validator.validate("curl", List.of("http://attacker.com"));
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasons()).anyMatch(r -> r.contains("not in allowlist"));
    }
    
    @Test
    @DisplayName("Should reject semicolon metacharacter")
    void testSemicolonMetacharacter() {
        CommandValidator.ValidationResult result = validator.validate("echo", List.of("test;curl"));
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasons()).anyMatch(r -> r.contains("metacharacter"));
    }
    
    @Test
    @DisplayName("Should reject pipe metacharacter")
    void testPipeMetacharacter() {
        CommandValidator.ValidationResult result = validator.validate("echo", List.of("test|nc"));
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasons()).anyMatch(r -> r.contains("metacharacter"));
    }
    
    @Test
    @DisplayName("Should reject path traversal sequence")
    void testPathTraversal() {
        CommandValidator.ValidationResult result = validator.validate("echo", List.of("../etc/passwd"));
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasons()).anyMatch(r -> r.contains("sequence"));
    }
    
    @Test
    @DisplayName("Should reject double ampersand sequence")
    void testDoubleAmpersand() {
        CommandValidator.ValidationResult result = validator.validate("echo", List.of("test&&curl"));
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasons()).anyMatch(r -> r.contains("sequence"));
    }
    
    @Test
    @DisplayName("Should reject too many arguments")
    void testTooManyArgs() {
        List<String> args = List.of("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9");
        CommandValidator.ValidationResult result = validator.validate("echo", args);
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasons()).anyMatch(r -> r.contains("Too many arguments"));
    }
    
    @Test
    @DisplayName("Should reject argument not matching pattern")
    void testInvalidArgumentPattern() {
        CommandValidator.ValidationResult result = validator.validate("echo", List.of("test@invalid!"));
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasons()).anyMatch(r -> r.contains("does not match allowed pattern"));
    }
    
    @Test
    @DisplayName("Should allow argument with allowed special chars")
    void testValidArgumentWithSpecialChars() {
        CommandValidator.ValidationResult result = validator.validate("echo", List.of("test-value_123.txt"));
        
        assertThat(result.isValid()).isTrue();
    }
    
    @Test
    @DisplayName("Should reject command exceeding total length")
    void testTotalLengthExceeded() {
        String longArg = "a".repeat(200);
        CommandValidator.ValidationResult result = validator.validate("echo", List.of(longArg));
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasons()).anyMatch(r -> r.contains("Total command length"));
    }
    
    @Test
    @DisplayName("Should reject empty command")
    void testEmptyCommand() {
        CommandValidator.ValidationResult result = validator.validate("", List.of());
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasons()).anyMatch(r -> r.contains("cannot be empty"));
    }
    
    @Test
    @DisplayName("Should reject null command")
    void testNullCommand() {
        CommandValidator.ValidationResult result = validator.validate(null, List.of());
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasons()).anyMatch(r -> r.contains("cannot be empty"));
    }
    
    @Test
    @DisplayName("Should allow all allowlisted binaries")
    void testAllAllowlistedBinaries() {
        List<String> allowlisted = List.of("echo", "date", "uname", "whoami", "id", "uptime", "printenv", "env", "pwd", "hostname");
        
        for (String binary : allowlisted) {
            CommandValidator.ValidationResult result = validator.validate(binary, List.of());
            assertThat(result.isValid())
                .as("Binary %s should be allowlisted", binary)
                .isTrue();
        }
    }
}
