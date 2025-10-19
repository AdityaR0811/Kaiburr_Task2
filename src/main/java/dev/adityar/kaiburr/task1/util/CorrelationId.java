package dev.adityar.kaiburr.task1.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility for managing correlation IDs across request lifecycle
 * Author: Aditya R.
 */
public class CorrelationId {
    
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_KEY = "correlationId";
    
    private CorrelationId() {
        // Utility class
    }
    
    public static String generate() {
        return UUID.randomUUID().toString();
    }
    
    public static void set(String correlationId) {
        MDC.put(CORRELATION_ID_KEY, correlationId);
    }
    
    public static String get() {
        String id = MDC.get(CORRELATION_ID_KEY);
        if (id == null) {
            id = generate();
            set(id);
        }
        return id;
    }
    
    public static void clear() {
        MDC.remove(CORRELATION_ID_KEY);
    }
}
