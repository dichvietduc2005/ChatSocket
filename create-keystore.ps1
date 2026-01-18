# create-keystore.ps1 - Auto-detect keytool and create SSL keystore
Write-Host "Creating keystore for SSL/TLS..." -ForegroundColor Cyan
Write-Host ""

# Tìm keytool
$keytool = $null

# Thử JAVA_HOME trước
if ($env:JAVA_HOME) {
    $keytoolPath = Join-Path $env:JAVA_HOME "bin\keytool.exe"
    if (Test-Path $keytoolPath) {
        $keytool = $keytoolPath
        Write-Host "Using JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Green
    }
}

# Nếu không tìm thấy, tìm trong Program Files
if (-not $keytool) {
    $jdkDirs = Get-ChildItem "C:\Program Files\Java\" -Directory -ErrorAction SilentlyContinue
    foreach ($dir in $jdkDirs) {
        $keytoolPath = Join-Path $dir.FullName "bin\keytool.exe"
        if (Test-Path $keytoolPath) {
            $keytool = $keytoolPath
            Write-Host "Found JDK: $($dir.FullName)" -ForegroundColor Green
            break
        }
    }
}

# Nếu vẫn không tìm thấy, thử Program Files (x86)
if (-not $keytool) {
    $jdkDirs = Get-ChildItem "C:\Program Files (x86)\Java\" -Directory -ErrorAction SilentlyContinue
    foreach ($dir in $jdkDirs) {
        $keytoolPath = Join-Path $dir.FullName "bin\keytool.exe"
        if (Test-Path $keytoolPath) {
            $keytool = $keytoolPath
            Write-Host "Found JDK: $($dir.FullName)" -ForegroundColor Green
            break
        }
    }
}

if (-not $keytool -or -not (Test-Path $keytool)) {
    Write-Host "ERROR: keytool not found!" -ForegroundColor Red
    Write-Host "Please install JDK or add JDK\bin to PATH" -ForegroundColor Yellow
    exit 1
}

Write-Host "Using keytool: $keytool" -ForegroundColor Green
Write-Host ""

$KEYSTORE_FILE = "server.jks"
$KEYSTORE_PASSWORD = "changeme"
$CERT_ALIAS = "chatserver"
$CERT_FILE = "server.cer"
$TRUSTSTORE_FILE = "client-truststore.jks"

# Step 1: Generate keystore
Write-Host "[Step 1] Generating Server Keystore..." -ForegroundColor Yellow
& $keytool -genkeypair `
    -alias $CERT_ALIAS `
    -keyalg RSA `
    -keysize 2048 `
    -validity 365 `
    -keystore $KEYSTORE_FILE `
    -storepass $KEYSTORE_PASSWORD `
    -keypass $KEYSTORE_PASSWORD `
    -dname "CN=ChatServer, OU=IT, O=ChatApp, L=HoChiMinh, ST=HCMC, C=VN"

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to create keystore" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Keystore created: $KEYSTORE_FILE" -ForegroundColor Green
Write-Host ""

# Step 2: Export certificate
Write-Host "[Step 2] Exporting Server Certificate..." -ForegroundColor Yellow
& $keytool -exportcert `
    -alias $CERT_ALIAS `
    -keystore $KEYSTORE_FILE `
    -storepass $KEYSTORE_PASSWORD `
    -file $CERT_FILE

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to export certificate" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Certificate exported: $CERT_FILE" -ForegroundColor Green
Write-Host ""

# Step 3: Create truststore
Write-Host "[Step 3] Creating Client Truststore..." -ForegroundColor Yellow
& $keytool -importcert `
    -alias $CERT_ALIAS `
    -file $CERT_FILE `
    -keystore $TRUSTSTORE_FILE `
    -storepass $KEYSTORE_PASSWORD `
    -noprompt

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to create truststore" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Truststore created: $TRUSTSTORE_FILE" -ForegroundColor Green
Write-Host ""

Write-Host "===== SUCCESS =====" -ForegroundColor Green
Write-Host "Files created:"
Write-Host "  - $KEYSTORE_FILE (server keystore)"
Write-Host "  - $TRUSTSTORE_FILE (client truststore)"
Write-Host "  - $CERT_FILE (certificate)"
Write-Host ""
Write-Host "Keystore Password: $KEYSTORE_PASSWORD"
Write-Host "Truststore Password: $KEYSTORE_PASSWORD"
Write-Host "Alias: $CERT_ALIAS"
Write-Host ""
