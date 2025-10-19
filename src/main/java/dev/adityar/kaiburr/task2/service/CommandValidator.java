package dev.adityar.kaiburr.task2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Policy-based command validator with hot-reload support.
 * 
 * Loads validation rules from command-policy.yaml and watches for changes.
 * Validates commands against denylist, allowlist, metacharacters, and limits.
 * 
 * @author Aditya R
 */
@Slf4j
@Component
public class CommandValidator {
    
    private volatile CommandPolicy policy;
    private final ExecutorService watcherExecutor = Executors.newSingleThreadExecutor();
    
    @PostConstruct
    public void init() {
        loadPolicy();
        startPolicyWatcher();
    }
    
    /**
     * Validate a command and its arguments.
     * 
     * @param command Command binary name
     * @param args Command arguments
     * @return ValidationResult with pass/fail and reasons
     */
    public ValidationResult validate(String command, List<String> args) {
        List<String> reasons = new ArrayList<>();
        
        if (command == null || command.isBlank()) {
            reasons.add("Command cannot be empty");
            return new ValidationResult(false, reasons);
        }
        
        if (args == null) {
            args = List.of();
        }
        
        // Check denylist commands
        if (policy.getDenylist().getCommands().contains(command)) {
            reasons.add(String.format("Command '%s' is denied by policy (dangerous operation)", command));
        }
        
        // Check allowlist binaries
        if (!policy.getAllowlist().getBinaries().contains(command)) {
            reasons.add(String.format("Command '%s' is not in allowlist", command));
        }
        
        // Check metacharacters in command
        for (String metachar : policy.getDenylist().getMetacharacters()) {
            if (command.contains(metachar)) {
                reasons.add(String.format("Command contains denied metacharacter: %s", metachar));
                break;
            }
        }
        
        // Check sequences in command
        for (String sequence : policy.getDenylist().getSequences()) {
            if (command.contains(sequence)) {
                reasons.add(String.format("Command contains denied sequence: %s", sequence));
                break;
            }
        }
        
        // Check arg count
        if (args.size() > policy.getLimits().getMaxArgs()) {
            reasons.add(String.format("Too many arguments: %d (max %d)", 
                args.size(), policy.getLimits().getMaxArgs()));
        }
        
        // Check total length
        int totalLength = command.length() + args.stream().mapToInt(String::length).sum();
        if (totalLength > policy.getLimits().getMaxTotalLength()) {
            reasons.add(String.format("Total command length %d exceeds limit %d", 
                totalLength, policy.getLimits().getMaxTotalLength()));
        }
        
        // Check arguments
        Pattern argPattern = Pattern.compile(policy.getLimits().getArgumentPattern());
        for (String arg : args) {
            // Check metacharacters
            for (String metachar : policy.getDenylist().getMetacharacters()) {
                if (arg.contains(metachar)) {
                    reasons.add(String.format("Argument contains denied metacharacter: %s", metachar));
                    break;
                }
            }
            
            // Check sequences
            for (String sequence : policy.getDenylist().getSequences()) {
                if (arg.contains(sequence)) {
                    reasons.add(String.format("Argument contains denied sequence: %s", sequence));
                    break;
                }
            }
            
            // Check pattern
            if (!argPattern.matcher(arg).matches()) {
                reasons.add(String.format("Argument '%s' does not match allowed pattern", arg));
            }
        }
        
        return new ValidationResult(reasons.isEmpty(), reasons);
    }
    
    /**
     * Load policy from classpath resource.
     */
    private void loadPolicy() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            this.policy = mapper.readValue(
                new ClassPathResource("command-policy.yaml").getInputStream(),
                CommandPolicy.class
            );
            log.info("Loaded command policy: {} allowlisted binaries, {} denylisted commands",
                policy.getAllowlist().getBinaries().size(),
                policy.getDenylist().getCommands().size());
        } catch (IOException e) {
            log.error("Failed to load command policy, using restrictive defaults", e);
            this.policy = createDefaultPolicy();
        }
    }
    
    /**
     * Watch policy file for changes and hot-reload.
     */
    private void startPolicyWatcher() {
        watcherExecutor.submit(() -> {
            try {
                Path policyPath = Paths.get("src/main/resources/command-policy.yaml");
                if (!Files.exists(policyPath)) {
                    log.warn("Policy file not found at {}, hot-reload disabled", policyPath);
                    return;
                }
                
                WatchService watcher = FileSystems.getDefault().newWatchService();
                policyPath.getParent().register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
                
                log.info("Watching policy file for changes: {}", policyPath);
                
                while (true) {
                    WatchKey key = watcher.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path changed = (Path) event.context();
                        if (changed.toString().equals("command-policy.yaml")) {
                            log.info("Policy file modified, reloading...");
                            Thread.sleep(100); // Debounce
                            loadPolicy();
                        }
                    }
                    key.reset();
                }
            } catch (Exception e) {
                log.error("Policy watcher failed", e);
            }
        });
    }
    
    /**
     * Create restrictive default policy if loading fails.
     */
    private CommandPolicy createDefaultPolicy() {
        CommandPolicy defaultPolicy = new CommandPolicy();
        
        // Minimal allowlist
        defaultPolicy.setAllowlist(new CommandPolicy.Allowlist());
        defaultPolicy.getAllowlist().setBinaries(List.of("echo"));
        
        // Comprehensive denylist
        defaultPolicy.setDenylist(new CommandPolicy.Denylist());
        defaultPolicy.getDenylist().setCommands(List.of("rm", "sudo", "curl", "wget"));
        defaultPolicy.getDenylist().setMetacharacters(List.of(";", "|", "&", ">", "<", "`", "$"));
        defaultPolicy.getDenylist().setSequences(List.of("&&", "||", "../"));
        
        // Conservative limits
        defaultPolicy.setLimits(new CommandPolicy.Limits());
        defaultPolicy.getLimits().setMaxArgs(4);
        defaultPolicy.getLimits().setMaxTotalLength(100);
        defaultPolicy.getLimits().setArgumentPattern("^[A-Za-z0-9._-]{1,32}$");
        
        return defaultPolicy;
    }
    
    /**
     * Validation result DTO.
     */
    @Data
    @AllArgsConstructor
    public static class ValidationResult {
        private boolean valid;
        private List<String> reasons;
    }
    
    /**
     * Command policy model matching command-policy.yaml structure.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommandPolicy {
        private Denylist denylist;
        private Allowlist allowlist;
        private Limits limits;
        private Timeouts timeouts;
        private Output output;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Denylist {
            private List<String> commands;
            private List<String> metacharacters;
            private List<String> sequences;
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Allowlist {
            private List<String> binaries;
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Limits {
            private int maxArgs;
            private int maxTotalLength;
            private String argumentPattern;
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Timeouts {
            private int apiClientSeconds;
            private int jobActiveDeadlineSeconds;
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Output {
            private int maxStdoutBytes;
            private int maxStderrBytes;
            private String truncationMarker;
        }
    }
}
