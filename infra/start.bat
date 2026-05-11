@echo off
chcp 65001 > nul
setlocal EnableDelayedExpansion

echo ====================================
echo   rental-crm 인프라 시작
echo ====================================
echo.

REM 스크립트 위치로 이동 (어디서 실행되어도 OK)
cd /d "%~dp0"

REM .env 없으면 .env.example 복사
if not exist .env (
    echo [INFO] .env 파일이 없습니다. .env.example 을 복사합니다.
    copy /Y .env.example .env > nul
    echo [OK]   .env 생성 완료. 필요시 메모장으로 수정하세요.
    echo.
)

REM Docker 데몬 확인
docker info > nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker 가 실행되지 않았습니다.
    echo         Docker Desktop 을 먼저 실행한 뒤 본 스크립트를 다시 실행하세요.
    echo.
    pause
    exit /b 1
)
echo [OK]   Docker 데몬 응답 정상.
echo.

REM 컨테이너 시작
echo [INFO] docker compose up -d 실행 중...
docker compose up -d
if errorlevel 1 (
    echo.
    echo [ERROR] 컨테이너 시작 실패. 위 로그 확인.
    pause
    exit /b 1
)

echo.
echo ====================================
echo   Oracle healthcheck 대기
echo ====================================
echo [INFO] Oracle 첫 부팅은 5~8분 소요됩니다.
echo.

set MAX_WAIT=600
set ELAPSED=0
:wait_oracle_loop
for /f "tokens=*" %%i in ('docker inspect --format "{{.State.Health.Status}}" rental-oracle 2^>nul') do set HEALTH=%%i
if "!HEALTH!"=="healthy" goto oracle_ready
if !ELAPSED! geq !MAX_WAIT! (
    echo [ERROR] Oracle healthy 도달 실패 ^(timeout 600s^)
    echo         로그 확인: docker compose logs oracle
    pause
    exit /b 1
)
echo   [!ELAPSED!s] Oracle 상태: !HEALTH!
timeout /t 20 /nobreak > nul
set /a ELAPSED=ELAPSED+20
goto wait_oracle_loop

:oracle_ready
echo [OK]   Oracle healthy. ^(경과: !ELAPSED!s^)
echo.

REM ============================================================
REM Fallback — init-scripts 자동 실행 검증 + 미실행 시 수동 복구
REM 알려진 이슈: ADR-004 참조
REM ============================================================
echo ====================================
echo   스키마 검증 ^(ADR-004 fallback^)
echo ====================================

REM RENTAL 스키마에 테이블 개수 확인
set TABLE_CNT=0
for /f "skip=2 tokens=*" %%i in ('docker exec -i rental-oracle sqlplus -s rental/rental@//localhost:1521/XEPDB1 2^>nul ^<^<EOF
SET PAGESIZE 0
SET FEEDBACK OFF
SET HEADING OFF
SELECT COUNT(*) FROM USER_TABLES;
EXIT;
EOF
') do (
    set TABLE_CNT=%%i
    goto check_done
)
:check_done

REM 공백 제거
for /f "tokens=* delims= " %%a in ("!TABLE_CNT!") do set TABLE_CNT=%%a

echo [INFO] RENTAL 스키마 테이블 개수: !TABLE_CNT!

if "!TABLE_CNT!"=="19" (
    echo [OK]   스키마 정상 ^(19개 테이블 확인^). fallback 스킵.
    goto skip_fallback
)

if "!TABLE_CNT!"=="0" (
    echo [WARN] 자동 init scripts 미실행 감지. 수동 fallback 실행합니다.
    goto run_fallback
)

echo [WARN] 테이블 개수가 예상 ^(19^) 과 다름: !TABLE_CNT!
echo        부분 실행된 상태일 수 있습니다. 수동 검증 권장.
echo        강제 재실행을 원하면 reset.bat 후 start.bat 재실행.
goto skip_fallback

:run_fallback
echo.
echo [INFO] 01-create-schema.sql 실행 중...
docker exec -i rental-oracle sqlplus -s system/oracle@//localhost:1521/XEPDB1 < init-scripts\oracle\01-create-schema.sql > nul 2>&1
if errorlevel 1 echo [WARN] 일부 객체 생성 경고 발생 ^(기존 객체 충돌 가능^). 다음 진행.

echo [INFO] 02-create-indexes.sql 실행 중...
docker exec -i rental-oracle sqlplus -s system/oracle@//localhost:1521/XEPDB1 < init-scripts\oracle\02-create-indexes.sql > nul 2>&1
if errorlevel 1 echo [WARN] 일부 인덱스 생성 경고 발생.

echo [INFO] 03-seed-codes.sql 실행 중...
docker exec -i rental-oracle sqlplus -s system/oracle@//localhost:1521/XEPDB1 < init-scripts\oracle\03-seed-codes.sql > nul 2>&1
if errorlevel 1 echo [WARN] 시드 데이터 일부 INSERT 경고 발생.

echo [OK]   Fallback 실행 완료.

:skip_fallback
echo.
echo ====================================
echo   시작 명령 완료
echo ====================================
echo   - Oracle:    localhost:1521  / XEPDB1  / rental/rental
echo   - Kafka:     localhost:9092
echo   - Kafka UI:  http://localhost:8090
echo   - Redis:     localhost:6379
echo.
echo   상태 확인:        status.bat  ^(더블클릭^)
echo   Oracle 부팅 로그: docker compose logs -f oracle
echo   정지:             stop.bat   ^(더블클릭^)
echo.
pause
endlocal
