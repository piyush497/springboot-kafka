@echo off
REM Script to seed the Courier Management System database with sample data
REM This script creates approximately 400 rows of realistic data

setlocal enabledelayedexpansion

REM Database configuration
if "%DB_HOST%"=="" set DB_HOST=localhost
if "%DB_PORT%"=="" set DB_PORT=5432
if "%DB_NAME%"=="" set DB_NAME=courier_db
if "%DB_USER%"=="" set DB_USER=courier_user
if "%DB_PASSWORD%"=="" set DB_PASSWORD=courier_pass

echo === Courier Management System - Database Seeding ===
echo Database: %DB_HOST%:%DB_PORT%/%DB_NAME%
echo User: %DB_USER%
echo.

REM Check if psql is available
where psql >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: psql is not installed or not in PATH
    echo Please install PostgreSQL client tools
    pause
    exit /b 1
)

echo Checking database connectivity...
set PGPASSWORD=%DB_PASSWORD%
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "SELECT 1;" >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Cannot connect to database
    echo Please ensure PostgreSQL is running and credentials are correct
    pause
    exit /b 1
)
echo Database connection successful

REM Check for existing data
echo Checking for existing data...
for /f %%i in ('psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -t -c "SELECT COUNT(*) FROM customers;" 2^>nul') do set CUSTOMER_COUNT=%%i
for /f %%i in ('psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -t -c "SELECT COUNT(*) FROM parcels;" 2^>nul') do set PARCEL_COUNT=%%i

set CUSTOMER_COUNT=%CUSTOMER_COUNT: =%
set PARCEL_COUNT=%PARCEL_COUNT: =%

echo Current data: %CUSTOMER_COUNT% customers, %PARCEL_COUNT% parcels

if %CUSTOMER_COUNT% gtr 0 (
    echo Warning: Database already contains data
    set /p CONTINUE="Do you want to continue and add more data? (y/N): "
    if /i not "!CONTINUE!"=="y" (
        echo Seeding cancelled
        pause
        exit /b 0
    )
)

echo Starting database seeding...

REM Seed customers
echo Seeding customers...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f scripts\seed-data.sql >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ 50 customers created
) else (
    echo ✗ Failed to create customers
    pause
    exit /b 1
)

REM Seed parcels
echo Seeding parcels...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f scripts\seed-parcels.sql >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ 200 parcels created
) else (
    echo ✗ Failed to create parcels
    pause
    exit /b 1
)

REM Seed EDI orders
echo Seeding EDI orders...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f scripts\seed-edi-orders.sql >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ 100 EDI orders created
) else (
    echo ✗ Failed to create EDI orders
    pause
    exit /b 1
)

REM Seed tracking events
echo Seeding tracking events...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f scripts\seed-tracking-events.sql >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ 50 tracking events created
) else (
    echo ✗ Failed to create tracking events
    pause
    exit /b 1
)

REM Create test events for Kafka
echo Creating test events for Kafka streaming...
echo INSERT INTO parcel_tracking_events (parcel_id, event_type, status, location, description, event_timestamp, created_at) > temp_events.sql
echo SELECT p.id, 'STATUS_UPDATE', >> temp_events.sql
echo CASE WHEN p.status = 'REGISTERED' THEN 'PICKED_UP' >> temp_events.sql
echo      WHEN p.status = 'PICKED_UP' THEN 'IN_TRANSIT' >> temp_events.sql
echo      WHEN p.status = 'IN_TRANSIT' THEN 'LOADED_IN_TRUCK' >> temp_events.sql
echo      WHEN p.status = 'LOADED_IN_TRUCK' THEN 'OUT_FOR_DELIVERY' >> temp_events.sql
echo      ELSE p.status END, >> temp_events.sql
echo p.delivery_city ^|^| ', ' ^|^| p.delivery_state, >> temp_events.sql
echo 'Automated status update for testing', NOW(), NOW() >> temp_events.sql
echo FROM parcels p WHERE p.status IN ('REGISTERED', 'PICKED_UP', 'IN_TRANSIT', 'LOADED_IN_TRUCK') >> temp_events.sql
echo AND p.id ^<= 200 LIMIT 10; >> temp_events.sql

psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f temp_events.sql >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Test events created for Kafka streaming
) else (
    echo ⚠ Test events creation skipped
)
del temp_events.sql >nul 2>&1

echo.
echo === Database Seeding Completed Successfully! ===

REM Get final record counts
echo Getting final record counts...
for /f %%i in ('psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -t -c "SELECT COUNT(*) FROM customers;" 2^>nul') do set FINAL_CUSTOMERS=%%i
for /f %%i in ('psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -t -c "SELECT COUNT(*) FROM parcels;" 2^>nul') do set FINAL_PARCELS=%%i
for /f %%i in ('psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -t -c "SELECT COUNT(*) FROM edi_parcel_orders;" 2^>nul') do set FINAL_EDI=%%i
for /f %%i in ('psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -t -c "SELECT COUNT(*) FROM parcel_tracking_events;" 2^>nul') do set FINAL_EVENTS=%%i

set FINAL_CUSTOMERS=%FINAL_CUSTOMERS: =%
set FINAL_PARCELS=%FINAL_PARCELS: =%
set FINAL_EDI=%FINAL_EDI: =%
set FINAL_EVENTS=%FINAL_EVENTS: =%

set /a TOTAL=%FINAL_CUSTOMERS%+%FINAL_PARCELS%+%FINAL_EDI%+%FINAL_EVENTS%

echo Record counts:
echo   Customers: %FINAL_CUSTOMERS%
echo   Parcels: %FINAL_PARCELS%
echo   EDI Orders: %FINAL_EDI%
echo   Tracking Events: %FINAL_EVENTS%
echo   Total Records: %TOTAL%

echo.
echo Sample data preview:
echo Recent customers:
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "SELECT id, name, customer_type, city, state FROM customers ORDER BY created_at DESC LIMIT 5;"

echo.
echo Recent parcels:
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "SELECT tracking_number, status, priority, delivery_city, delivery_state FROM parcels ORDER BY created_at DESC LIMIT 5;"

echo.
echo Parcel status distribution:
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "SELECT status, COUNT(*) as count FROM parcels GROUP BY status ORDER BY count DESC;"

echo.
echo === Next Steps ===
echo 1. Start the application: gradlew.bat bootRun
echo 2. Test APIs:
echo    - GET http://localhost:8080/api/v1/customers
echo    - GET http://localhost:8080/api/v1/parcels
echo    - GET http://localhost:8080/api/v1/edi/orders
echo 3. Monitor Kafka events: http://localhost:8090 (Kafka UI)
echo 4. Test event streaming: The system will process tracking events automatically

echo.
echo Press any key to exit...
pause >nul
