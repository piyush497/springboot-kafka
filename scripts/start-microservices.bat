@echo off
echo ========================================
echo Starting Courier Management Microservices
echo ========================================

echo.
echo Checking Docker and Docker Compose...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker is not installed or not running
    pause
    exit /b 1
)

docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker Compose is not installed
    pause
    exit /b 1
)

echo.
echo Creating logs directory...
if not exist "logs" mkdir logs

echo.
echo Stopping any existing containers...
docker-compose -f docker-compose.microservices.yml down

echo.
echo Building and starting microservices...
docker-compose -f docker-compose.microservices.yml up --build -d

echo.
echo Waiting for services to start...
timeout /t 30 /nobreak >nul

echo.
echo ========================================
echo Service Status Check
echo ========================================

echo.
echo Checking Infrastructure Services...
echo - Zookeeper: 
docker-compose -f docker-compose.microservices.yml ps zookeeper
echo - Kafka: 
docker-compose -f docker-compose.microservices.yml ps kafka
echo - PostgreSQL: 
docker-compose -f docker-compose.microservices.yml ps postgres
echo - Redis: 
docker-compose -f docker-compose.microservices.yml ps redis

echo.
echo Checking Backend Services...
echo - Main Service: 
docker-compose -f docker-compose.microservices.yml ps courier-main-service
echo - Customer Interface: 
docker-compose -f docker-compose.microservices.yml ps courier-customer-interface

echo.
echo Checking Frontend Service...
echo - Frontend: 
docker-compose -f docker-compose.microservices.yml ps courier-frontend

echo.
echo ========================================
echo Service URLs
echo ========================================
echo Frontend Application:     http://localhost:3000
echo Main API Service:         http://localhost:8080
echo Customer API Service:     http://localhost:8081
echo API Gateway:              http://localhost:80
echo Kafka UI:                 http://localhost:8090
echo PostgreSQL:               localhost:5432
echo Redis:                    localhost:6379
echo.

echo ========================================
echo Health Check
echo ========================================
echo.
echo Waiting for services to be ready...
timeout /t 60 /nobreak >nul

echo Checking service health...
echo.

echo Frontend Health:
curl -s http://localhost:3000/health || echo "Frontend not ready"
echo.

echo Main Service Health:
curl -s http://localhost:8080/actuator/health || echo "Main Service not ready"
echo.

echo Customer Interface Health:
curl -s http://localhost:8081/actuator/health || echo "Customer Interface not ready"
echo.

echo ========================================
echo Microservices Started Successfully!
echo ========================================
echo.
echo You can now:
echo 1. Access the web application at: http://localhost:3000
echo 2. Use the API gateway at: http://localhost:80
echo 3. Monitor Kafka topics at: http://localhost:8090
echo 4. View logs with: docker-compose -f docker-compose.microservices.yml logs -f [service-name]
echo 5. Stop services with: scripts\stop-microservices.bat
echo.

echo Press any key to exit...
pause >nul
