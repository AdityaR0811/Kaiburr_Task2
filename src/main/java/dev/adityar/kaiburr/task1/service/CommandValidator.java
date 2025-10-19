package dev.adityar.kaiburr.task1.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Command validator with policy-as-data configuration
 * Author: Aditya R.
 */
@Slf4j
@Component
public class CommandValidator {
    
    @Value("${app.security.policy-file:config/command-policy.yaml}")
    private String policyFile;
    
    private SecurityPolicy policy;
    private Pattern argCharPattern;
    
    @PostConstruct
    public void init() {
        loadPolicy();
        log.info("CommandValidator initialized with policy from: {}", policyFile);
    }
    
    private void loadPolicy() {
        try {
            Path path = Paths.get(policyFile);
            if (!Files.exists(path)) {
                log.warn("Policy file not found: {}, using defaults", policyFile);
                policy = createDefaultPolicy();
                return;
            }
            
            Yaml yaml = new Yaml();
            try (FileInputStream fis = new FileInputStream(path.toFile())) {
                Map<String, Object> config = yaml.load(fis);
                policy = parsePolicy(config);
            }
            
            // Compile regex for allowed argument characters
            String allowedChars = policy.getValidation().getAllowedArgCharacters();
            argCharPattern = Pattern.compile("^[" + allowedChars + "]+$");
            
            log.info("Loaded security policy: maxCommandLength={}, maxArgs={}, timeoutSeconds={}", 
                    policy.getLimits().getMaxCommandLength(),
                    policy.getLimits().getMaxArgs(),
                    policy.getLimits().getTimeoutSeconds());
            
        } catch (IOException e) {
            log.error("Failed to load policy file, using defaults", e);
            policy = createDefaultPolicy();
        }
    }
    
    private SecurityPolicy parsePolicy(Map<String, Object> config) {
        SecurityPolicy sp = new SecurityPolicy();
        
        // Parse limits
        Map<String, Object> limits = (Map<String, Object>) config.get("limits");
        if (limits != null) {
            Limits l = new Limits();
            l.setMaxCommandLength((Integer) limits.getOrDefault("maxCommandLength", 200));
            l.setMaxArgs((Integer) limits.getOrDefault("maxArgs", 8));
            l.setTimeoutSeconds((Integer) limits.getOrDefault("timeoutSeconds", 5));
            l.setMaxStdoutBytes((Integer) limits.getOrDefault("maxStdoutBytes", 131072));
            l.setMaxStderrBytes((Integer) limits.getOrDefault("maxStderrBytes", 65536));
            sp.setLimits(l);
        }
        
        // Parse allowlist
        Map<String, Object> allowlist = (Map<String, Object>) config.get("allowlist");
        if (allowlist != null) {
            Allowlist a = new Allowlist();
            a.setBinaries((List<String>) allowlist.getOrDefault("binaries", new ArrayList<>()));
            sp.setAllowlist(a);
        }
        
        // Parse denylist
        Map<String, Object> denylist = (Map<String, Object>) config.get("denylist");
        if (denylist != null) {
            Denylist d = new Denylist();
            d.setTokens((List<String>) denylist.getOrDefault("tokens", new ArrayList<>()));
            d.setMetacharacters((List<String>) denylist.getOrDefault("metacharacters", new ArrayList<>()));
            d.setSequences((List<String>) denylist.getOrDefault("sequences", new ArrayList<>()));
            sp.setDenylist(d);
        }
        
        // Parse validation
        Map<String, Object> validation = (Map<String, Object>) config.get("validation");
        if (validation != null) {
            Validation v = new Validation();
            v.setAllowedArgCharacters((String) validation.getOrDefault("allowedArgCharacters", "A-Za-z0-9._:/=-"));
            v.setRejectQuotes((Boolean) validation.getOrDefault("rejectQuotes", true));
            v.setRejectNewlines((Boolean) validation.getOrDefault("rejectNewlines", true));
            v.setRejectEscapes((Boolean) validation.getOrDefault("rejectEscapes", true));
            sp.setValidation(v);
        }
        
        return sp;
    }
    
