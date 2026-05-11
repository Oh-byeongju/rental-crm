@echo off
chcp 65001 > nul
setlocal

echo ====================================
echo   rental-crm 인프라 상태
echo ====================================
echo.

cd /d "%~dp0"

REM Docker 데몬 확인
docker info > nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker 가 실행되지 않았습니다.
    pause
    exit /b 1
)

echo [컨테이너 목록]
docker compose ps
echo.

echo [헬스 상태]
call :check_health rental-oracle    Oracle
call :check_health rental-zookeeper Zookeeper
call :check_health rental-kafka     Kafka
call :check_health rental-redis     Redis
echo.

echo [접속 정보]
echo   - Oracle:    sqlplus rental/rental@//localhost:1521/XEPDB1
echo   - Kafka UI:  http://localhost:8090
echo   - Redis CLI: docker exec -it rental-redis redis-cli
echo.

echo [Kafka 토픽 목록]
docker exec rental-kafka kafka-topics --bootstrap-server localhost:9092 --list 2>nul
if errorlevel 1 echo   ^(Kafka 미준비 — 부팅 대기 중일 수 있음^)
echo.

pause
endlocal
exit /b 0

:check_health
docker inspect --format="  %~2: {{.State.Health.Status}} ^({{.State.Status}}^)" %~1 2>nul
if errorlevel 1 echo   %~2: ^(컨테이너 없음^)
exit /b 0
