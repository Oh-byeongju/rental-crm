@echo off
chcp 65001 > nul
setlocal

echo ====================================
echo   rental-crm 인프라 정지
echo ====================================
echo.

cd /d "%~dp0"

docker compose stop

if errorlevel 1 (
    echo.
    echo [ERROR] 정지 실패. 위 로그 확인.
    pause
    exit /b 1
)

echo.
echo [OK] 정지 완료. 데이터는 유지됩니다.
echo.
echo   재시작:        start.bat
echo   컨테이너 제거: docker compose down       ^(볼륨 유지^)
echo   완전 초기화:   reset.bat                 ^(데이터 삭제, 주의^)
echo.
pause
endlocal
