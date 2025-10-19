package dev.adityar.kaiburr.task2.service;

import dev.adityar.kaiburr.task2.domain.Task;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Kubernetes Job-based command runner for production.
 * 
 * Creates a Kubernetes Job per execution with security hardening,
 * waits for completion, fetches logs, and handles cleanup.
 * 
 * Activated with profile=k8s.
 * 
 * @author Aditya R
 */
@Slf4j
@Service
@Profile("k8s")
@RequiredArgsConstructor
public class KubernetesCommandRunner implements CommandRunner {
    
    private final ApiClient apiClient;
    
    @Value("${k8s.namespace:kaiburr}")
    private String namespace;
    
    @Value("${k8s.executor.image:kaiburr-executor:dev}")
    private String executorImage;
    
    @Value("${k8s.ttl.seconds:120}")
    private Integer ttlSeconds;
    
    @Value("${k8s.active-deadline.seconds:15}")
    private Integer activeDeadlineSeconds;
    
    @Value("${k8s.backoff-limit:0}")
    private Integer backoffLimit;
    
    @Value("${k8s.pull-policy:IfNotPresent}")
    private String imagePullPolicy;
    
    @Value("${k8s.max-stdout-bytes:131072}")
    private int maxStdoutBytes;
    
    @Value("${k8s.max-stderr-bytes:65536}")
    private int maxStderrBytes;
    
