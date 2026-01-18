# Maven Installation Script for Windows (Fixed)
Write-Host "--- Tự động cài đặt Apache Maven ---" -ForegroundColor Cyan

# Cấu hình sử dụng giao thức bảo mật mới nhất
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

# Dùng link Archive để đảm bảo file luôn tồn tại
$mavenVersion = "3.9.6"
$mavenUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
$mavenZip = "$env:TEMP\apache-maven-$mavenVersion-bin.zip"
$installDir = "C:\Program Files\Apache\maven"

# 1. Tải Maven
Write-Host "Đang tải Maven $mavenVersion..." -ForegroundColor Green
try {
    Invoke-WebRequest -Uri $mavenUrl -OutFile $mavenZip -UseBasicParsing
} catch {
    Write-Error "Lỗi tải file: $_"
    exit
}

# 2. Giải nén
Write-Host "Đang cài đặt..." -ForegroundColor Green
# Xóa bản cũ nếu có
if (Test-Path $installDir) { Remove-Item -Path $installDir -Recurse -Force }
# Tạo thư mục cha
if (!(Test-Path "C:\Program Files\Apache")) {
    New-Item -ItemType Directory -Path "C:\Program Files\Apache" -Force | Out-Null
}

try {
    Expand-Archive -Path $mavenZip -DestinationPath "C:\Program Files\Apache" -Force
} catch {
    Write-Error "Lỗi giải nén. File tải về có thể bị lỗi."
    exit
}

# Đổi tên thư mục giải nén thành 'maven'
$extracted = "C:\Program Files\Apache\apache-maven-$mavenVersion"
if (Test-Path $extracted) {
    Rename-Item -Path $extracted -NewName "maven" -Force
}

# 3. Cài đặt biến môi trường
$mavenBin = "$installDir\bin"
[Environment]::SetEnvironmentVariable("MAVEN_HOME", $installDir, "Machine")

# Thêm vào PATH nếu chưa có
$currentPath = [Environment]::GetEnvironmentVariable("Path", "Machine")
if ($currentPath -notlike "*$mavenBin*") {
    [Environment]::SetEnvironmentVariable("Path", $currentPath + ";$mavenBin", "Machine")
    Write-Host "Đã thêm Maven vào PATH." -ForegroundColor Green
} else {
    Write-Host "Maven đã có trong PATH." -ForegroundColor Green
}

Write-Host "Cài đặt thành công! Vui lòng TẮT và MỞ LẠI PowerShell để dùng lệnh 'mvn'." -ForegroundColor Yellow