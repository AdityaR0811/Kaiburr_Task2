# RUN CHECK — Kaiburr Task 2 Verification

**Author:** Aditya R  
**Purpose:** Complete step-by-step verification of Task 2

Copy and paste these commands to verify the complete Kaiburr Task 2 system.

---

## Prerequisites Check

```bash
# Verify tools are installed
kind --version          # Should be v0.17.0 or later
kubectl version --client    # Should be v1.25.0 or later
docker --version        # Should be 20.10 or later
java -version           # Should be 17 or later
mvn --version           # Should be 3.8 or later
```

---

## Step 1: Create kind Cluster

```bash
# Create cluster
kind create cluster --name kaiburr --config - <<EOF
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  image: kindest/node:v1.28.0
EOF

# Verify cluster
kubectl cluster-info --context kind-kaiburr
kubectl get nodes
```

**Expected Output:**
```
Kubernetes control plane is running at https://127.0.0.1:xxxxx
Creating cluster "kaiburr" ...
 ✓ Ensuring node image (kindest/node:v1.28.0) 🖼
 ✓ Preparing nodes 📦
 ✓ Writing configuration 📜
 ✓ Starting control-plane 🕹️
 ✓ Installing CNI 🔌
 ✓ Installing StorageClass 💾
Set kubectl context to "kind-kaiburr"
You can now use your cluster with:

kubectl cluster-info --context kind-kaiburr
```

---

## Step 2: Build and Load Executor Image

```bash
# Navigate to executor directory
cd executor

# Build executor image
docker build -t kaiburr-executor:dev .

# Verify image
docker images | grep kaiburr-executor

# Load into kind
kind load docker-image kaiburr-executor:dev --name kaiburr

# Verify loaded
docker exec -it kaiburr-control-plane crictl images | grep kaiburr-executor

cd ..
```

**Expected Output:**
```
Successfully built abc123def456
Successfully tagged kaiburr-executor:dev
Image: "kaiburr-executor:dev" with ID "sha256:abc..." not yet present on node "kaiburr-control-plane", loading...
```

---

## Step 3: Apply Kubernetes Manifests

```bash
# Create namespace
kubectl create namespace kaiburr

# Apply all manifests
kubectl apply -f deploy/k8s/

# Verify resources
kubectl get all -n kaiburr
kubectl get sa,role,rolebinding,networkpolicy,configmap -n kaiburr
```

**Expected Output:**
```
namespace/kaiburr created
serviceaccount/kaiburr-runner created
role.rbac.authorization.k8s.io/kaiburr-job-manager created
rolebinding.rbac.authorization.k8s.io/kaiburr-runner-binding created
networkpolicy.networking.k8s.io/kaiburr-executor-netpol created
configmap/kaiburr-policy created
```

---

## Step 4: Start MongoDB

```bash
# Start MongoDB container
docker run -d \
  --name mongo-kaiburr \
  -p 27017:27017 \
  mongo:5.0

# Verify MongoDB is running
docker ps | grep mongo-kaiburr

# Test connection
docker exec -it mongo-kaiburr mongosh --eval "db.version()"
```

**Expected Output:**
```
5.0.x
```

---

## Step 5: Build Application

```bash
# Build with Maven
mvn -f pom-task2.xml clean package

# Verify JAR created
ls -lh target/task2-1.0.0-SNAPSHOT.jar
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
-rw-r--r-- 1 user user 45M Oct 19 10:30 target/task2-1.0.0-SNAPSHOT.jar
```

---

## Step 6: Run Application

```bash
# Run with Kubernetes profile
SPRING_PROFILES_ACTIVE=k8s \
MONGODB_URI=mongodb://localhost:27017/kaiburrdb \
K8S_NAMESPACE=kaiburr \
K8S_EXECUTOR_IMAGE=kaiburr-executor:dev \
java -jar target/task2-1.0.0-SNAPSHOT.jar
```

