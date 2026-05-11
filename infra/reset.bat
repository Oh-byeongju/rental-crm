@echo off
chcp 65001 > nul
setlocal

echo ====================================
echo   rental-crm 인프라 완전 초기화
echo ====================================
echo.
echo [경고] 다음 작업이 실행됩니다:
echo   - 모든 컨테이너 제거
echo   - Oracle 볼륨 삭제 ^(DB 데이터 전부 사라짐^)
echo   - Redis 볼륨 삭제 ^(캐시 데이터 사라짐^)
echo.

set /p CONFIRM="정말 실행하시려면 'YES' 를 정확히 입력하세요: "
if /i not "%CONFIRM%"=="YES" (
    echo.
    echo [취소] 'YES' 가 입력되지 않아 작업을 중단합니다.
    pause
    exit /b 0
)

cd /d "%~dp0"

echo.
echo [INFO] docker compose down -v 실행 중...
docker compose down -v

if errorlevel 1 (
    echo.
    echo [ERROR] 제거 실패. 위 로그 확인.
    pause
    exit /b 1
)

echo.
echo [OK] 완전 초기화 완료.
echo      재시작 시 init-scripts/oracle/ 의 SQL 이 다시 실행됩니다.
echo.
pause
endlocal
