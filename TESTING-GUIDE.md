# 🎉 Kaiburr Task 2 - Testing Guide

**Date:** October 19, 2025  
**Status:** ✅ Application Running Successfully!

---

## ✅ Current Status

Your application is **LIVE** and running on:
- **URL:** http://localhost:8080
- **Health:** http://localhost:8080/actuator/health (Status: UP ✅)
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI Spec:** http://localhost:8080/v3/api-docs
- **Metrics:** http://localhost:8080/actuator/prometheus

**Components:**
- ✅ MongoDB: Connected and UP
- ✅ Disk Space: UP
- ✅ Application: Healthy

---

## 🧪 Testing Scenarios

### 1. Open Swagger UI (Easiest!)

Open in your browser:
```
http://localhost:8080/swagger-ui.html
```

You'll see the interactive API documentation where you can test all endpoints!

---

### 2. Test via PowerShell (Command Line)

#### A. Create a Task

```powershell
$task = @{
    id = "demo-echo"
    name = "Echo Demo"
    command = "echo"
    args = @("Hello", "from", "Kaiburr", "Task", "2!")
    assignee = "Aditya R"
} | ConvertTo-Json

$result = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" `
    -Method PUT `
    -Body $task `
    -ContentType "application/json"

$result | ConvertTo-Json
```

#### B. Execute the Task

```powershell
$execution = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/demo-echo/executions" `
    -Method PUT