    private SecurityPolicy createDefaultPolicy() {
        SecurityPolicy sp = new SecurityPolicy();
        
        Limits l = new Limits();
        l.setMaxCommandLength(200);
        l.setMaxArgs(8);
        l.setTimeoutSeconds(5);
        l.setMaxStdoutBytes(131072);
        l.setMaxStderrBytes(65536);
        sp.setLimits(l);
        
        Allowlist a = new Allowlist();
        a.setBinaries(Arrays.asList("echo", "date", "uname", "whoami", "id", "uptime", "printenv"));
        sp.setAllowlist(a);
        
        Denylist d = new Denylist();
        d.setTokens(Arrays.asList("rm", "sudo", "reboot", "shutdown", "halt", "kill"));
        d.setMetacharacters(Arrays.asList("`", "$", ";", "|", "&", ">", "<"));
        d.setSequences(Arrays.asList("&&", "||", "../"));
        sp.setDenylist(d);
        
        Validation v = new Validation();
        v.setAllowedArgCharacters("A-Za-z0-9._:/=-");
        v.setRejectQuotes(true);
        v.setRejectNewlines(true);
        v.setRejectEscapes(true);
        sp.setValidation(v);
        
        return sp;
    }
    
    /**
     * Validate command and return list of violations
     */
    public ValidationResult validate(String command) {
        List<String> violations = new ArrayList<>();
        
        if (command == null || command.trim().isEmpty()) {
            violations.add("Command cannot be empty");
            return new ValidationResult(false, violations);
        }
        
        command = command.trim();
        
        // Check length
        if (command.length() > policy.getLimits().getMaxCommandLength()) {
            violations.add("Command exceeds maximum length of " + 
                    policy.getLimits().getMaxCommandLength() + " characters");
        }
        
        // Check for newlines
        if (policy.getValidation().isRejectNewlines() && 
                (command.contains("\n") || command.contains("\r"))) {
            violations.add("Command contains newline characters");
        }
        
        // Check for quotes
        if (policy.getValidation().isRejectQuotes() && 
                (command.contains("\"") || command.contains("'"))) {
            violations.add("Command contains quote characters");
        }
        
        // Tokenize command
        String[] parts = command.split("\\s+");
        if (parts.length == 0) {
            violations.add("Command cannot be empty");
            return new ValidationResult(false, violations);
        }
        
        // Check arg count
        if (parts.length - 1 > policy.getLimits().getMaxArgs()) {
            violations.add("Command has too many arguments (max " + 
                    policy.getLimits().getMaxArgs() + ")");
        }
        
        String binary = parts[0];
        
        // Check if binary is in allowlist
        if (!policy.getAllowlist().getBinaries().contains(binary)) {
            violations.add("Binary '" + binary + "' is not in allowlist");
        }
        
        // Check for denied tokens
        for (String token : parts) {
            String lowerToken = token.toLowerCase();
            for (String deniedToken : policy.getDenylist().getTokens()) {
                if (lowerToken.equals(deniedToken.toLowerCase()) || 
                    lowerToken.contains(deniedToken.toLowerCase())) {
                    violations.add("Command contains denied token: " + deniedToken);
                }
            }
        }
        
        // Check for metacharacters
        for (String meta : policy.getDenylist().getMetacharacters()) {
            if (command.contains(meta)) {
                violations.add("Command contains denied metacharacter: " + meta);
            }
        }
        
        // Check for denied sequences
        for (String seq : policy.getDenylist().getSequences()) {
            if (command.contains(seq)) {
                violations.add("Command contains denied sequence: " + seq);
            }
        }
        
        // Check argument characters (skip the binary itself)
        for (int i = 1; i < parts.length; i++) {
            if (!argCharPattern.matcher(parts[i]).matches()) {
                violations.add("Argument '" + parts[i] + 
                        "' contains invalid characters (allowed: " + 
                        policy.getValidation().getAllowedArgCharacters() + ")");
            }
        }
        
        return new ValidationResult(violations.isEmpty(), violations);
    }
    
    public SecurityPolicy getPolicy() {
        return policy;
    }
    
    // Inner classes for policy structure
    @Data
    public static class SecurityPolicy {
        private Limits limits;
        private Allowlist allowlist;
        private Denylist denylist;
        private Validation validation;
    }
    
    @Data
    public static class Limits {
        private int maxCommandLength;
        private int maxArgs;
        private int timeoutSeconds;
        private int maxStdoutBytes;
        private int maxStderrBytes;
    }
    
    @Data
    public static class Allowlist {
        private List<String> binaries;
    }
    
    @Data
    public static class Denylist {
        private List<String> tokens;
        private List<String> metacharacters;
        private List<String> sequences;
    }
    
    @Data
    public static class Validation {
        private String allowedArgCharacters;
        private boolean rejectQuotes;
        private boolean rejectNewlines;
        private boolean rejectEscapes;
    }
    
    @Data
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> violations;
    }
}