    @Override
    public ExecutionResult execute(Task task) {
        String jobName = generateJobName(task.getId());
        String execUuid = UUID.randomUUID().toString();
        Instant startTime = Instant.now();
        
        try {
            log.info("Creating Kubernetes Job: {} for task: {}", jobName, task.getId());
            
            // Create Job
            V1Job job = buildJobSpec(jobName, task.getId(), execUuid, task.getCommand(), task.getArgs());
            BatchV1Api batchApi = new BatchV1Api(apiClient);
            batchApi.createNamespacedJob(namespace, job, null, null, null, null);
            
            log.info("Job created: {}", jobName);
            
            // Wait for completion
            boolean completed = waitForJobCompletion(jobName);
            
            if (!completed) {
                log.warn("Job {} did not complete within timeout", jobName);
                return ExecutionResult.builder()
                    .jobName(jobName)
                    .exitCode(-1)
                    .stdout("")
                    .stderr("Job timeout exceeded")
                    .durationMs(Duration.between(startTime, Instant.now()).toMillis())
                    .timeout(true)
                    .build();
            }
            
            // Fetch logs
            String podName = findPodForJob(jobName);
            if (podName == null) {
                throw new CommandExecutionException("Could not find pod for job: " + jobName);
            }
            
            String stdout = fetchPodLogs(podName, false);
            String stderr = fetchPodLogs(podName, true);
            
            // Get exit code from Pod status
            int exitCode = getPodExitCode(podName);
            
            long durationMs = Duration.between(startTime, Instant.now()).toMillis();
            
            log.info("Job {} completed: exitCode={}, duration={}ms", jobName, exitCode, durationMs);
            
            return ExecutionResult.builder()
                .jobName(jobName)
                .exitCode(exitCode)
                .stdout(truncateOutput(stdout, maxStdoutBytes))
                .stderr(truncateOutput(stderr, maxStderrBytes))
                .durationMs(durationMs)
                .timeout(false)
                .build();
                
        } catch (ApiException e) {
            log.error("Kubernetes API error creating job: code={}, body={}", e.getCode(), e.getResponseBody(), e);
            throw new CommandExecutionException("Failed to create Kubernetes job: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error executing command in Kubernetes", e);
            throw new CommandExecutionException("Kubernetes execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Build Kubernetes Job specification with security hardening.
     */
    private V1Job buildJobSpec(String jobName, String taskId, String execUuid, String command, List<String> args) {
        // Build command as full path
        List<String> containerCommand = List.of("/usr/bin/" + command);
        List<String> containerArgs = args != null ? args : List.of();
        
        return new V1Job()
            .apiVersion("batch/v1")
            .kind("Job")
            .metadata(new V1ObjectMeta()
                .name(jobName)
                .namespace(namespace)
                .labels(Map.of(
                    "app", "kaiburr-exec",
                    "taskId", taskId,
                    "execUuid", execUuid,
                    "owner", "aditya-r"
                )))
            .spec(new V1JobSpec()
                .ttlSecondsAfterFinished(ttlSeconds)
                .activeDeadlineSeconds((long) activeDeadlineSeconds)  // Cast to Long
                .backoffLimit(backoffLimit)
                .template(new V1PodTemplateSpec()
                    .metadata(new V1ObjectMeta()
                        .labels(Map.of(
                            "app", "kaiburr-exec",
                            "taskId", taskId,
                            "execUuid", execUuid
                        )))
                    .spec(new V1PodSpec()
                        .restartPolicy("Never")
                        .serviceAccountName("kaiburr-runner")
                        .securityContext(new V1PodSecurityContext()
                            .runAsNonRoot(true)
                            .runAsUser(65532L)
                            .runAsGroup(65532L)
                            .fsGroup(65532L)
                            .seccompProfile(new V1SeccompProfile().type("RuntimeDefault")))
                        .containers(List.of(new V1Container()
                            .name("executor")
                            .image(executorImage)
                            .imagePullPolicy(imagePullPolicy)
                            .command(containerCommand)
                            .args(containerArgs)
                            .resources(new V1ResourceRequirements()
                                .requests(Map.of(
                                    "cpu", new Quantity("50m"),
                                    "memory", new Quantity("64Mi")
                                ))
                                .limits(Map.of(
                                    "cpu", new Quantity("200m"),
                                    "memory", new Quantity("128Mi")
                                )))
                            .securityContext(new V1SecurityContext()
                                .runAsNonRoot(true)
                                .runAsUser(65532L)
                                .runAsGroup(65532L)
                                .readOnlyRootFilesystem(true)
                                .allowPrivilegeEscalation(false)
                                .capabilities(new V1Capabilities()
                                    .drop(List.of("ALL")))
                                .seccompProfile(new V1SeccompProfile().type("RuntimeDefault"))))))));
    }
    
    /**
     * Wait for Job to complete (success or failure).
     */
    private boolean waitForJobCompletion(String jobName) throws ApiException, InterruptedException {
        BatchV1Api batchApi = new BatchV1Api(apiClient);
        int maxAttempts = 60; // 60 attempts * 1s = 60s max wait (covers activeDeadlineSeconds + buffer)
        int attempts = 0;
        
        while (attempts < maxAttempts) {
            V1Job job = batchApi.readNamespacedJobStatus(jobName, namespace, null);
            V1JobStatus status = job.getStatus();
            
            if (status != null) {
                // Check for completion
                if (status.getSucceeded() != null && status.getSucceeded() > 0) {
                    log.info("Job {} succeeded", jobName);
                    return true;
                }
                
                if (status.getFailed() != null && status.getFailed() > 0) {
                    log.warn("Job {} failed", jobName);
                    return true; // Still consider completed (will have non-zero exit code)
                }
            }
            
            Thread.sleep(1000);
            attempts++;
        }
        
        log.warn("Job {} did not complete within max wait time", jobName);
        return false;
    }
    
    /**
     * Find Pod name for a Job using label selectors.
     */
    private String findPodForJob(String jobName) throws ApiException {
        CoreV1Api coreApi = new CoreV1Api(apiClient);
        
        // Extract execUuid from job name (format: exec-{taskId}-{uuid})
        String labelSelector = "app=kaiburr-exec";
        
        V1PodList pods = coreApi.listNamespacedPod(
            namespace,
            null,  // pretty
            null,  // allowWatchBookmarks
            null,  // continue
            null,  // fieldSelector
            labelSelector,  // labelSelector
            null,  // limit
            null,  // resourceVersion
            null,  // resourceVersionMatch
            null,  // sendInitialEvents
            null,  // timeoutSeconds
            null   // watch
        );
        
        // Find pod with matching job name prefix
        return pods.getItems().stream()
            .filter(pod -> {
                Map<String, String> labels = pod.getMetadata().getLabels();
                if (labels == null) return false;
                
                // Match by controller-uid or just find the most recent pod
                return pod.getMetadata().getName().startsWith(jobName);
            })
            .map(pod -> pod.getMetadata().getName())
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Fetch logs from Pod container.
     */
    private String fetchPodLogs(String podName, boolean previous) throws ApiException {
        CoreV1Api coreApi = new CoreV1Api(apiClient);
        
        try {
            String logs = coreApi.readNamespacedPodLog(
                podName,
                namespace,
                "executor",  // container name
                null,        // follow
                null,        // insecureSkipTLSVerifyBackend
                null,        // limitBytes
                null,        // pretty
                previous,    // previous
                null,        // sinceSeconds
                null,        // tailLines
                null         // timestamps
            );
            
            return logs != null ? logs : "";
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                log.warn("Pod {} not found for log fetch", podName);
                return "";
            }
            throw e;
        }
    }
    
    /**
     * Get exit code from Pod container status.
     */
    private int getPodExitCode(String podName) throws ApiException {
        CoreV1Api coreApi = new CoreV1Api(apiClient);
        V1Pod pod = coreApi.readNamespacedPodStatus(podName, namespace, null);
        
        if (pod.getStatus() != null && pod.getStatus().getContainerStatuses() != null) {
            for (V1ContainerStatus status : pod.getStatus().getContainerStatuses()) {
                if ("executor".equals(status.getName()) && status.getState() != null) {
                    V1ContainerStateTerminated terminated = status.getState().getTerminated();
                    if (terminated != null) {
                        return terminated.getExitCode();
                    }
                }
            }
        }
        
        return -1; // Unknown exit code
    }
    
    /**
     * Truncate output to max bytes with marker.
     */
    private String truncateOutput(String output, int maxBytes) {
        if (output == null) {
            return "";
        }
        
        if (output.length() > maxBytes) {
            return output.substring(0, maxBytes) + "\n⟂TRUNCATED";
        }
        
        return output;
    }
    
    /**
     * Generate unique job name from task ID.
     */
    private String generateJobName(String taskId) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String sanitized = taskId.replaceAll("[^a-z0-9-]", "-").toLowerCase();
        if (sanitized.length() > 30) {
            sanitized = sanitized.substring(0, 30);
        }
        return "exec-" + sanitized + "-" + uuid;
    }
}
