# =============================================================================
# Script khoi chay Loan Origination System (LOS) - Ca Portal & Backend
# =============================================================================
[CmdletBinding()]
param(
    [Alias("m")]
    [ValidateSet("all-in-one", "dev", "1", "2")]
    [string]$Mode = "all-in-one",

    [Alias("b")]
    [switch]$Build,

    [switch]$Dev
)

$ErrorActionPreference = "Stop"
$RootDir = $PSScriptRoot

if ($Dev) {
    $Mode = "dev"
}

# Xac dinh thu muc Portal va Backend
$PortalDir = Join-Path $RootDir "los-portal"
if (-not (Test-Path $PortalDir)) { $PortalDir = Join-Path $RootDir "app" }

$BackendDir = Join-Path $RootDir "los-backend"
if (-not (Test-Path $BackendDir)) { $BackendDir = Join-Path $RootDir "los" }

$envFile = Join-Path $BackendDir ".env"
$exampleEnv = Join-Path $BackendDir ".env.example"
$jarFile = Join-Path $BackendDir "target\los-0.0.1-SNAPSHOT.jar"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "     KHOI CHAY LOAN ORIGINATION SYSTEM (PORTAL + BACKEND)   " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# -------------------------------------------------------------
# 1. Nap bien moi truong tu .env cua Backend
# -------------------------------------------------------------
Write-Host "[1/3] Dang nap bien moi truong he thong..." -ForegroundColor Yellow
if (-not (Test-Path $envFile)) {
    if (Test-Path $exampleEnv) {
        Write-Host "Chua co file .env, dang tao tu .env.example..." -ForegroundColor Yellow
        Copy-Item $exampleEnv $envFile
    } else {
        Write-Host "[CANH BAO] Khong tim thay file .env hoac .env.example!" -ForegroundColor Red
    }
}

if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $name = $parts[0].Trim()
            $value = $parts[1].Trim()
            [System.Environment]::SetEnvironmentVariable($name, $value, [System.EnvironmentVariableTarget]::Process)
        }
    }
    Write-Host "Da nap cau hinh Database, JWT & Gmail thanh cong." -ForegroundColor Green
}

# -------------------------------------------------------------
# 2. Xu ly tuy chon Build truoc khi chay
# -------------------------------------------------------------
if ($Build -or (-not (Test-Path $jarFile) -and ($Mode -eq "all-in-one" -or $Mode -eq "1"))) {
    Write-Host "`n[2/3] Dang thuc hien dong goi truoc khi khoi chay..." -ForegroundColor Yellow
    & (Join-Path $RootDir "build-demo.ps1")
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[LOI] Dong goi he thong that bai!" -ForegroundColor Red
        Exit 1
    }
} else {
    Write-Host "[2/3] File JAR da san sang: $jarFile" -ForegroundColor Green
}

# -------------------------------------------------------------
# 3. Khoi chay he thong theo che do
# -------------------------------------------------------------

# CHE DO 1: ALL-IN-ONE JAR (Gom ca Portal UI va Backend API tren port 8080)
if ($Mode -eq "all-in-one" -or $Mode -eq "1") {
    Write-Host "`n[3/3] Dang khoi chay Che do ALL-IN-ONE (Portal + Backend tren Port 8080)..." -ForegroundColor Green
    Write-Host "--------------------------------------------------------" -ForegroundColor Cyan
    Write-Host "  TRUY CAP UNG DUNG TAI: http://localhost:8080" -ForegroundColor Yellow
    Write-Host "--------------------------------------------------------" -ForegroundColor Cyan
    Write-Host "Nhan Ctrl+C de dung ung dung.`n" -ForegroundColor Gray

    # Tu dong mo trinh duyet sau 4 giay
    Start-Job -ScriptBlock {
        Start-Sleep -Seconds 4
        Start-Process "http://localhost:8080"
    } | Out-Null

    Set-Location $BackendDir
    java -jar $jarFile
}

# CHE DO 2: DEV MODE (Chay song song Backend 8080 va Portal 3000)
elseif ($Mode -eq "dev" -or $Mode -eq "2") {
    Write-Host "`n[3/3] Dang khoi chay Che do DEV SONG SONG (Backend: 8080 | Portal: 3000)..." -ForegroundColor Green
    Write-Host "--------------------------------------------------------" -ForegroundColor Cyan
    Write-Host "  1. BACKEND API SERVICE : http://localhost:8080" -ForegroundColor Yellow
    Write-Host "  2. FRONTEND VITE PORTAL: http://localhost:3000" -ForegroundColor Yellow
    Write-Host "--------------------------------------------------------" -ForegroundColor Cyan
    Write-Host "Nhan Ctrl+C de dung toan bo ung dung.`n" -ForegroundColor Gray

    # 1. Khoi dong Backend Spring Boot trong tien trinh rieng
    Write-Host "Dang khoi dong Backend Spring Boot tren cong 8080..." -ForegroundColor Cyan
    $backendProcess = Start-Process -FilePath "powershell.exe" `
        -ArgumentList "-NoExit", "-Command", "Set-Location '$BackendDir'; .\run-demo.ps1" `
        -PassThru

    # 2. Khoi dong Frontend Vite Portal
    Write-Host "Dang khoi dong Frontend Vite Portal tren cong 3000..." -ForegroundColor Cyan
    
    # Tu dong mo trinh duyet Portal sau 3 giay
    Start-Job -ScriptBlock {
        Start-Sleep -Seconds 3
        Start-Process "http://localhost:3000"
    } | Out-Null

    Set-Location $PortalDir
    try {
        npm run dev
    } finally {
        Write-Host "`nDang dung tien trinh Backend..." -ForegroundColor Yellow
        if ($backendProcess -and -not $backendProcess.HasExited) {
            Stop-Process -Id $backendProcess.Id -Force -ErrorAction SilentlyContinue
        }
        Write-Host "Da dung toan bo he thong.`n" -ForegroundColor Green
    }
}
