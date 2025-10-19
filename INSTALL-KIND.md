# Installing kind on Windows

**Date:** October 19, 2025  
**Platform:** Windows with PowerShell

---

## Issue
`kind` command not found when trying to run:
```powershell
kind load docker-image kaiburr-executor:dev --name kaiburr
```

---

## Solution Options

### ⭐ OPTION 1: Run in Local Mode (No Kubernetes Needed)

The simplest option if you just want to test the application:

```powershell
# 1. Start MongoDB
docker run -d --name mongo-kaiburr -p 27017:27017 mongo:5.0

# 2. Build the application
cd D:\Kaiburr_copy\Kaiburr
mvn clean package -DskipTests

# 3. Run in Local Mode (no Kubernetes)
$env:SPRING_PROFILES_ACTIVE = "local"
$env:MONGODB_URI = "mongodb://localhost:27017/kaiburrdb"
mvn spring-boot:run
```

✅ **This works immediately without installing kind!**

---

### OPTION 2: Install kind for Kubernetes Mode

If you want to test Kubernetes Job execution:

#### Method A: Manual Installation (No Admin Required)

1. **Download kind binary:**
   ```powershell
   # Create a local bin directory
   New-Item -ItemType Directory -Force -Path "$HOME\bin"
   
   # Download kind
   curl.exe -Lo "$HOME\bin\kind.exe" https://kind.sigs.k8s.io/dl/v0.20.0/kind-windows-amd64
   
   # Add to PATH for current session
   $env:Path += ";$HOME\bin"
   
   # Verify
   kind version
   ```

2. **To make it permanent**, add to your PowerShell profile:
   ```powershell
   # Open profile
   notepad $PROFILE
   
   # Add this line:
   $env:Path += ";$HOME\bin"
   
   # Save and reload
   . $PROFILE
   ```

#### Method B: Chocolatey (Requires Admin)

Open PowerShell **as Administrator** and run:
```powershell
choco install kind -y
```

#### Method C: Using winget (Requires Admin)

```powershell
winget install Kubernetes.kind
```

---

## After Installing kind

### 1. Create Cluster
```powershell
kind create cluster --name kaiburr
```

### 2. Load Executor Image
```powershell
kind load docker-image kaiburr-executor:dev --name kaiburr
```

### 3. Apply Kubernetes Manifests
```powershell
cd D:\Kaiburr_copy\Kaiburr

# Create namespace
kubectl create namespace kaiburr

# Apply all manifests
kubectl apply -f deploy/k8s/
```

### 4. Run Application in K8s Mode
```powershell
$env:SPRING_PROFILES_ACTIVE = "k8s"
$env:MONGODB_URI = "mongodb://localhost:27017/kaiburrdb"
$env:K8S_NAMESPACE = "kaiburr"
$env:K8S_EXECUTOR_IMAGE = "kaiburr-executor:dev"
mvn spring-boot:run
```

---

## Comparison: Local vs Kubernetes Mode

| Feature | Local Mode | Kubernetes Mode |
|---------|-----------|-----------------|
| **Setup Time** | 1 minute | 10-15 minutes |
| **Prerequisites** | Java, Maven, Docker, MongoDB | + kind, kubectl |
| **Installation** | None | kind installation needed |
| **Execution** | Local processes (fork/exec) | Kubernetes Jobs |
| **Use Case** | Development, testing, demos | Production simulation |
| **Admin Rights** | Not needed | Needed for kind install |

---

## Recommended Approach

### For Quick Testing/Demo:
✅ **Use Local Mode** - No kind installation needed

### For Full Production Simulation:
1. Install kind using Method A (manual, no admin)
2. Follow "After Installing kind" steps above

---

## Troubleshooting

### Issue: "kind: command not found" after manual install

**Solution:** Ensure `$HOME\bin` is in your PATH:
```powershell
$env:Path
# Should include something like C:\Users\YourName\bin
```

### Issue: Chocolatey needs admin rights

**Solution:** Either:
1. Use Method A (manual installation)
2. Right-click PowerShell → "Run as Administrator"
3. Run: `choco install kind -y`

### Issue: Docker not running

**Solution:**
```powershell
# Check Docker status
docker ps

# If not running, start Docker Desktop
```

### Issue: Port 27017 already in use

**Solution:**
```powershell
# Check if MongoDB is already running
docker ps | Select-String mongo

# Stop old container
docker stop mongo-kaiburr
docker rm mongo-kaiburr

# Start fresh
docker run -d --name mongo-kaiburr -p 27017:27017 mongo:5.0
```

---

## Quick Start Commands

### Local Mode (Recommended for Now)
```powershell
# Start MongoDB
docker run -d --name mongo-kaiburr -p 27017:27017 mongo:5.0

# Build app
cd D:\Kaiburr_copy\Kaiburr
mvn clean package -DskipTests

# Run in local mode
$env:SPRING_PROFILES_ACTIVE = "local"
$env:MONGODB_URI = "mongodb://localhost:27017/kaiburrdb"
mvn spring-boot:run

# Test
curl http://localhost:8080/actuator/health
```

### After kind is Installed
```powershell
# Install kind (choose one method above)
# Then run:
.\scripts\dev-bootstrap-kind.sh  # or follow manual steps
```

---

## Automated Script

I've also created `run-task2.ps1` that handles everything:

```powershell
cd D:\Kaiburr_copy\Kaiburr
.\run-task2.ps1
```

This script will:
1. Check MongoDB
2. Ask which mode (Local or K8s)
3. Handle setup automatically
4. Run the application

---

**Recommendation:** Start with **Local Mode** to verify everything works, then install kind if you want to test Kubernetes Job execution.