$execution | ConvertTo-Json
```

#### C. Get Task Details

```powershell
$taskDetails = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/demo-echo"
$taskDetails | ConvertTo-Json -Depth 10
```

#### D. List All Tasks

```powershell
$allTasks = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks"
$allTasks | ConvertTo-Json
```

#### E. Search Tasks by Name

```powershell
$searchResults = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/search?name=Echo"
$searchResults | ConvertTo-Json
```

---

### 3. Test Different Commands

#### Test `date` command

```powershell
$dateTask = @{
    id = "test-date"
    name = "Current Date"
    command = "date"
    args = @()
    assignee = "Aditya R"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" -Method PUT -Body $dateTask -ContentType "application/json"
Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/test-date/executions" -Method PUT | ConvertTo-Json
```

#### Test `uname` command

```powershell
$unameTask = @{
    id = "test-uname"
    name = "System Info"
    command = "uname"
    args = @("-a")
    assignee = "Aditya R"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" -Method PUT -Body $unameTask -ContentType "application/json"
Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/test-uname/executions" -Method PUT | ConvertTo-Json
```

#### Test `whoami` command

```powershell
$whoamiTask = @{
    id = "test-whoami"
    name = "Current User"
    command = "whoami"
    args = @()
    assignee = "Aditya R"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" -Method PUT -Body $whoamiTask -ContentType "application/json"
Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/test-whoami/executions" -Method PUT | ConvertTo-Json
```

---

### 4. Test Command Validation

#### A. Validate a Safe Command

```powershell
$validation = @{
    command = "echo"
    args = @("Hello", "World")
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/validation/command" `
    -Method POST `
    -Body $validation `
    -ContentType "application/json" | ConvertTo-Json
```

**Expected:** `allowed: true`

#### B. Try a Blocked Command (should fail)

```powershell
$dangerousCmd = @{
    command = "rm"
    args = @("-rf", "/")
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/validation/command" `
    -Method POST `
    -Body $dangerousCmd `
    -ContentType "application/json" | ConvertTo-Json
```

**Expected:** `allowed: false` with reason explaining why it's blocked

#### C. Try a Command with Metacharacters (should fail)

```powershell
$metacharCmd = @{
    command = "echo"
    args = @("test", "|", "cat")
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/validation/command" `
    -Method POST `
    -Body $metacharCmd `
    -ContentType "application/json" | ConvertTo-Json
```

**Expected:** Blocked due to pipe metacharacter

---

### 5. Check Application Metrics

```powershell
# Get Prometheus metrics
curl http://localhost:8080/actuator/prometheus | Select-String "kaiburr"

# See custom metrics
curl http://localhost:8080/actuator/prometheus | Select-String "command_execution"
```

---

### 6. Test Error Handling

#### Execute Non-Existent Task

```powershell
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/nonexistent/executions" -Method PUT
} catch {
    $_.Exception.Response.StatusCode
    $_ | ConvertFrom-Json | ConvertTo-Json
}
```

**Expected:** 404 Not Found

---

## 🎯 Complete Test Workflow Script

Save this as `test-all.ps1`:

```powershell
Write-Host "🧪 Kaiburr Task 2 - Complete Test Suite" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Health Check
Write-Host "1️⃣  Testing Health Endpoint..." -ForegroundColor Yellow
$health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"
if ($health.status -eq "UP") {
    Write-Host "✅ Health Check: PASSED" -ForegroundColor Green
} else {
    Write-Host "❌ Health Check: FAILED" -ForegroundColor Red
}
Write-Host ""

# Test 2: Create Tasks
Write-Host "2️⃣  Creating Test Tasks..." -ForegroundColor Yellow

$tasks = @(
    @{ id="task-echo"; name="Echo Test"; command="echo"; args=@("Hello", "Kaiburr") },
    @{ id="task-date"; name="Date Test"; command="date"; args=@() },
    @{ id="task-whoami"; name="User Test"; command="whoami"; args=@() }
)

foreach ($task in $tasks) {
    $taskJson = $task | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" -Method PUT -Body $taskJson -ContentType "application/json" | Out-Null
        Write-Host "  ✅ Created: $($task.name)" -ForegroundColor Green
    } catch {
        Write-Host "  ❌ Failed: $($task.name)" -ForegroundColor Red
    }
}
Write-Host ""

# Test 3: Execute Tasks
Write-Host "3️⃣  Executing Tasks..." -ForegroundColor Yellow

foreach ($task in $tasks) {
    try {
        $result = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/$($task.id)/executions" -Method PUT
        Write-Host "  ✅ Executed: $($task.name) - Status: $($result.status)" -ForegroundColor Green
        Write-Host "     Output: $($result.output)" -ForegroundColor Gray
    } catch {
        Write-Host "  ❌ Failed: $($task.name)" -ForegroundColor Red
    }
}
Write-Host ""

# Test 4: List All Tasks
Write-Host "4️⃣  Listing All Tasks..." -ForegroundColor Yellow
$allTasks = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks"
Write-Host "  ✅ Found $($allTasks.Count) tasks" -ForegroundColor Green
Write-Host ""

# Test 5: Command Validation
Write-Host "5️⃣  Testing Command Validation..." -ForegroundColor Yellow

$validCmd = @{ command="echo"; args=@("safe") } | ConvertTo-Json
$validation = Invoke-RestMethod -Uri "http://localhost:8080/api/validation/command" -Method POST -Body $validCmd -ContentType "application/json"
if ($validation.allowed) {
    Write-Host "  ✅ Safe command validation: PASSED" -ForegroundColor Green
} else {
    Write-Host "  ❌ Safe command validation: FAILED" -ForegroundColor Red
}

try {
    $dangerousCmd = @{ command="rm"; args=@("-rf", "/") } | ConvertTo-Json
    $validation = Invoke-RestMethod -Uri "http://localhost:8080/api/validation/command" -Method POST -Body $dangerousCmd -ContentType "application/json"
    if (-not $validation.allowed) {
        Write-Host "  ✅ Dangerous command blocked: PASSED" -ForegroundColor Green
    } else {
        Write-Host "  ❌ Dangerous command not blocked: FAILED" -ForegroundColor Red
    }
} catch {
    Write-Host "  ✅ Dangerous command blocked: PASSED" -ForegroundColor Green
}

Write-Host ""
Write-Host "🎉 Test Suite Complete!" -ForegroundColor Cyan
Write-Host ""
Write-Host "📊 View results in Swagger UI: http://localhost:8080/swagger-ui.html" -ForegroundColor Yellow
```

---

## 🔍 Verify MongoDB Data

```powershell
# Connect to MongoDB and check stored data
docker exec -it mongo mongosh kaiburrdb --eval "db.tasks.find().pretty()"
docker exec -it mongo mongosh kaiburrdb --eval "db.task_executions.find().pretty()"
```

---

## 🐛 Troubleshooting

### Application not responding?

```powershell
# Check if it's running
Get-Process -Name java -ErrorAction SilentlyContinue

# Check port 8080
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
```

### Need to restart?

```powershell
# Stop the application (press Ctrl+C in the terminal running it)
# Or kill the process:
$javaProcess = Get-Process -Name java -ErrorAction SilentlyContinue | Where-Object { (Get-NetTCPConnection -OwningProcess $_.Id -LocalPort 8080 -ErrorAction SilentlyContinue) }
if ($javaProcess) {
    Stop-Process -Id $javaProcess.Id -Force
}

# Restart
$env:SPRING_PROFILES_ACTIVE = "local"
$env:MONGODB_URI = "mongodb://localhost:27017/kaiburrdb"
mvn spring-boot:run
```

---

## 📸 Taking Screenshots

For your documentation, capture:

1. **Swagger UI:** http://localhost:8080/swagger-ui.html
2. **Health Check:** http://localhost:8080/actuator/health
3. **Task Execution:** Show the execution result in Swagger or PowerShell
4. **MongoDB Data:** Show stored tasks and executions
5. **Metrics:** http://localhost:8080/actuator/prometheus (search for "kaiburr")

---

## 🎯 What Mode Are You Running?

Check your current mode:

```powershell
$env:SPRING_PROFILES_ACTIVE
```

- If **"local"**: Commands run as local processes (simple, fast)
- If **"k8s"**: Commands run as Kubernetes Jobs (production-like)

To check if running in K8s mode:

```powershell
# Check for Jobs
kubectl get jobs -n kaiburr

# Check for Pods
kubectl get pods -n kaiburr

# View logs
kubectl logs -n kaiburr -l app=kaiburr-exec --tail=50
```

---

## ✅ Success Checklist

- [x] Application built successfully
- [x] MongoDB connected
- [x] Application running on port 8080
- [x] Health check returns UP
- [ ] Tested creating tasks via Swagger/PowerShell
- [ ] Tested executing tasks
- [ ] Verified command validation works
- [ ] Checked MongoDB for stored data
- [ ] Took screenshots for documentation
- [ ] (Optional) Tested Kubernetes mode with Jobs

---

**You're ready! Start testing with Swagger UI or run the test script above! 🚀**

Open: http://localhost:8080/swagger-ui.html
