@echo off
title Library System - Public Tunnel
cd /d D:\BackEnd\library-system

echo [1/2] Starting backend + frontend (minimized windows)...
start "library-backend" /min cmd /c "cd /d D:\BackEnd\library-system\backend && mvn spring-boot:run"
start "library-frontend" /min cmd /c "cd /d D:\BackEnd\library-system\frontend && npm run dev"

echo [2/2] Waiting 30s for services, then opening public tunnel...
timeout /t 30 /nobreak >nul

echo.
echo ============================================================
echo  PUBLIC URL will be printed below, looks like:
echo  https://xxxx-xxxx-xxxx-xxxx.trycloudflare.com
echo  Copy it and send to anyone. Keep THIS window open.
echo  Closing this window = stopping public access.
echo ============================================================
echo.
scripts\cloudflared.exe tunnel --url http://localhost:5173
pause
