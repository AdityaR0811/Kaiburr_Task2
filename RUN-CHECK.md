# RUN CHECK - Kaiburr Task 1

**Author**: Aditya R.  
**Date**: 2025-10-18

This document provides step-by-step commands to build, test, run, and validate the entire Kaiburr Task 1 application.

## Prerequisites Verification

### Check Java Version
```powershell
java -version
```
**Expected**: Java 17 or higher

### Check Maven
```powershell
mvn -version
```
**Expected**: Maven 3.8+ 

### Check Docker
```powershell
docker --version
docker ps
```
**Expected**: Docker running

## Build Process

### 1. Clean and Build
```powershell
cd D:\Kaiburr
mvn clean package
```

**Expected Output**:
- `BUILD SUCCESS`
- JAR file created: `target/task1-1.0.0-SNAPSHOT.jar`
- Tests passed (all green)

### 2. Run Tests Only
```powershell
mvn test
```

**Expected**:
- CommandValidatorTest: 20+ tests passed
- LocalCommandRunnerTest: Tests passed (on Linux/Mac)
- TaskServiceTest: All tests passed
- TaskControllerTest: All tests passed
- Integration tests with Testcontainers: Passed

## MongoDB Setup

### Start MongoDB Container
```powershell
docker run -d --name mongo -p 27017:27017 mongo:7
```

### Verify MongoDB is Running
```powershell
docker ps | findstr mongo
```

**Expected**: Container running on port 27017

### Connect to MongoDB (optional)
```powershell
docker exec -it mongo mongosh
```

```javascript
// Inside mongosh
use kaiburrdb
db.tasks.find().pretty()
```

## Application Startup

### Method 1: Using JAR
```powershell
cd D:\Kaiburr
$env:MONGODB_URI="mongodb://localhost:27017/kaiburrdb"
$env:SERVER_PORT="8080"
java -jar target/task1-1.0.0-SNAPSHOT.jar
```

### Method 2: Using Maven
```powershell
mvn spring-boot:run
```

### Method 3: Using Bootstrap Script (Linux/Mac)
```bash
chmod +x scripts/dev-bootstrap.sh
./scripts/dev-bootstrap.sh
```

## Application Verification

### Check Banner
**Look for**:
```
  _  __     _ _                       _____         _    __ 
 | |/ /__ _(_) |__  _   _ _ __ _ __  |_   _|_ _ ___| | _/_ |
 ...
 :: Secure Task Execution Service :: Author: Aditya R. ::
```

### Check Logs
**Look for**:
- `Started KaiburrTask1Application in X.XXX seconds`
- `Tomcat started on port(s): 8080`
- `MongoDB connection established`

### Health Check
```powershell
curl http://localhost:8080/actuator/health | ConvertFrom-Json
```

**Expected**:
```json
{
  "status": "UP",
  "components": {
    "mongo": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

## API Endpoint Testing

### 1. Create a Task
```powershell
curl -X PUT http://localhost:8080/api/tasks `
  -H "Content-Type: application/json" `
  -d '{
    "id": "task-001",
    "name": "Echo Hello Task",
    "owner": "Aditya R.",
    "command": "echo Hello World"
  }' | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

**Expected**: 200 OK with task JSON

### 2. Get All Tasks
```powershell
curl http://localhost:8080/api/tasks?page=0&size=10 | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

**Expected**: Paginated list of tasks

### 3. Get Task by ID
```powershell
curl http://localhost:8080/api/tasks/task-001 | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

**Expected**: Task details

### 4. Search Tasks
```powershell
curl "http://localhost:8080/api/tasks/search?name=Echo" | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

**Expected**: Array of matching tasks

### 5. Execute Task
```powershell
curl -X PUT http://localhost:8080/api/tasks/task-001/executions | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

**Expected**: Execution result with stdout: "Hello World"

### 6. Validate Command (Valid)
```powershell
curl -X POST http://localhost:8080/api/validation/command `
  -H "Content-Type: application/json" `
  -d '{"command": "echo test"}' | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

**Expected**: `"valid": true`

### 7. Validate Command (Invalid)
```powershell
curl -X POST http://localhost:8080/api/validation/command `
  -H "Content-Type: application/json" `
  -d '{"command": "rm -rf /"}' | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

**Expected**: `"valid": false` with violations listed

### 8. Delete Task
```powershell
curl -X DELETE http://localhost:8080/api/tasks/task-001 -v
```

**Expected**: 204 No Content

### 9. Try to Get Deleted Task
```powershell
curl http://localhost:8080/api/tasks/task-001
```

**Expected**: 404 Not Found

## Swagger UI Testing

### Open Swagger UI
```
http://localhost:8080/swagger-ui.html
```

**Verify**:
- All endpoints visible
- Request/response schemas shown
- "Try it out" functionality works

### Check OpenAPI Spec
```powershell
curl http://localhost:8080/api-docs
```

**Expected**: OpenAPI 3.0 JSON

## Security Validation

### Test 1: Command Injection with Semicolon
```powershell
curl -X POST http://localhost:8080/api/validation/command `
  -H "Content-Type: application/json" `
  -d '{"command": "echo hello; rm -rf /"}' | ConvertFrom-Json
```

**Expected**: `"valid": false`, violation: "contains denied metacharacter: ;"

### Test 2: Sudo Attempt
```powershell
curl -X POST http://localhost:8080/api/validation/command `
  -H "Content-Type: application/json" `
  -d '{"command": "sudo whoami"}' | ConvertFrom-Json
```

