@echo off
echo ===== run run.bat =====

REM 進入 src 資料夾
cd src

echo [編譯中...]
javac -encoding UTF-8 -cp "..\lib\mysql-connector-j-9.2.0.jar;." -d ..\bin Main.java

if %errorlevel% neq 0 (
    echo 編譯失敗，請檢查錯誤訊息。
    pause
    exit /b
)

echo [running...]
java -cp "..\lib\mysql-connector-j-9.2.0.jar;..\bin" Main

echo ===== run over =====
pause
