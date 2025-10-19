# How to Run Kaiburr Task 2 Application

**Author:** Aditya R  
**Platform:** Windows with PowerShell

---

## 🚀 Quick Start (Recommended)

### Option 1: Automated Setup

Simply run the PowerShell script:

```powershell
.\run-task2.ps1
```

This script will:
1. Check/start MongoDB
2. Ask you to choose Local or Kubernetes mode
3. Set up the environment
4. Build and run the application

---

## 📋 Manual Setup

### Prerequisites

Ensure you have installed:
- ✅ Java 17 or later
- ✅ Maven 3.8+
- ✅ Docker Desktop
- ✅ kind (for Kubernetes mode)
- ✅ kubectl (for Kubernetes mode)

Verify:
```powershell
java -version
mvn -version
docker --version
kind --version    # Only for K8s mode
kubectl version   # Only for K8s mode
```

---

### Step 1: Start MongoDB

```powershell
# Start MongoDB container
docker run -d --name mongo-kaiburr -p 27017:27017 mongo:5.0

# Verify it's running
docker ps | Select-String mongo-kaiburr
```

---

### Step 2: Build the Application

```powershell
# Build with Maven
mvn clean package -DskipTests
```

This will create `target/task1-1.0.0-SNAPSHOT.jar`

---

### Step 3A: Run in Local Mode (Simple - No Kubernetes)

This mode uses local process execution (fork/exec) instead of Kubernetes Jobs.

```powershell
# Set environment variables
$env:SPRING_PROFILES_ACTIVE = "local"
$env:MONGODB_URI = "mongodb://localhost:27017/kaiburrdb"

# Run the application
mvn spring-boot:run
```

**OR using the JAR directly:**

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:MONGODB_URI = "mongodb://localhost:27017/kaiburrdb"

java -jar target/task1-1.0.0-SNAPSHOT.jar
```

---

### Step 3B: Run in Kubernetes Mode (Production)

This mode executes commands as Kubernetes Jobs.

#### 1. Create kind Cluster

```powershell
# Create cluster
kind create cluster --name kaiburr

# Verify
kubectl cluster-info --context kind-kaiburr
```

#### 2. Build and Load Executor Image

```powershell
# Navigate to executor directory
cd executor

# Build executor image
docker build -t kaiburr-executor:dev .

# Load image into kind
kind load docker-image kaiburr-executor:dev --name kaiburr

# Return to project root
cd ..
```

#### 3. Apply Kubernetes Manifests

```powershell
# Create namespace
kubectl create namespace kaiburr

# Apply all manifests
kubectl apply -f deploy/k8s/

# Verify resources
kubectl get all -n kaiburr
kubectl get sa,role,rolebinding,networkpolicy,configmap -n kaiburr
```

#### 4. Run Application

```powershell
# Set environment variables
$env:SPRING_PROFILES_ACTIVE = "k8s"
$env:MONGODB_URI = "mongodb://localhost:27017/kaiburrdb"
$env:K8S_NAMESPACE = "kaiburr"
$env:K8S_EXECUTOR_IMAGE = "kaiburr-executor:dev"

# Run the application
mvn spring-boot:run
```

**OR using the JAR:**

```powershell
$env:SPRING_PROFILES_ACTIVE = "k8s"
$env:MONGODB_URI = "mongodb://localhost:27017/kaiburrdb"
$env:K8S_NAMESPACE = "kaiburr"
$env:K8S_EXECUTOR_IMAGE = "kaiburr-executor:dev"

