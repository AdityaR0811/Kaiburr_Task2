# Test Commands for Kaiburr Task 2
# This script demonstrates working with allowed vs blocked commands

Write-Host "=== Kaiburr Task 2 - API Testing ===" -ForegroundColor Cyan
Write-Host ""

# 1. Health Check
Write-Host "1. Testing Health Endpoint..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get
    Write-Host "✅ Health Status: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "❌ Health check failed: $_" -ForegroundColor Red
}
Write-Host ""

# 2. Create Task with ALLOWED Command (echo)
Write-Host "2. Creating Task with ALLOWED command (echo)..." -ForegroundColor Yellow
$allowedTask = @{
    id = "task-echo-test"
    name = "Echo Test"
    description = "Test with allowed echo command"
    command = "echo"
    args = @("Hello", "from", "Kaiburr", "Task2!")
} | ConvertTo-Json

try {
    $result = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" `
        -Method Put `
        -ContentType "application/json" `
        -Body $allowedTask
    Write-Host "✅ Task created successfully!" -ForegroundColor Green
    Write-Host "   Task ID: $($result.id)" -ForegroundColor Cyan
    Write-Host "   Command: $($result.command)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Failed to create task: $_" -ForegroundColor Red
}
Write-Host ""

# 3. Execute the Allowed Task
Write-Host "3. Executing ALLOWED task (echo)..." -ForegroundColor Yellow
try {
    $execution = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/task-echo-test/executions" `
        -Method Put
    Write-Host "✅ Task executed successfully!" -ForegroundColor Green
    Write-Host "   Exit Code: $($execution.exitCode)" -ForegroundColor Cyan
    Write-Host "   Output: $($execution.stdout)" -ForegroundColor Cyan
    Write-Host "   Duration: $($execution.durationMs)ms" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Failed to execute task: $_" -ForegroundColor Red
}
Write-Host ""

# 4. Try BLOCKED Command (should fail validation)
Write-Host "4. Testing BLOCKED command (curl) - Should fail..." -ForegroundColor Yellow
$blockedTask = @{
    id = "task-blocked"
    name = "Blocked Command"
    description = "This should be rejected"
    command = "curl"
    args = @("http://example.com")
} | ConvertTo-Json

try {
    $result = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" `
        -Method Put `
        -ContentType "application/json" `
        -Body $blockedTask
    Write-Host "❌ Task was created (should have been blocked!)" -ForegroundColor Red
} catch {
    Write-Host "✅ Task correctly BLOCKED by validation!" -ForegroundColor Green
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Cyan
}
Write-Host ""

# 5. Validate Command (Dry-run)
Write-Host "5. Testing Validation Endpoint (dry-run)..." -ForegroundColor Yellow

# Valid command
Write-Host "   Testing valid command (pwd)..." -ForegroundColor Cyan
$validCommand = @{
    command = "pwd"
    args = @()
} | ConvertTo-Json

try {
    $validation = Invoke-RestMethod -Uri "http://localhost:8080/api/validation/command" `
        -Method Post `
        -ContentType "application/json" `
        -Body $validCommand
    Write-Host "   ✅ pwd is valid: $($validation.valid)" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Validation failed: $_" -ForegroundColor Red
}

# Invalid command
Write-Host "   Testing invalid command (rm)..." -ForegroundColor Cyan
$invalidCommand = @{
    command = "rm"
    args = @("-rf", "/")
} | ConvertTo-Json

try {
    $validation = Invoke-RestMethod -Uri "http://localhost:8080/api/validation/command" `
        -Method Post `
        -ContentType "application/json" `
        -Body $invalidCommand
    Write-Host "   ❌ rm was marked valid (should be blocked!)" -ForegroundColor Red
} catch {
    Write-Host "   ✅ rm is correctly BLOCKED!" -ForegroundColor Green
}
Write-Host ""

# 6. List All Tasks
Write-Host "6. Listing all tasks..." -ForegroundColor Yellow
try {
    $tasks = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" -Method Get
    Write-Host "✅ Found $($tasks.Count) task(s)" -ForegroundColor Green
    foreach ($task in $tasks) {
        Write-Host "   - $($task.id): $($task.name) (command: $($task.command))" -ForegroundColor Cyan
    }
} catch {
    Write-Host "❌ Failed to list tasks: $_" -ForegroundColor Red
}
Write-Host ""

# 7. Create and Execute Multiple Allowed Commands
Write-Host "7. Testing multiple allowed commands..." -ForegroundColor Yellow

$allowedCommands = @(
    @{ id = "task-date"; name = "Date Test"; command = "date"; args = @() }
    @{ id = "task-pwd"; name = "PWD Test"; command = "pwd"; args = @() }
    @{ id = "task-whoami"; name = "Whoami Test"; command = "whoami"; args = @() }
)

foreach ($cmd in $allowedCommands) {
    Write-Host "   Creating task: $($cmd.name)..." -ForegroundColor Cyan
    $taskJson = $cmd | ConvertTo-Json
    try {
        $task = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" `
            -Method Put `
            -ContentType "application/json" `
            -Body $taskJson
        Write-Host "   ✅ Created: $($task.id)" -ForegroundColor Green
        
        # Execute it
        $exec = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/$($task.id)/executions" `
            -Method Put
        Write-Host "   ✅ Executed: Exit=$($exec.exitCode), Duration=$($exec.durationMs)ms" -ForegroundColor Green
        if ($exec.stdout) {
            Write-Host "      Output: $($exec.stdout.Substring(0, [Math]::Min(50, $exec.stdout.Length)))..." -ForegroundColor Gray
        }
    } catch {
        Write-Host "   ❌ Failed: $_" -ForegroundColor Red
    }
}
Write-Host ""

# 8. Search Tasks
Write-Host "8. Searching tasks by name..." -ForegroundColor Yellow
try {
    $searchResults = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/search?name=Test" -Method Get
    Write-Host "✅ Found $($searchResults.Count) task(s) matching 'Test'" -ForegroundColor Green
} catch {
    Write-Host "❌ Search failed: $_" -ForegroundColor Red
}
Write-Host ""

# 9. Get Metrics
Write-Host "9. Checking application metrics..." -ForegroundColor Yellow
try {
    $metrics = Invoke-RestMethod -Uri "http://localhost:8080/actuator/metrics" -Method Get
    Write-Host "✅ Available metrics: $($metrics.names.Count)" -ForegroundColor Green
    
    # Try to get execution metrics
    try {
        $execMetrics = Invoke-RestMethod -Uri "http://localhost:8080/actuator/metrics/kaiburr.executions.total" -Method Get
        Write-Host "   Total executions: $($execMetrics.measurements[0].value)" -ForegroundColor Cyan
    } catch {
        Write-Host "   No execution metrics yet" -ForegroundColor Gray
    }
} catch {
    Write-Host "❌ Failed to get metrics: $_" -ForegroundColor Red
}
Write-Host ""

Write-Host "=== Testing Complete ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Summary:" -ForegroundColor Yellow
Write-Host "✅ Allowed commands (echo, date, pwd, whoami) should work" -ForegroundColor Green
Write-Host "❌ Blocked commands (curl, rm, sudo, etc.) should be rejected" -ForegroundColor Red
Write-Host ""
Write-Host "Check Swagger UI for more: http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
