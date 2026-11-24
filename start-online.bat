@echo off
rem Library System - start backend on 8081 and open public tunnel
cd /d D:\BackEnd\library-system

if not exist "D:\BackEnd\library-system\logs" mkdir "D:\BackEnd\library-system\logs"

echo [1/3] cleaning port 8081 ...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8081" ^| findstr "LISTENING"') do taskkill /f /pid %%p >nul 2>&1

echo [2/3] starting backend on 8081 ...
start "library-backend" /min cmd /c "set SERVER_PORT=8081 && cd /d D:\BackEnd\library-system\backend && java -jar target\library-backend-1.0.0.jar >> D:\BackEnd\library-system\logs\backend.log 2>&1"

ping -n 26 127.0.0.1 >nul

echo [3/3] opening tunnel to http://localhost:8081
echo Wait for a line like https://xxxx.trycloudflare.com
echo The URL is also saved to logs\tunnel.log
powershell -NoProfile -Command "& 'D:\BackEnd\library-system\scripts\cloudflared.exe' tunnel --url http://localhost:8081 2>&1 | Tee-Object -FilePath 'D:\BackEnd\library-system\logs\tunnel.log'"
pause