**Expected Output:**
```
 _  __     _ _                       _____         _      ___  
| |/ /    (_) |                     |_   _|       | |    |__ \ 
| ' / __ _ _| |__  _   _ _ __ _ __    | | __ _ ___| | __    ) |
|  < / _` | | '_ \| | | | '__| '__|   | |/ _` / __| |/ /   / / 
| . \ (_| | | |_) | |_| | |  | |      | | (_| \__ \   <   / /_ 
|_|\_\__,_|_|_.__/ \__,_|_|  |_|      \_/\__,_|___/_|\_\ |____|

========================================
  Kubernetes Job Execution Engine
  Author: Aditya R
  Assessment: Kaiburr 2025 — Task 2
========================================

2025-10-19 10:30:45.123  INFO [main] [...] - Starting KaiburrTask2Application
2025-10-19 10:30:45.456  INFO [main] [...] - Using in-cluster Kubernetes configuration
2025-10-19 10:30:45.789  INFO [main] [...] - Loaded command policy: 10 allowlisted binaries, 45 denylisted commands
2025-10-19 10:30:46.000  INFO [main] [...] - Started KaiburrTask2Application in 2.5 seconds
```

**Leave this terminal running. Open a new terminal for the next steps.**

---

## Step 7: Test API Endpoints

### 7a. Health Check

```bash
curl -s http://localhost:8080/actuator/health | jq .
```

**Expected:**
```json
{
  "status": "UP",
  "components": {
    "mongo": {
      "status": "UP"
    }
  }
}
```

### 7b. Create Task

```bash
curl -s -X PUT http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "id": "task-echo",
    "name": "Echo Hello",
    "command": "echo",
    "args": ["Hello", "Kaiburr"],
    "assignee": "Aditya R"
  }' | jq .
```

**Expected:**
```json
{
  "id": "task-echo",
  "name": "Echo Hello",
  "command": "echo",
  "args": ["Hello", "Kaiburr"],
  "assignee": "Aditya R",
  "executions": []
}
```

### 7c. Validate Command (Dry-Run)

```bash
# Test safe command
curl -s -X POST http://localhost:8080/api/validation/command \
  -H "Content-Type: application/json" \
  -d '{
    "command": "echo",
    "args": ["test"]
  }' | jq .
```

**Expected:**
```json
{
  "valid": true,
  "reasons": []
}
```

```bash
# Test dangerous command
curl -s -X POST http://localhost:8080/api/validation/command \
  -H "Content-Type: application/json" \
  -d '{
    "command": "rm",
    "args": ["-rf", "/"]
  }' | jq .
```

**Expected:**
```json
{
  "valid": false,
  "reasons": [
    "Command 'rm' is denied by policy (dangerous operation)",
    "Command 'rm' is not in allowlist"
  ]
}
```

### 7d. Execute Task (Creates Kubernetes Job)

```bash
curl -s -X PUT http://localhost:8080/api/tasks/task-echo/executions | jq .
```

**Expected:**
```json
{
  "taskId": "task-echo",
  "executionId": "abc123-...",
  "jobName": "exec-task-echo-xyz789",
  "status": "SUCCEEDED",
  "exitCode": 0,
  "stdout": "Hello Kaiburr\n",
  "stderr": "",
  "durationMs": 847,
  "startedAt": "2025-10-19T10:35:00.123Z",
  "completedAt": "2025-10-19T10:35:00.970Z"
}
```

---

## Step 8: Verify Kubernetes Resources

### 8a. Check Jobs

```bash
kubectl get jobs -n kaiburr
```

**Expected:**
```
NAME                    COMPLETIONS   DURATION   AGE
exec-task-echo-xyz789   1/1           1s         30s
```

### 8b. Check Pods

```bash
kubectl get pods -n kaiburr
```

**Expected:**
```
NAME                          READY   STATUS      RESTARTS   AGE
exec-task-echo-xyz789-abcd    0/1     Completed   0          1m
```

### 8c. View Pod Logs

```bash
kubectl logs -n kaiburr -l app=kaiburr-exec --tail=20
```

**Expected:**
```
Hello Kaiburr
```

### 8d. Describe Job

```bash
kubectl describe job -n kaiburr $(kubectl get jobs -n kaiburr -o name | head -1)
```

**Expected:**
```
Name:             exec-task-echo-xyz789
Namespace:        kaiburr
Labels:           app=kaiburr-exec
                  owner=aditya-r
                  taskId=task-echo
