# =============================================================================
# Script dong goi All-In-One Demo: React Portal + Spring Boot Backend -> 1 file JAR
# =============================================================================
$ErrorActionPreference = "Stop"
$RootDir = $PSScriptRoot

# Xac dinh thu muc Portal va Backend (ho tro ca los-portal/los-backend va app/los)
$PortalDir = Join-Path $RootDir "los-portal"
if (-not (Test-Path $PortalDir)) { $PortalDir = Join-Path $RootDir "app" }

$BackendDir = Join-Path $RootDir "los-backend"
if (-not (Test-Path $BackendDir)) { $BackendDir = Join-Path $RootDir "los" }

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  DONG GOI ALL-IN-ONE (PORTAL FRONTEND + BACKEND SERVICE)  " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Thư mục Portal : $PortalDir" -ForegroundColor Gray
Write-Host "Thư mục Backend: $BackendDir`n" -ForegroundColor Gray

# -------------------------------------------------------------
# 0. Kiem tra moi truong
# -------------------------------------------------------------
Write-Host "[0/4] Kiem tra moi truong build..." -ForegroundColor Yellow

# Kiem tra Node.js & npm
if (-not (Get-Command node -ErrorAction SilentlyContinue) -or -not (Get-Command npm -ErrorAction SilentlyContinue)) {
    Write-Host "[LOI] Khong tim thay Node.js hoac npm tren he thong!" -ForegroundColor Red
    Write-Host "Vui long cai dat Node.js tai https://nodejs.org/" -ForegroundColor Yellow
    Exit 1
}

# Kiem tra Java
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "[LOI] Khong tim thay Java tren he thong!" -ForegroundColor Red
    Write-Host "Vui long cai dat JDK 21 tai https://adoptium.net/" -ForegroundColor Yellow
    Exit 1
}
Write-Host "Moi truong Node.js va Java hop le.`n" -ForegroundColor Green

# -------------------------------------------------------------
# 1. Build React Portal
# -------------------------------------------------------------
Write-Host "[1/4] Dang chuan bi va build React Portal (los-portal)..." -ForegroundColor Yellow
Set-Location $PortalDir

# Kiem tra file .env cua Portal
$portalEnv = Join-Path $PortalDir ".env"
$portalEnvExample = Join-Path $PortalDir ".env.example"
if (-not (Test-Path $portalEnv)) {
    if (Test-Path $portalEnvExample) {
        Copy-Item $portalEnvExample $portalEnv
        Write-Host "Da khoi tao file .env cho Portal tu .env.example" -ForegroundColor Green
    }
}

# Kiem tra node_modules
if (-not (Test-Path (Join-Path $PortalDir "node_modules"))) {
    Write-Host "Chua co thu vien node_modules, dang chay npm install..." -ForegroundColor Yellow
    npm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[LOI] Cai dat npm dependencies cho Portal that bai!" -ForegroundColor Red
        Exit 1
    }
}

# Chay build Portal
Write-Host "Dang build bundle React bang Vite..." -ForegroundColor Yellow
npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "[LOI] Build React Portal that bai!" -ForegroundColor Red
    Exit 1
}
Write-Host "Build React Portal thanh cong.`n" -ForegroundColor Green

# -------------------------------------------------------------
# 2. Dong bo Static Resources sang Backend
# -------------------------------------------------------------
Write-Host "[2/4] Dang dong bo file giao dien sang Backend..." -ForegroundColor Yellow
$staticDir = Join-Path $BackendDir "src\main\resources\static"

if (Test-Path $staticDir) {
    Remove-Item "$staticDir\*" -Recurse -Force -ErrorAction SilentlyContinue
} else {
    New-Item -ItemType Directory -Path $staticDir -Force | Out-Null
}

$portalDistHtml = Join-Path $PortalDir "dist\index.html"
$portalDistAssets = Join-Path $PortalDir "dist\assets"

if (-not (Test-Path $portalDistHtml)) {
    Write-Host "[LOI] Khong tim thay $portalDistHtml sau khi build!" -ForegroundColor Red
    Exit 1
}

Copy-Item -Path $portalDistHtml -Destination "$staticDir\" -Force
if (Test-Path $portalDistAssets) {
    Copy-Item -Path $portalDistAssets -Destination "$staticDir\" -Recurse -Force
}
Write-Host "Da sao chep index.html va assets vao Spring Boot static resources.`n" -ForegroundColor Green

# -------------------------------------------------------------
# 3. Kiem tra cau hinh va Build Spring Boot Backend
# -------------------------------------------------------------
Write-Host "[3/4] Dang dong goi file JAR Backend bang Maven..." -ForegroundColor Yellow
Set-Location $BackendDir

# Kiem tra file .env cua Backend
$backendEnv = Join-Path $BackendDir ".env"
$backendEnvExample = Join-Path $BackendDir ".env.example"
if (-not (Test-Path $backendEnv)) {
    if (Test-Path $backendEnvExample) {
        Copy-Item $backendEnvExample $backendEnv
        Write-Host "Da khoi tao file .env cho Backend tu .env.example" -ForegroundColor Green
        Write-Host "Luu y: Kiem tra cau hinh DB_PASSWORD trong file .env neu MySQL dung mat khau khac." -ForegroundColor Magenta
    }
}

# Goi Maven Wrapper de dong goi JAR
.\mvnw.cmd clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "[LOI] Dong goi file JAR that bai!" -ForegroundColor Red
    Exit 1
}

# -------------------------------------------------------------
# 4. Kiem tra ket qua
# -------------------------------------------------------------
$targetJar = Join-Path $BackendDir "target\los-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $targetJar)) {
    Write-Host "[LOI] Khong tim thay file JAR sau khi build tai: $targetJar" -ForegroundColor Red
    Exit 1
}

$jarSizeMB = [math]::Round(((Get-Item $targetJar).Length / 1MB), 2)

Set-Location $RootDir
Write-Host "`n============================================================" -ForegroundColor Green
Write-Host "  DONG GOI THANH CONG CA PORTAL VA BACKEND!                " -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "File JAR trọn gói: $targetJar ($jarSizeMB MB)" -ForegroundColor White
Write-Host "Cách khởi chạy:" -ForegroundColor Cyan
Write-Host "  1. Chạy file JAR đã gói (Khuyên dùng): .\run-demo.ps1" -ForegroundColor Yellow
Write-Host "  2. Hoặc chạy chế độ Dev song song    : .\run-demo.ps1 -Dev" -ForegroundColor Yellow
Write-Host "============================================================`n" -ForegroundColor Green
