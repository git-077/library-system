@echo off
title Library System - Local Deploy
cd /d D:\BackEnd\library-system

echo [1/2] Starting backend (Spring Boot :8080)...
start "library-backend" cmd /c "cd /d D:\BackEnd\library-system\backend && mvn spring-boot:run"

echo [2/2] Starting frontend (Vite :5173)...
start "library-frontend" cmd /c "cd /d D:\BackEnd\library-system\frontend && npm run dev"

echo Waiting for services (about 30s on first run)...
timeout /t 30 /nobreak >nul
start http://localhost:5173

echo.
echo Two console windows are now running:
echo   - library-backend  (close it to stop the API)
echo   - library-frontend (close it to stop the web page)
echo.
echo Login accounts:  admin/admin123  (admin)   reader/123456  (reader)
pause
