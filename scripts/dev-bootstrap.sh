#!/bin/bash

# Kaiburr Task 1 - Development Bootstrap Script
# Author: Aditya R.
# This script sets up the complete development environment

set -e

echo "========================================="
echo "  Kaiburr Task 1 - Dev Bootstrap"
echo "  Author: Aditya R."
echo "========================================="
echo ""

# Check Java version
echo "Checking Java version..."
if ! command -v java &> /dev/null; then
    echo "ERROR: Java not found. Please install Java 17 or higher."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "ERROR: Java 17 or higher required. Found Java $JAVA_VERSION"
    exit 1
fi

echo "✓ Java $JAVA_VERSION detected"
echo ""

# Check Docker
echo "Checking Docker..."
if ! command -v docker &> /dev/null; then
    echo "ERROR: Docker not found. Please install Docker."
    exit 1
fi

echo "✓ Docker detected"
echo ""

# Check if MongoDB container is running
echo "Checking MongoDB..."
if docker ps --format '{{.Names}}' | grep -q '^mongo$'; then
    echo "✓ MongoDB container already running"
else
    echo "Starting MongoDB container..."
    docker run -d \
        --name mongo \
        -p 27017:27017 \
        mongo:7 \
        --quiet
    
    echo "Waiting for MongoDB to be ready..."
    sleep 5
    echo "✓ MongoDB started"
fi

echo ""

# Set environment variables
export MONGODB_URI=mongodb://localhost:27017/kaiburrdb
export SERVER_PORT=8080

echo "Environment configured:"
echo "  MONGODB_URI=$MONGODB_URI"
echo "  SERVER_PORT=$SERVER_PORT"
echo ""

# Build application
echo "Building application..."
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven not found. Please install Maven 3.8+"
    exit 1
fi

mvn clean package -DskipTests

echo "✓ Build completed"
echo ""

# Run application
echo "Starting application..."
echo "========================================="
echo ""

java -jar target/task1-1.0.0-SNAPSHOT.jar

# Note: This line will only execute if the application exits
echo ""
echo "Application stopped."