**Expected**: `"valid": false`, violation: "contains denied token: sudo"

### Test 3: Path Traversal
```powershell
curl -X POST http://localhost:8080/api/validation/command `
  -H "Content-Type: application/json" `
  -d '{"command": "../../bin/cat /etc/passwd"}' | ConvertFrom-Json
```

**Expected**: `"valid": false`, violation: "contains denied sequence: ../"

### Test 4: Pipe Metacharacter
```powershell
curl -X POST http://localhost:8080/api/validation/command `
  -H "Content-Type: application/json" `
  -d '{"command": "echo hello | grep h"}' | ConvertFrom-Json
```

**Expected**: `"valid": false`, violation: "contains denied metacharacter: |"

## Audit Log Verification

### Check Audit Log
```powershell
Get-Content audit.log.jsonl
```

**Expected**: JSON lines with:
- `timestamp`
- `correlationId`
- `taskId`
- `owner`
- `commandHash`
- `exitCode`
- `durationMs`

### Parse Audit Log
```powershell
Get-Content audit.log.jsonl | ForEach-Object { $_ | ConvertFrom-Json }
```

## MongoDB Data Verification

### Using mongosh
```powershell
docker exec -it mongo mongosh
```

```javascript
use kaiburrdb

// Count tasks
db.tasks.count()

// Find all tasks
db.tasks.find().pretty()

// Find specific task
db.tasks.findOne({_id: "task-001"})

// Check execution history
db.tasks.find({}, {name: 1, taskExecutions: 1}).pretty()

// Create index (should already exist)
db.tasks.getIndexes()
```

**Expected Indexes**:
- `_id_` (default)
- `name_1` (created by Spring Data)

### Using MongoDB Compass (GUI)
1. Open MongoDB Compass
2. Connect to: `mongodb://localhost:27017`
3. Select database: `kaiburrdb`
4. Select collection: `tasks`
5. View documents with executions

## Postman Collection Testing

### Import Collection
1. Open Postman
2. Import → File → Select `docs/postman-collection.json`
3. Collection "Kaiburr Task 1 API" should appear

### Run Collection
1. Select collection
2. Click "Run" button
3. Select all requests
4. Click "Run Kaiburr Task 1 API"

**Expected**: All requests pass (except intentional error scenarios)

### Check Pre-request Script Output
**Console should show**:
```
==================================================
Author: Aditya R.
Timestamp: 10/18/2025, 2:30:45 PM
Correlation ID: a1b2c3d4-...
==================================================
```

## Performance Testing (Optional)

### Load Test with curl loop
```powershell
1..100 | ForEach-Object {
    curl -X PUT http://localhost:8080/api/tasks/task-001/executions
    Write-Host "Execution $_"
}
```

**Monitor**:
- Response times should be consistent
- No memory leaks
- MongoDB connections stable

### Check Metrics
```powershell
curl http://localhost:8080/actuator/metrics
```

**Available metrics**:
- `jvm.memory.used`
- `http.server.requests`
- `mongodb.driver.pool.size`

## Demo Script Execution (Linux/Mac)

```bash
chmod +x scripts/demo-commands.sh
./scripts/demo-commands.sh
```

**Expected**: Sequential execution of all API calls with formatted JSON output

## Cleanup

### Stop Application
`Ctrl+C` in terminal running the application

### Stop and Remove MongoDB Container
```powershell
docker stop mongo
docker rm mongo
```

### Clean Build Artifacts
```powershell
mvn clean
```

### Remove Audit Log
```powershell
Remove-Item audit.log.jsonl
```

## Common Issues and Solutions

### Issue: Port 8080 already in use
**Solution**:
```powershell
# Find process using port 8080
netstat -ano | findstr :8080

# Kill process (replace PID)
taskkill /PID <PID> /F

# Or use different port
$env:SERVER_PORT="8081"
java -jar target/task1-1.0.0-SNAPSHOT.jar
```

### Issue: MongoDB connection refused
**Solution**:
```powershell
# Verify MongoDB is running
docker ps | findstr mongo

# Restart MongoDB
docker restart mongo

# Check logs
docker logs mongo
```

### Issue: Tests failing (Testcontainers)
**Solution**:
```powershell
# Ensure Docker is running
docker info

# Run tests with Docker Desktop running
mvn test
```

### Issue: Command validation too strict
**Solution**:
- Review `config/command-policy.yaml`
- Add binary to allowlist if needed
- Restart application to reload policy

## Final Validation Checklist

- [ ] Application builds successfully
- [ ] All tests pass
- [ ] MongoDB container running
- [ ] Application starts on port 8080
- [ ] Banner shows "Aditya R."
- [ ] Health endpoint returns UP
- [ ] Can create task
- [ ] Can retrieve task
- [ ] Can search tasks
- [ ] Can execute task with valid command
- [ ] Invalid commands are rejected
- [ ] Can delete task
- [ ] Swagger UI accessible
- [ ] Audit log created with entries
- [ ] MongoDB contains task documents
- [ ] Postman collection imports successfully
- [ ] All security validations work
- [ ] Correlation IDs appear in responses

## Success Criteria

**Application is ready for demonstration if**:

✅ All endpoints return expected responses  
✅ Security validation blocks malicious commands  
✅ Audit trail is created and readable  
✅ MongoDB data is persisted correctly  
✅ Swagger UI is functional  
✅ Postman collection runs successfully  
✅ No errors in application logs  
✅ Health check returns UP  

---

**Document Complete - Ready for Task 1 Submission**
