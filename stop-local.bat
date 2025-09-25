@echo off
echo Stopping Courier Management System - Local Development Environment
echo.

echo Stopping all Docker containers...
docker-compose down

echo.
echo All services have been stopped.
echo.
echo To remove all data volumes (WARNING: This will delete all data), run:
echo docker-compose down -v
echo.
pause
