# Kubernetes Job Template for Kaiburr Executor

This document describes the Job specification used by `KubernetesCommandRunner` to execute commands.

## Template Structure

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: exec-<taskId>-<uuid>
  namespace: kaiburr
  labels:
    app: kaiburr-exec
    taskId: <taskId>
    execUuid: <uuid>
    owner: aditya-r
spec:
  # Auto-cleanup after completion
  ttlSecondsAfterFinished: 120
  
  # Kill job if it exceeds this duration
  activeDeadlineSeconds: 15
  
  # No retries (commands must be idempotent)
  backoffLimit: 0
  
  template:
    metadata:
      labels:
        app: kaiburr-exec
        taskId: <taskId>
        execUuid: <uuid>
    spec:
      restartPolicy: Never
      serviceAccountName: kaiburr-runner
      
      securityContext:
        # Pod-level security
        runAsNonRoot: true
        runAsUser: 65532
        runAsGroup: 65532
        fsGroup: 65532
        seccompProfile:
          type: RuntimeDefault
      
      containers:
      - name: executor
        image: kaiburr-executor:dev
        imagePullPolicy: IfNotPresent
        
        # Command and args injected at runtime
        command: ["/usr/bin/<binary>"]
        args: ["<arg1>", "<arg2>", ...]
        
        resources:
          requests:
            cpu: "50m"
            memory: "64Mi"
          limits:
            cpu: "200m"
            memory: "128Mi"
        
        securityContext:
          # Container-level security (defense in depth)
          runAsNonRoot: true
          runAsUser: 65532
          runAsGroup: 65532
          readOnlyRootFilesystem: true
          allowPrivilegeEscalation: false
          capabilities:
            drop:
              - ALL
          seccompProfile:
            type: RuntimeDefault
        
        # No environment variables (minimize attack surface)
        env: []
        
        # No volumes (read-only FS, no secrets)
        volumeMounts: []
      
      volumes: []
```

## Field Descriptions

### Metadata

- **name**: `exec-<taskId>-<uuid>` — Unique job name combining task ID and execution UUID
- **labels**:
  - `app: kaiburr-exec` — Identifies all executor jobs
  - `taskId: <taskId>` — Links to task
  - `execUuid: <uuid>` — Unique execution identifier

### Spec

- **ttlSecondsAfterFinished**: `120` — Jobs are auto-deleted 2 minutes after completion
- **activeDeadlineSeconds**: `15` — Jobs killed after 15 seconds (timeout protection)
- **backoffLimit**: `0` — No retries; fail immediately on error

### Pod Template

- **restartPolicy**: `Never` — Don't restart failed containers
- **serviceAccountName**: `kaiburr-runner` — RBAC identity (not used by executor, but required for audit)

### Security Context (Pod-level)

- **runAsNonRoot**: `true` — Reject images with `USER 0`
- **runAsUser**: `65532` — Nobody user
- **runAsGroup**: `65532` — Nobody group
- **fsGroup**: `65532` — File ownership group
- **seccompProfile**: `RuntimeDefault` — Syscall filtering

### Container Spec

- **image**: `kaiburr-executor:dev` — Distroless executor image
- **imagePullPolicy**: `IfNotPresent` — Use local image (kind/dev)
- **command**: Injected at runtime, e.g., `["/usr/bin/echo"]`
- **args**: Validated arguments, e.g., `["hello", "world"]`

### Resources

- **requests**: `cpu: 50m, memory: 64Mi` — Guaranteed resources
- **limits**: `cpu: 200m, memory: 128Mi` — Hard caps

### Container Security Context

- **readOnlyRootFilesystem**: `true` — Immutable container FS
- **allowPrivilegeEscalation**: `false` — No setuid/setgid
- **capabilities.drop**: `["ALL"]` — Remove all Linux capabilities

## Example: Echo Command

Input task:
```json
{
  "id": "task-001",
  "command": "echo",
  "args": ["Hello", "Kaiburr"]
}
```

Generated Job:
```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: exec-task-001-a1b2c3d4
  namespace: kaiburr
  labels:
    app: kaiburr-exec
    taskId: task-001
    execUuid: a1b2c3d4-e5f6-7890-abcd-ef1234567890
spec:
  ttlSecondsAfterFinished: 120
  activeDeadlineSeconds: 15
  backoffLimit: 0
  template:
    spec:
      restartPolicy: Never
      serviceAccountName: kaiburr-runner
      containers:
      - name: executor
        image: kaiburr-executor:dev
        command: ["/usr/bin/echo"]
        args: ["Hello", "Kaiburr"]
        # ... (security context and resources as above)
```

## Execution Flow

1. **Validation**: `CommandValidator` checks command against policy
2. **Job Creation**: `KubernetesCommandRunner` builds Job spec from template
3. **Submit**: Create Job via Kubernetes API
4. **Wait**: Poll Job status until completion or timeout
5. **Log Fetch**: Read stdout/stderr from Pod logs
6. **Cleanup**: TTL controller deletes Job after 120s

## Security Rationale

### Why Read-Only Root FS?

Prevents:
- Writing malware to disk
- Creating persistence mechanisms
- Modifying system binaries

### Why Drop All Capabilities?

Prevents:
- Network operations (CAP_NET_RAW)
- File ownership changes (CAP_CHOWN)
- System time changes (CAP_SYS_TIME)
- Module loading (CAP_SYS_MODULE)

### Why No Environment Variables?

Prevents:
- Secret injection attacks
- LD_PRELOAD attacks
- PATH manipulation

### Why TTL Controller?

Prevents:
- Job accumulation (resource exhaustion)
- Stale job cleanup overhead
- Namespace pollution

## Monitoring

Watch Jobs:
```bash
kubectl get jobs -n kaiburr -w
```

Check completed Jobs before TTL deletion:
```bash
kubectl get jobs -n kaiburr --field-selector status.successful=1
```

View logs:
```bash
kubectl logs -n kaiburr -l app=kaiburr-exec --tail=50
```

Describe failed Job:
```bash
kubectl describe job <job-name> -n kaiburr
```

## References

- [Kubernetes Jobs](https://kubernetes.io/docs/concepts/workloads/controllers/job/)
- [Pod Security Standards](https://kubernetes.io/docs/concepts/security/pod-security-standards/)
- [TTL Controller](https://kubernetes.io/docs/concepts/workloads/controllers/ttlafterfinished/)
