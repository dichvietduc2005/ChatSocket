@echo off
chcp 65001 >nul
echo Creating keystore for SSL/TLS...
echo.

REM Tìm keytool tự động
set KEYTOOL=
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\keytool.exe" (
        set KEYTOOL=%JAVA_HOME%\bin\keytool.exe
        goto :found
    )
)

REM Tìm trong Program Files\Java
for /d %%i in ("C:\Program Files\Java\jdk-*") do (
    if exist "%%i\bin\keytool.exe" (
        set KEYTOOL=%%i\bin\keytool.exe
        goto :found
    )
)

REM Tìm trong Program Files (x86)\Java
for /d %%i in ("C:\Program Files (x86)\Java\jdk-*") do (
    if exist "%%i\bin\keytool.exe" (
        set KEYTOOL=%%i\bin\keytool.exe
        goto :found
    )
)

REM Nếu không tìm thấy, thử dùng keytool từ PATH
where keytool >nul 2>&1
if %errorlevel% equ 0 (
    set KEYTOOL=keytool
    goto :found
)

echo ERROR: keytool not found!
echo Please install JDK or add JDK\bin to PATH
pause
exit /b 1

:found
echo Using keytool: %KEYTOOL%
echo.

set KEYSTORE_FILE=server.jks
set KEYSTORE_PASSWORD=changeme
set CERT_ALIAS=chatserver
set CERT_FILE=server.cer
set TRUSTSTORE_FILE=client-truststore.jks

echo [Step 1] Generating Server Keystore...
"%KEYTOOL%" -genkeypair ^
    -alias %CERT_ALIAS% ^
    -keyalg RSA ^
    -keysize 2048 ^
    -validity 365 ^
    -keystore %KEYSTORE_FILE% ^
    -storepass %KEYSTORE_PASSWORD% ^
    -keypass %KEYSTORE_PASSWORD% ^
    -dname "CN=ChatServer, OU=IT, O=ChatApp, L=HoChiMinh, ST=HCMC, C=VN"

if errorlevel 1 (
    echo ERROR: Failed to create keystore
    pause
    exit /b 1
)

echo Keystore created: %KEYSTORE_FILE%
echo.

echo [Step 2] Exporting Server Certificate...
"%KEYTOOL%" -exportcert ^
    -alias %CERT_ALIAS% ^
    -keystore %KEYSTORE_FILE% ^
    -storepass %KEYSTORE_PASSWORD% ^
    -file %CERT_FILE%

if errorlevel 1 (
    echo ERROR: Failed to export certificate
    pause
    exit /b 1
)

echo Certificate exported: %CERT_FILE%
echo.

echo [Step 3] Creating Client Truststore...
"%KEYTOOL%" -importcert ^
    -alias %CERT_ALIAS% ^
    -file %CERT_FILE% ^
    -keystore %TRUSTSTORE_FILE% ^
    -storepass %KEYSTORE_PASSWORD% ^
    -noprompt

if errorlevel 1 (
    echo ERROR: Failed to create truststore
    pause
    exit /b 1
)

echo Truststore created: %TRUSTSTORE_FILE%
echo.

echo ===== SUCCESS =====
echo Files created:
echo   - %KEYSTORE_FILE% (server keystore)
echo   - %TRUSTSTORE_FILE% (client truststore)
echo   - %CERT_FILE% (certificate)
echo.
echo Keystore Password: %KEYSTORE_PASSWORD%
echo Truststore Password: %KEYSTORE_PASSWORD%
echo Alias: %CERT_ALIAS%
echo.
echo Copy these files to your project root or specify path in code.
echo.
pause
