# 🚀 Application Running Successfully!

**Started:** October 19, 2025 at 21:47:56  
**Port:** 8080  
**Profile:** local  
**MongoDB:** Connected to localhost:27017/kaiburrdb  
**Status:** ✅ RUNNING

---

## Application Information

```
_  __     _ _                       _____         _    __ 
| |/ /__ _(_) |__  _   _ _ __ _ __  |_   _|_ _ ___| | _/_ |
| ' // _` | | '_ \| | | | '__| '__|   | |/ _` / __| |/ /| |
| . \ (_| | | |_) | |_| | |  | |      | | (_| \__ \   < | |
|_|\_\__,_|_|_.__/ \__,_|_|  |_|      |_|\__,_|___/_|\_\|_|

Secure Task Execution Service :: Author: Aditya R.
Spring Boot 3.2.0 :: MongoDB Powered
```

- **Process ID:** 17112
- **Spring Boot:** v3.2.0
- **Java Version:** 23.0.1
- **MongoDB:** Connected successfully
- **Command Policy:** 10 allowlisted binaries, 42 denylisted commands loaded
- **Policy Watcher:** Active (hot-reload enabled)

---

## 🌐 Available Endpoints

### Main API (Port 8080)

#### Task Management
- **PUT** `http://localhost:8080/api/tasks` - Create/Update Task
- **GET** `http://localhost:8080/api/tasks` - List All Tasks (with pagination)
- **GET** `http://localhost:8080/api/tasks/{id}` - Get Task by ID
- **GET** `http://localhost:8080/api/tasks/search?name={name}` - Search Tasks by Name
- **DELETE** `http://localhost:8080/api/tasks/{id}` - Delete Task

#### Task Execution
- **PUT** `http://localhost:8080/api/tasks/{id}/executions` - Execute Task Command
- **POST** `http://localhost:8080/api/validation/command` - Validate Command (Dry-run)

#### Documentation & Monitoring
- **GET** `http://localhost:8080/swagger-ui.html` - Swagger UI (API Documentation)
- **GET** `http://localhost:8080/v3/api-docs` - OpenAPI JSON Specification
- **GET** `http://localhost:8080/actuator/health` - Health Check
- **GET** `http://localhost:8080/actuator/metrics` - Application Metrics
- **GET** `http://localhost:8080/actuator/prometheus` - Prometheus Metrics
- **GET** `http://localhost:8080/actuator/info` - Application Info

---

## 🧪 Quick Test Commands

### 1. Health Check
```bash
curl http://localhost:8080/actuator/health
```

### 2. Create a Task
```bash
curl -X PUT http://localhost:8080/api/tasks `
  -H "Content-Type: application/json" `
  -d '{
    "id": "task-001",
    "name": "List Files",
    "description": "List current directory",
    "command": "ls",
    "args": ["-la"]
  }'
```

### 3. Get All Tasks
```bash
curl http://localhost:8080/api/tasks
```

### 4. Execute a Task
```bash
curl -X PUT http://localhost:8080/api/tasks/task-001/executions
```

### 5. Validate a Command (Dry-run)
```bash
curl -X POST http://localhost:8080/api/validation/command `
  -H "Content-Type: application/json" `
  -d '{
    "command": "echo",
    "args": ["Hello World"]
  }'
```

### 6. Search Tasks
```bash
curl "http://localhost:8080/api/tasks/search?name=List"
```

---

## 🔍 Testing with PowerShell

### Using Invoke-RestMethod (PowerShell Native)

#### Health Check
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get
```

#### Create Task
```powershell
$task = @{
    id = "task-demo"
    name = "Demo Task"
    description = "Echo demo message"
    command = "echo"
    args = @("Hello", "from", "Kaiburr")
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" `
  -Method Put `
  -ContentType "application/json" `
  -Body $task
```

#### Execute Task
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/task-demo/executions" `
  -Method Put
```

---

## 📊 Access Swagger UI

Open your browser and navigate to:

**🌐 http://localhost:8080/swagger-ui.html**

This provides an interactive API documentation where you can:
- View all endpoints
- See request/response schemas
- Test API calls directly from the browser
- Download OpenAPI specification

---

## 🔐 Security Features Active

✅ **Command Validation:** Policy-as-data with 42 blocked commands  
✅ **Allowlist Enforcement:** Only 10 safe binaries allowed  
✅ **Metacharacter Blocking:** Prevents shell injection  
✅ **Path Traversal Prevention:** Blocks `../`, `//`, `~/` patterns  
✅ **Output Truncation:** Limits stdout (128KB) and stderr (64KB)  
✅ **Correlation IDs:** Request tracing enabled  
✅ **Hot-Reload Policy:** Changes to policy file apply automatically

---

## 📝 Allowed Commands (Local Profile)

The following commands are safe to execute:
- `echo` - Print text
- `date` - Display date/time
- `pwd` - Print working directory
- `ls` / `dir` - List directory contents
- `cat` / `type` - Display file contents
- `head` - Display file beginning
- `tail` - Display file end
- `wc` - Count lines/words/characters
- `env` / `printenv` - Display environment variables
- `whoami` - Display current user

---

## 🚫 Blocked Commands (Examples)

These commands are denied by policy:
- `rm`, `del`, `erase` - File deletion
- `curl`, `wget` - Network access
- `bash`, `sh`, `cmd` - Shell execution
- `sudo`, `su` - Privilege escalation
- `chmod`, `chown` - Permission changes
- And 37+ more dangerous commands...

---

## 🛠️ Testing the Security

### Try a Valid Command
```powershell
$validTask = @{
    id = "safe-task"
    name = "Safe Echo"
    command = "echo"
    args = @("This is safe!")
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" `
  -Method Put `
  -ContentType "application/json" `
  -Body $validTask
```

### Try a Blocked Command
```powershell
$blockedTask = @{
    id = "blocked-task"
    name = "Dangerous Command"
    command = "rm"
    args = @("-rf", "/")
} | ConvertTo-Json

# This will return 400 BAD REQUEST with validation error
Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" `
  -Method Put `
  -ContentType "application/json" `
  -Body $blockedTask
```

---

## 📈 Monitoring

### View Metrics
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/actuator/metrics"
```

### View Specific Metric
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/actuator/metrics/kaiburr.executions.total"
```

---

## 🗄️ MongoDB Data

Your MongoDB is running at: `mongodb://localhost:27017/kaiburrdb`

### View Tasks in MongoDB
```bash
# If you have mongosh installed
mongosh kaiburrdb --eval "db.tasks.find().pretty()"
```

Or use MongoDB Compass GUI:
- Connection String: `mongodb://localhost:27017`
- Database: `kaiburrdb`
- Collection: `tasks`

---

## 🛑 Stop the Application

To stop the application:
1. Press `Ctrl+C` in the terminal where it's running
2. Or find the process and kill it:
   ```powershell
   # Find the process
   Get-Process -Name java | Where-Object {$_.Id -eq 17112}
   
   # Stop it
   Stop-Process -Id 17112
   ```

---

## 📋 Application Logs

Logs are being displayed in real-time in your terminal with:
- **Correlation IDs** for request tracing
- **Structured JSON** format for production
- **Log Levels:** DEBUG, INFO, WARN, ERROR
- **MDC Context:** Includes task IDs, execution context

---

## 🎯 Next Steps

1. **Test the API** - Use Swagger UI or curl/PowerShell
2. **Create Tasks** - Define your command execution tasks
3. **Execute Tasks** - Run commands and view results
4. **Monitor** - Check metrics and health endpoints
5. **Validate** - Use dry-run endpoint before execution

---

## ✅ Application Status

| Component | Status | Details |
|-----------|--------|---------|
| **Web Server** | ✅ Running | Tomcat on port 8080 |
| **MongoDB** | ✅ Connected | localhost:27017 |
| **Command Validator** | ✅ Active | Policy loaded, 42 rules |
| **Policy Watcher** | ✅ Active | Hot-reload enabled |
| **Actuator** | ✅ Available | 4 endpoints exposed |
| **Swagger UI** | ✅ Available | http://localhost:8080/swagger-ui.html |

---

**🎉 Your Kaiburr Task 2 Application is Live and Ready!**

**Author:** Aditya R  
**Startup Time:** 8.5 seconds  
**Profile:** local  
**Ready for Testing!** 🚀
