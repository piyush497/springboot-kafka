@echo off
echo ========================================
echo Stopping Courier Management Microservices
echo ========================================

echo.
echo Stopping all services...
docker-compose -f docker-compose.microservices.yml down

echo.
echo Removing unused containers and networks...
docker system prune -f

echo.
echo ========================================
echo Microservices Stopped Successfully!
echo ========================================

echo.
echo All services have been stopped and cleaned up.
echo To start services again, run: scripts\start-microservices.bat
echo.

pause
