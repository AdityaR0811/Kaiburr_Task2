package dev.adityar.kaiburr.task1.service;

import dev.adityar.kaiburr.task1.domain.TaskExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Kubernetes command runner stub for future implementation
 * Author: Aditya R.
 */
@Slf4j
@Service
@Profile("k8s")
public class KubernetesCommandRunner implements CommandRunner {
    
    @Override
    public TaskExecution execute(String command, String correlationId) throws Exception {
        log.warn("Kubernetes command runner invoked but not yet implemented");
        throw new ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                "Kubernetes command execution not yet implemented. Use local profile for Task 1."
        );
    }
}
