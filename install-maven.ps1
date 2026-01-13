# Maven Installation Script for Windows
# This script downloads and installs Apache Maven automatically

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Apache Maven Installation Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if running as Administrator
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "WARNING: Not running as Administrator!" -ForegroundColor Yellow
    Write-Host "You may need to run this script as Administrator to add Maven to system PATH." -ForegroundColor Yellow
    Write-Host ""
    $continue = Read-Host "Continue anyway? (Y/N)"
    if ($continue -ne "Y" -and $continue -ne "y") {
        Write-Host "Installation cancelled." -ForegroundColor Red
        exit
    }
}

# Maven version and download URL
$mavenVersion = "3.9.6"
$mavenUrl = "https://dlcdn.apache.org/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
$mavenZip = "$env:TEMP\apache-maven-$mavenVersion-bin.zip"
$installDir = "C:\Program Files\Apache\maven"

Write-Host "[1/5] Checking Java installation..." -ForegroundColor Green
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    if ($javaVersion) {
        Write-Host "✓ Java found: $javaVersion" -ForegroundColor Green
    } else {
        Write-Host "✗ Java not found!" -ForegroundColor Red
        Write-Host "Please install Java JDK 23+ first from: https://www.oracle.com/java/technologies/downloads/" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "✗ Java not found!" -ForegroundColor Red
    Write-Host "Please install Java JDK 23+ first from: https://www.oracle.com/java/technologies/downloads/" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "[2/5] Downloading Maven $mavenVersion..." -ForegroundColor Green
Write-Host "URL: $mavenUrl" -ForegroundColor Gray

try {
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri $mavenUrl -OutFile $mavenZip -UseBasicParsing
    Write-Host "✓ Download complete" -ForegroundColor Green
} catch {
    Write-Host "✗ Download failed: $_" -ForegroundColor Red
    Write-Host "Please check your internet connection and try again." -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "[3/5] Extracting Maven..." -ForegroundColor Green

# Create installation directory
if (-not (Test-Path "C:\Program Files\Apache")) {
    New-Item -ItemType Directory -Path "C:\Program Files\Apache" -Force | Out-Null
}

# Remove old installation if exists
if (Test-Path $installDir) {
    Write-Host "Removing old installation..." -ForegroundColor Yellow
    Remove-Item -Path $installDir -Recurse -Force
}

# Extract ZIP
try {
    Expand-Archive -Path $mavenZip -DestinationPath "C:\Program Files\Apache" -Force
    # Rename extracted folder
    $extractedFolder = "C:\Program Files\Apache\apache-maven-$mavenVersion"
    if (Test-Path $extractedFolder) {
        Rename-Item -Path $extractedFolder -NewName "maven" -Force
    }
    Write-Host "✓ Extraction complete" -ForegroundColor Green
} catch {
    Write-Host "✗ Extraction failed: $_" -ForegroundColor Red
    exit 1
}

# Clean up ZIP file
Remove-Item -Path $mavenZip -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "[4/5] Setting up environment variables..." -ForegroundColor Green

$mavenBin = "$installDir\bin"

# Set MAVEN_HOME
try {
    [Environment]::SetEnvironmentVariable("MAVEN_HOME", $installDir, "Machine")
    Write-Host "✓ MAVEN_HOME set to: $installDir" -ForegroundColor Green
} catch {
    Write-Host "⚠ Could not set MAVEN_HOME (may need admin): $_" -ForegroundColor Yellow
    Write-Host "Setting for current session only..." -ForegroundColor Yellow
    $env:MAVEN_HOME = $installDir
}

# Add to PATH
try {
    $currentPath = [Environment]::GetEnvironmentVariable("Path", "Machine")
    if ($currentPath -notlike "*$mavenBin*") {
        $newPath = $currentPath + ";$mavenBin"
        [Environment]::SetEnvironmentVariable("Path", $newPath, "Machine")
        Write-Host "✓ Added Maven to system PATH" -ForegroundColor Green
    } else {
        Write-Host "✓ Maven already in PATH" -ForegroundColor Green
    }
} catch {
    Write-Host "⚠ Could not add to system PATH (may need admin): $_" -ForegroundColor Yellow
    Write-Host "Adding to current session PATH..." -ForegroundColor Yellow
    $env:Path += ";$mavenBin"
}

Write-Host ""
Write-Host "[5/5] Verifying installation..." -ForegroundColor Green

# Refresh PATH for current session
$env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")
$env:MAVEN_HOME = $installDir

# Test Maven
Start-Sleep -Seconds 2
try {
    $mvnVersion = & "$mavenBin\mvn.cmd" --version 2>&1
    if ($mvnVersion) {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "  ✓ Maven installed successfully!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "Maven Version:" -ForegroundColor Cyan
        $mvnVersion | Select-Object -First 3
        Write-Host ""
        Write-Host "Installation directory: $installDir" -ForegroundColor Gray
        Write-Host ""
        Write-Host "IMPORTANT: Please close and reopen your terminal/PowerShell" -ForegroundColor Yellow
        Write-Host "for the PATH changes to take effect." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Then test with: mvn --version" -ForegroundColor Cyan
    } else {
        throw "Maven not found"
    }
} catch {
    Write-Host ""
    Write-Host "⚠ Maven installed but not accessible in PATH yet." -ForegroundColor Yellow
    Write-Host "Please close and reopen your terminal, then run: mvn --version" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Or use full path: $mavenBin\mvn.cmd --version" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Installation complete!" -ForegroundColor Green