Annotations:      <none>
Parallelism:      1
Completions:      1
Completion Mode:  NonIndexed
Start Time:       Fri, 19 Oct 2025 10:35:00 +0000
Completed At:     Fri, 19 Oct 2025 10:35:01 +0000
Duration:         1s
Pods Statuses:    0 Active / 1 Succeeded / 0 Failed
Pod Template:
  Labels:       app=kaiburr-exec
                taskId=task-echo
  Service Account:  kaiburr-runner
  Containers:
   executor:
    Image:      kaiburr-executor:dev
    Port:       <none>
    Host Port:  <none>
    Command:
      /usr/bin/echo
    Args:
      Hello
      Kaiburr
    Environment:  <none>
    Mounts:       <none>
  Volumes:        <none>
Events:
  Type    Reason            Age   From            Message
  ----    ------            ----  ----            -------
  Normal  SuccessfulCreate  1m    job-controller  Created pod: exec-task-echo-xyz789-abcd
  Normal  Completed         1m    job-controller  Job completed
```

### 8e. Verify Security Context

```bash
kubectl get pod -n kaiburr $(kubectl get pods -n kaiburr -o name | head -1) -o jsonpath='{.spec.containers[0].securityContext}' | jq .
```

**Expected:**
```json
{
  "allowPrivilegeEscalation": false,
  "capabilities": {
    "drop": ["ALL"]
  },
  "readOnlyRootFilesystem": true,
  "runAsGroup": 65532,
  "runAsNonRoot": true,
  "runAsUser": 65532,
  "seccompProfile": {
    "type": "RuntimeDefault"
  }
}
```

---

## Step 9: Verify MongoDB Persistence

```bash
docker exec -it mongo-kaiburr mongosh kaiburrdb --eval '
db.tasks.find({id: "task-echo"}).pretty()
'
```

**Expected:**
```javascript
{
  _id: 'task-echo',
  name: 'Echo Hello',
  command: 'echo',
  args: ['Hello', 'Kaiburr'],
  assignee: 'Aditya R',
  executions: [
    {
      id: 'abc123-...',
      jobName: 'exec-task-echo-xyz789',
      status: 'SUCCEEDED',
      exitCode: 0,
      stdout: 'Hello Kaiburr\n',
      stderr: '',
      durationMs: 847,
      startedAt: ISODate('2025-10-19T10:35:00.123Z'),
      completedAt: ISODate('2025-10-19T10:35:00.970Z')
    }
  ]
}
```

---

## Step 10: Verify Metrics

```bash
curl -s http://localhost:8080/actuator/prometheus | grep kaiburr_executions
```

**Expected:**
```
# HELP kaiburr_executions_total  
# TYPE kaiburr_executions_total counter
kaiburr_executions_total{result="success",} 1.0
```

---

## Step 11: Test Timeout Scenario

```bash
# Create a task that will timeout (would sleep forever if shell existed)
# Since we don't have sleep, simulate with uname and verify activeDeadlineSeconds works

# Create task
curl -s -X PUT http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "id": "task-uname",
    "name": "System Info",
    "command": "uname",
    "args": ["-a"],
    "assignee": "Aditya R"
  }' | jq .

# Execute
curl -s -X PUT http://localhost:8080/api/tasks/task-uname/executions | jq .
```

---

## Step 12: Verify TTL Cleanup

```bash
# Wait 2 minutes and check if Jobs are cleaned up
echo "Waiting 120 seconds for TTL cleanup..."
sleep 120