java -jar target/task1-1.0.0-SNAPSHOT.jar
```

---

## ✅ Verify Application is Running

### 1. Check Health Endpoint

```powershell
curl http://localhost:8080/actuator/health | ConvertFrom-Json
```

Expected:
```json
{
  "status": "UP"
}
```

### 2. Open Swagger UI

Open in browser: http://localhost:8080/swagger-ui.html

### 3. Test API

```powershell
# Create a task
$body = @{
    id = "task-echo"
    name = "Echo Hello"
    command = "echo"
    args = @("Hello", "Kaiburr")
    assignee = "Aditya R"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" -Method PUT -Body $body -ContentType "application/json"

# Execute the task
Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/task-echo/executions" -Method PUT
```

---

## 🔍 Verify Kubernetes Execution (K8s Mode Only)

```powershell
# Check Jobs
kubectl get jobs -n kaiburr

# Check Pods
kubectl get pods -n kaiburr

# View Pod logs
kubectl logs -n kaiburr -l app=kaiburr-exec --tail=20

# Check Job details
kubectl describe job -n kaiburr <job-name>
```

---

## 🎯 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/actuator/health` | Health check |
| `GET` | `/swagger-ui.html` | Swagger UI |
| `PUT` | `/api/tasks` | Create/update task |
| `GET` | `/api/tasks` | List all tasks |
| `GET` | `/api/tasks/{id}` | Get task by ID |
| `GET` | `/api/tasks/search?name={name}` | Search tasks |
| `DELETE` | `/api/tasks/{id}` | Delete task |
| `PUT` | `/api/tasks/{id}/executions` | Execute task |
| `POST` | `/api/validation/command` | Validate command |
| `GET` | `/actuator/prometheus` | Metrics |

---

## 🛠️ Troubleshooting

### Issue: Build fails with compilation errors

**Solution:** Make sure you ran `mvn clean package` after the pom.xml was updated with Kubernetes dependencies.

```powershell
mvn clean install -U
```

### Issue: MongoDB connection refused

**Solution:** Check if MongoDB is running:

```powershell
docker ps | Select-String mongo

# If not running, start it:
docker start mongo-kaiburr

# Or create new one:
docker run -d --name mongo-kaiburr -p 27017:27017 mongo:5.0
```

### Issue: Cannot connect to Kubernetes API (K8s mode)

**Solution:** Verify kubectl context:

```powershell
kubectl config current-context
kubectl config use-context kind-kaiburr
```

### Issue: Executor image not found in kind

**Solution:** Reload the image:

```powershell
cd executor
docker build -t kaiburr-executor:dev .
kind load docker-image kaiburr-executor:dev --name kaiburr
cd ..
```

### Issue: Jobs stay in Pending status

**Solution:** Check pod events:

```powershell
kubectl get pods -n kaiburr
kubectl describe pod -n kaiburr <pod-name>
```

Common causes:
- Image not loaded into kind
- Resource limits too high
- Node not ready

---

## 🧹 Cleanup

### Stop Application

Press `Ctrl+C` in the terminal running the application

### Stop MongoDB

```powershell
docker stop mongo-kaiburr
docker rm mongo-kaiburr
```

### Delete kind Cluster

```powershell
kind delete cluster --name kaiburr
```

### Clean Build

```powershell
mvn clean
```

---

## 📊 Comparing Local vs Kubernetes Mode

| Feature | Local Mode | Kubernetes Mode |
|---------|-----------|-----------------|
| **Setup** | Simple | Requires kind cluster |
| **Execution** | Local processes | Kubernetes Jobs |
| **Security** | Process isolation | Pod security policies |
| **Resources** | Host limited | K8s resource limits |
| **Cleanup** | Automatic | TTL controller (120s) |
| **Best for** | Development | Production simulation |
| **Prerequisites** | Java, Maven, MongoDB | + Docker, kind, kubectl |

---

## 🎓 Environment Variables Reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | Yes | `k8s` | `local` or `k8s` |
| `MONGODB_URI` | Yes | `mongodb://localhost:27017/kaiburrdb` | MongoDB connection |
| `SERVER_PORT` | No | `8080` | HTTP port |
| `K8S_NAMESPACE` | K8s only | `kaiburr` | Kubernetes namespace |
| `K8S_EXECUTOR_IMAGE` | K8s only | `kaiburr-executor:dev` | Executor image |
| `K8S_TTL_SECONDS` | No | `120` | Job cleanup TTL |
| `K8S_ACTIVE_DEADLINE_SECONDS` | No | `15` | Job timeout |

---

## 📝 Example Session

```powershell
# 1. Start everything
.\run-task2.ps1
# Choose option 1 (Local mode) for simplicity

# 2. In another terminal, test the API
$task = @{
    id = "test-date"
    name = "Show Date"
    command = "date"
    args = @()
    assignee = "Aditya R"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" -Method PUT -Body $task -ContentType "application/json"

# 3. Execute the task
Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/test-date/executions" -Method PUT

# 4. View results in MongoDB
docker exec -it mongo-kaiburr mongosh kaiburrdb --eval "db.tasks.find().pretty()"
```

---

**Built with ❤️ by Aditya R for Kaiburr Assessment 2025**
