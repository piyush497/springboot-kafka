@echo off
echo Starting Courier Management System - Local Development Environment
echo.

echo Checking if Docker is running...
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker is not running. Please start Docker Desktop first.
    pause
    exit /b 1
)

echo Docker is running. Starting infrastructure services...
echo.

echo Starting PostgreSQL, Kafka, and supporting services...
docker-compose up -d postgres zookeeper kafka kafka-ui redis

echo Waiting for services to be ready...
timeout /t 30 /nobreak >nul

echo Initializing Kafka topics...
docker-compose up kafka-init

echo.
echo Infrastructure services are starting up. Please wait a moment for all services to be ready.
echo.
echo Available services:
echo - PostgreSQL Database: localhost:5432
echo - Kafka Broker: localhost:9092
echo - Kafka UI: http://localhost:8090
echo - Redis Cache: localhost:6379
echo.
echo You can now start the Spring Boot application with the 'local' profile.
echo.
echo To stop all services, run: docker-compose down
echo To view logs, run: docker-compose logs -f [service-name]
echo.
pause
