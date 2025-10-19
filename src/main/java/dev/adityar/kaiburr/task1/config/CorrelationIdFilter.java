package dev.adityar.kaiburr.task1.config;

import dev.adityar.kaiburr.task1.util.CorrelationId;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to handle correlation IDs for request tracing
 * Author: Aditya R.
 */
@Slf4j
@Component
public class CorrelationIdFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        long startTime = System.currentTimeMillis();
        
        try {
            String correlationId = httpRequest.getHeader(CorrelationId.CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = CorrelationId.generate();
            }
            
            CorrelationId.set(correlationId);
            httpResponse.setHeader(CorrelationId.CORRELATION_ID_HEADER, correlationId);
            
            log.info("Request started: method={}, path={}, correlationId={}", 
                    httpRequest.getMethod(), 
                    httpRequest.getRequestURI(), 
                    correlationId);
            
            chain.doFilter(request, response);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Request completed: method={}, path={}, status={}, duration={}ms, correlationId={}", 
                    httpRequest.getMethod(), 
                    httpRequest.getRequestURI(), 
                    httpResponse.getStatus(), 
                    duration, 
                    correlationId);
            
        } finally {
            CorrelationId.clear();
        }
    }
}
