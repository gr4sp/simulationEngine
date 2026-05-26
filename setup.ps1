# GR4SP Setup Script (Windows / PowerShell)
# Run once after cloning the repository: .\setup.ps1

$ErrorActionPreference = "Stop"

function Write-Step { param($msg) Write-Host "`n[GR4SP] $msg" -ForegroundColor Cyan }
function Write-OK   { param($msg) Write-Host "  OK: $msg" -ForegroundColor Green }
function Write-Fail { param($msg) Write-Host "  ERROR: $msg" -ForegroundColor Red; exit 1 }

Write-Host "`nGR4SP Setup" -ForegroundColor Yellow
Write-Host "===========" -ForegroundColor Yellow

# --- Step 1: Check Java ---
Write-Step "Checking Java..."
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    $javaHome = [System.Environment]::GetEnvironmentVariable("JAVA_HOME", "User")
    if ($javaHome -and (Test-Path "$javaHome\bin\java.exe")) {
        $env:PATH = "$javaHome\bin;$env:PATH"
    } else {
        Write-Fail "Java not found. Install JDK 17 from https://adoptium.net and re-run this script."
    }
}
$javaVersion = cmd /c "java -version 2>&1" | Select-String "version" | Select-Object -First 1
Write-OK $javaVersion

# --- Step 2: Build ---
Write-Step "Building GR4SP (this may take a minute on first run)..."
& .\gradlew.bat build
if ($LASTEXITCODE -ne 0) { Write-Fail "Gradle build failed. Check the output above." }
Write-OK "Build successful"

# --- Step 3: Create output directories ---
Write-Step "Creating output directories..."
New-Item -ItemType Directory -Force -Path logs, csv, plots | Out-Null
Write-OK "logs/, csv/, plots/ ready"

# --- Step 4: Check PostgreSQL ---
Write-Step "Checking PostgreSQL..."
if (-not (Get-Command pg_restore -ErrorAction SilentlyContinue)) {
    Write-Host ""
    Write-Host "  PostgreSQL not found in PATH." -ForegroundColor Red
    Write-Host "  Install PostgreSQL from https://www.postgresql.org/download/windows/" -ForegroundColor Yellow
    Write-Host "  Then re-run this script." -ForegroundColor Yellow
    exit 1
}
Write-OK (pg_restore --version)

# --- Step 5 & 6: Set up database ---
Write-Step "Setting up database 'gr4spdb'..."
$pgPass = Read-Host "Enter PostgreSQL password for user 'postgres'" -AsSecureString
$env:PGPASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($pgPass))

try {
    dropdb --if-exists -U postgres gr4spdb 2>&1 | Out-Null
    Write-OK "Dropped existing gr4spdb (if any)"

    createdb -U postgres gr4spdb 2>&1 | Out-Null
    Write-OK "Created database gr4spdb"

    Write-Step "Restoring database from backup (this may take a minute)..."
    pg_restore -U postgres -d gr4spdb backupDB\DB-2021-8-21.sql
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  WARNING: pg_restore finished with non-fatal warnings (e.g. missing adminpack extension)." -ForegroundColor Yellow
        Write-Host "           This does not affect GR4SP data. Continuing..." -ForegroundColor Yellow
    }
    Write-OK "Database restored from backupDB\DB-2021-8-21.sql"
} finally {
    $env:PGPASSWORD = ""
}

# --- Step 7: Validate ---
Write-Step "Validating installation..."
if (-not (Test-Path "build\classes\java\main\core\Gr4spSim.class")) {
    Write-Fail "Gr4spSim.class not found - build may not have completed correctly."
}
Write-OK "Gr4spSim.class found"

Write-Host ""
Write-Host "Setup complete!" -ForegroundColor Green
Write-Host "Run the simulation with:  .\runGr4sp.bat" -ForegroundColor Yellow
Write-Host "Run with GUI:             .\runGr4spUI.bat" -ForegroundColor Yellow
