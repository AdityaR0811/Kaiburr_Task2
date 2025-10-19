package dev.adityar.kaiburr.task2.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Kubernetes client configuration.
 * 
 * Creates ApiClient from in-cluster config or kubeconfig for development.
 * 
 * @author Aditya R
 */
@Slf4j
@Configuration
@Profile("k8s")
public class KubernetesClientConfig {
    
    @Bean
    public ApiClient kubernetesApiClient() {
        try {
            // Try in-cluster config first (for running inside Kubernetes)
            try {
                ApiClient client = Config.fromCluster();
                log.info("Using in-cluster Kubernetes configuration");
                return client;
            } catch (Exception e) {
                // Fall back to kubeconfig (for local development with kubectl configured)
                ApiClient client = Config.defaultClient();
                log.info("Using default kubeconfig configuration");
                return client;
            }
        } catch (Exception e) {
            log.error("Failed to create Kubernetes API client", e);
            throw new IllegalStateException("Cannot connect to Kubernetes API", e);
        }
    }
}
