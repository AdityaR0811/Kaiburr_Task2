# Quick Start Script for Kaiburr Task 2
# Author: Aditya R
# Date: October 19, 2025

Write-Host "🚀 Kaiburr Task 2 - Quick Start" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# Check if we're in the right directory
if (!(Test-Path "pom.xml")) {
    Write-Host "❌ Error: pom.xml not found. Please run this script from the project root." -ForegroundColor Red
    exit 1
}

# Check MongoDB
Write-Host "📊 Checking MongoDB..." -ForegroundColor Yellow
$mongoRunning = docker ps --filter "publish=27017" --format "{{.Names}}" 2>$null
if ($mongoRunning) {
    Write-Host "✅ MongoDB is running on port 27017" -ForegroundColor Green
} else {
    Write-Host "❌ MongoDB is not running on port 27017" -ForegroundColor Red
    Write-Host "   Starting MongoDB..." -ForegroundColor Yellow
    docker run -d --name mongo-kaiburr -p 27017:27017 mongo:5.0 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ MongoDB started successfully" -ForegroundColor Green
    } else {
        Write-Host "   Trying to start existing container..." -ForegroundColor Yellow
        docker start mongo 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ MongoDB started successfully" -ForegroundColor Green
        } else {
            Write-Host "❌ Could not start MongoDB. Please start it manually." -ForegroundColor Red
            exit 1
        }
    }
    Start-Sleep -Seconds 3
}
Write-Host ""

# Choose mode
Write-Host "🎯 Choose Execution Mode:" -ForegroundColor Cyan
Write-Host "  1. Local Mode (simple, no Kubernetes)" -ForegroundColor White
Write-Host "  2. Kubernetes Mode (Jobs in kind cluster)" -ForegroundColor White
Write-Host ""
$choice = Read-Host "Enter choice (1 or 2)"

if ($choice -eq "2") {
    # Kubernetes mode
    Write-Host ""
    Write-Host "🎯 Kubernetes Mode Selected" -ForegroundColor Cyan
    Write-Host ""
    
    # Check kind
    $kindInstalled = Get-Command kind -ErrorAction SilentlyContinue
    if (!$kindInstalled) {
        Write-Host "❌ kind is not installed or not in PATH" -ForegroundColor Red
        Write-Host "   Please add $HOME\bin to your PATH and restart PowerShell" -ForegroundColor Yellow
        Write-Host "   Or run: `$env:Path += `";$HOME\bin`"" -ForegroundColor Yellow
        exit 1
    }
    
    # Check cluster
    Write-Host "🔍 Checking kind cluster..." -ForegroundColor Yellow
    $clusterExists = kind get clusters 2>$null | Select-String "kaiburr"
    if (!$clusterExists) {
        Write-Host "⚠️  Cluster 'kaiburr' not found. Please create it first:" -ForegroundColor Yellow
        Write-Host "   kind create cluster --name kaiburr" -ForegroundColor White
        Write-Host "   kind load docker-image kaiburr-executor:dev --name kaiburr" -ForegroundColor White
        Write-Host "   kubectl create namespace kaiburr" -ForegroundColor White
        Write-Host "   kubectl apply -f deploy/k8s/" -ForegroundColor White
        exit 1
    }
    Write-Host "✅ kind cluster 'kaiburr' is ready" -ForegroundColor Green
    Write-Host ""
    
    # Set environment
    $env:SPRING_PROFILES_ACTIVE = "k8s"
    $env:MONGODB_URI = "mongodb://localhost:27017/kaiburrdb"
    $env:K8S_NAMESPACE = "kaiburr"
    $env:K8S_EXECUTOR_IMAGE = "kaiburr-executor:dev"
    
    Write-Host "🔧 Environment configured for Kubernetes mode:" -ForegroundColor Green
    Write-Host "   SPRING_PROFILES_ACTIVE = k8s" -ForegroundColor White
    Write-Host "   K8S_NAMESPACE = kaiburr" -ForegroundColor White
    Write-Host "   K8S_EXECUTOR_IMAGE = kaiburr-executor:dev" -ForegroundColor White
} else {
    # Local mode
    Write-Host ""
    Write-Host "🎯 Local Mode Selected" -ForegroundColor Cyan
    Write-Host ""
    
    # Set environment
    $env:SPRING_PROFILES_ACTIVE = "local"
    $env:MONGODB_URI = "mongodb://localhost:27017/kaiburrdb"
    
    Write-Host "🔧 Environment configured for Local mode:" -ForegroundColor Green
    Write-Host "   SPRING_PROFILES_ACTIVE = local" -ForegroundColor White
}

Write-Host "   MONGODB_URI = mongodb://localhost:27017/kaiburrdb" -ForegroundColor White
Write-Host ""

# Build
Write-Host "🔨 Building application..." -ForegroundColor Yellow
mvn clean package -DskipTests -q

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed. Please check the errors above." -ForegroundColor Red
    exit 1
}

Write-Host "✅ Build successful!" -ForegroundColor Green
Write-Host ""

# Run
Write-Host "🚀 Starting application..." -ForegroundColor Cyan
Write-Host "   Access Swagger UI: http://localhost:8080/swagger-ui.html" -ForegroundColor Yellow
Write-Host "   Health endpoint: http://localhost:8080/actuator/health" -ForegroundColor Yellow
Write-Host "   Prometheus metrics: http://localhost:8080/actuator/prometheus" -ForegroundColor Yellow
Write-Host ""
Write-Host "Press Ctrl+C to stop" -ForegroundColor Gray
Write-Host ""

mvn spring-boot:run
