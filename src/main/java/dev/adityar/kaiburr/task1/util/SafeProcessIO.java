package dev.adityar.kaiburr.task1.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Safe process I/O handler with timeouts and output truncation
 * Author: Aditya R.
 */
@Slf4j
public class SafeProcessIO {
    
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    
    /**
     * Execute process with timeout and capture output
     */
    public static ProcessResult executeWithTimeout(
            Process process,
            long timeoutSeconds,
            int maxStdoutBytes,
            int maxStderrBytes) throws InterruptedException {
        
        Future<String> stdoutFuture = executorService.submit(() -> {
            try {
                return readStream(process.getInputStream(), maxStdoutBytes);
            } catch (IOException e) {
                return "Error reading stdout: " + e.getMessage();
            }
        });
        
        Future<String> stderrFuture = executorService.submit(() -> {
            try {
                return readStream(process.getErrorStream(), maxStderrBytes);
            } catch (IOException e) {
                return "Error reading stderr: " + e.getMessage();
            }
        });
        
        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        
        if (!completed) {
            process.destroyForcibly();
            stdoutFuture.cancel(true);
            stderrFuture.cancel(true);
            return new ProcessResult(-1, "", "Process execution timed out after " + timeoutSeconds + " seconds", true);
        }
        
        try {
            String stdout = stdoutFuture.get(1, TimeUnit.SECONDS);
            String stderr = stderrFuture.get(1, TimeUnit.SECONDS);
            int exitCode = process.exitValue();
            return new ProcessResult(exitCode, stdout, stderr, false);
        } catch (Exception e) {
            log.error("Error reading process output", e);
            return new ProcessResult(-1, "", "Error reading output: " + e.getMessage(), true);
        }
    }
    
    /**
     * Read stream with size limit
     */
    private static String readStream(InputStream inputStream, int maxBytes) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            char[] buffer = new char[8192];
            int bytesRead = 0;
            int totalBytes = 0;
            
            while ((bytesRead = reader.read(buffer)) != -1 && totalBytes < maxBytes) {
                int toAppend = Math.min(bytesRead, maxBytes - totalBytes);
                output.append(buffer, 0, toAppend);
                totalBytes += toAppend;
            }
            
            if (totalBytes >= maxBytes) {
                output.append("\n[OUTPUT TRUNCATED - LIMIT REACHED]");
            }
        }
        return output.toString();
    }
    
    public static class ProcessResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;
        private final boolean timedOut;
        
        public ProcessResult(int exitCode, String stdout, String stderr, boolean timedOut) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
            this.timedOut = timedOut;
        }
        
        public int getExitCode() { return exitCode; }
        public String getStdout() { return stdout; }
        public String getStderr() { return stderr; }
        public boolean isTimedOut() { return timedOut; }
    }
    
    public static class TimeoutException extends RuntimeException {
        public TimeoutException(String message) {
            super(message);
        }
    }
}