kubectl get jobs -n kaiburr
```

**Expected:**
```
No resources found in kaiburr namespace.
```

---

## Step 13: Test Error Handling

### 13a. Invalid Command (Not in Allowlist)

```bash
curl -s -X PUT http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "id": "task-invalid",
    "name": "Invalid Command",
    "command": "curl",
    "args": ["http://attacker.com"],
    "assignee": "Aditya R"
  }' | jq .
```

**Expected:**
```json
{
  "timestamp": "2025-10-19T10:40:00.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Command validation failed: Command 'curl' is denied by policy (dangerous operation); Command 'curl' is not in allowlist",
  "path": "/api/tasks",
  "correlationId": "xyz-..."
}
```

### 13b. Task Not Found

```bash
curl -s http://localhost:8080/api/tasks/nonexistent | jq .
```

**Expected:**
```json
{
  "timestamp": "2025-10-19T10:41:00.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "...",
  "path": "/api/tasks/nonexistent",
  "correlationId": "abc-..."
}
```

---

## Step 14: Swagger UI

```bash
# Open in browser
open http://localhost:8080/swagger-ui.html

# Or curl
curl -s http://localhost:8080/api-docs | jq .info
```

**Expected:**
```json
{
  "title": "Kaiburr Task 2 — Kubernetes Job Execution API",
  "description": "Production-grade REST API for executing commands via Kubernetes Jobs with policy-based validation and security hardening",
  "contact": {
    "name": "Aditya R"
  },
  "license": {
    "name": "MIT License",
    "url": "https://opensource.org/licenses/MIT"
  },
  "version": "1.0.0"
}
```

---

## Step 15: Cleanup

```bash
# Stop application (Ctrl+C in the terminal running the app)

# Delete kind cluster
kind delete cluster --name kaiburr

# Stop MongoDB
docker stop mongo-kaiburr
docker rm mongo-kaiburr

# Remove images (optional)
docker rmi kaiburr-executor:dev
```

---

## ✅ Verification Checklist

- [ ] kind cluster created successfully
- [ ] Executor image built and loaded
- [ ] Kubernetes manifests applied (namespace, RBAC, NetworkPolicy, ConfigMap)
- [ ] MongoDB running and accessible
- [ ] Application starts with Kubernetes profile
- [ ] Health check returns UP
- [ ] Task creation succeeds
- [ ] Command validation (dry-run) works for safe and dangerous commands
- [ ] Task execution creates Kubernetes Job
- [ ] Job completes successfully
- [ ] Pod logs contain expected output
- [ ] Security context enforced (non-root, dropped caps, read-only FS)
- [ ] Execution persisted to MongoDB
- [ ] Metrics exposed at /actuator/prometheus
- [ ] TTL cleanup deletes Jobs after 120 seconds
- [ ] Invalid commands rejected with proper error responses
- [ ] Swagger UI accessible
- [ ] Cleanup successful

---

## Common Issues

### Issue: `kind: command not found`
**Solution:** Install kind from https://kind.sigs.k8s.io/docs/user/quick-start/

### Issue: `Cannot connect to Kubernetes API`
**Solution:** Ensure kubeconfig is set correctly:
```bash
kubectl config use-context kind-kaiburr
```

### Issue: `Image not found in kind`
**Solution:** Reload image:
```bash
kind load docker-image kaiburr-executor:dev --name kaiburr
```

### Issue: `MongoDB connection refused`
**Solution:** Check MongoDB is running:
```bash
docker ps | grep mongo
# If not running, start it:
docker run -d --name mongo-kaiburr -p 27017:27017 mongo:5.0
```

### Issue: `Job stuck in Pending`
**Solution:** Check pod events:
```bash
kubectl describe pod -n kaiburr $(kubectl get pods -n kaiburr -o name | head -1)
```

---

**Built with ❤️ by Aditya R for Kaiburr Assessment 2025**
