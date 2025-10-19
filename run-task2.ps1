# Kaiburr Task 2 — Quick Start Script for Windows
# Author: Aditya R

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " Kaiburr Task 2 — Quick Start" -ForegroundColor Cyan
Write-Host " Author: Aditya R" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Check if MongoDB is running
Write-Host "→ Checking MongoDB..." -ForegroundColor Yellow
$mongoRunning = docker ps --format "{{.Names}}" | Select-String -Pattern "mongo-kaiburr"

if (-not $mongoRunning) {
    Write-Host "  Starting MongoDB container..." -ForegroundColor Green
    docker run -d --name mongo-kaiburr -p 27017:27017 mongo:5.0
    Start-Sleep -Seconds 3
} else {
    Write-Host "  ✓ MongoDB already running" -ForegroundColor Green
}

# Ask user which mode to run
Write-Host ""
Write-Host "Select execution mode:" -ForegroundColor Yellow
Write-Host "  1) Local mode (no Kubernetes required)" -ForegroundColor White
Write-Host "  2) Kubernetes mode (requires kind cluster)" -ForegroundColor White
$mode = Read-Host "Enter choice (1 or 2)"

if ($mode -eq "2") {
    Write-Host ""
    Write-Host "→ Setting up Kubernetes mode..." -ForegroundColor Yellow
    
    # Check if kind cluster exists
    $clusterExists = kind get clusters 2>$null | Select-String -Pattern "kaiburr"
    
    if (-not $clusterExists) {
        Write-Host "  Creating kind cluster..." -ForegroundColor Green
        kind create cluster --name kaiburr
    } else {
        Write-Host "  ✓ kind cluster already exists" -ForegroundColor Green
    }
    
    # Create namespace
    Write-Host "  Creating namespace..." -ForegroundColor Green
    kubectl create namespace kaiburr --dry-run=client -o yaml | kubectl apply -f -
    
    # Apply manifests
    Write-Host "  Applying Kubernetes manifests..." -ForegroundColor Green
    kubectl apply -f deploy/k8s/
    
    # Build and load executor image
    Write-Host "  Building executor image..." -ForegroundColor Green
    Push-Location executor
    docker build -t kaiburr-executor:dev .
    Pop-Location
    
    Write-Host "  Loading executor image into kind..." -ForegroundColor Green
    kind load docker-image kaiburr-executor:dev --name kaiburr
    
    # Set environment variables
    $env:SPRING_PROFILES_ACTIVE = "k8s"
    $env:MONGODB_URI = "mongodb://localhost:27017/kaiburrdb"
    $env:K8S_NAMESPACE = "kaiburr"
    $env:K8S_EXECUTOR_IMAGE = "kaiburr-executor:dev"
    
    Write-Host ""
    Write-Host "✓ Kubernetes mode configured" -ForegroundColor Green
    
} else {
    Write-Host ""
    Write-Host "→ Setting up Local mode..." -ForegroundColor Yellow
    
    # Set environment variables for local mode
    $env:SPRING_PROFILES_ACTIVE = "local"
    $env:MONGODB_URI = "mongodb://localhost:27017/kaiburrdb"
    
    Write-Host "✓ Local mode configured" -ForegroundColor Green
}

# Build application
Write-Host ""
Write-Host "→ Building application..." -ForegroundColor Yellow
mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Build successful" -ForegroundColor Green

# Run application
Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " Starting Kaiburr Task 2 Application" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Profile: $env:SPRING_PROFILES_ACTIVE" -ForegroundColor Yellow
Write-Host "MongoDB: $env:MONGODB_URI" -ForegroundColor Yellow
Write-Host ""
Write-Host "Press Ctrl+C to stop" -ForegroundColor Gray
Write-Host ""

mvn spring-boot:run
