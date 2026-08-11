# GR4SP Setup Script (Windows / PowerShell)
# Run once after cloning the repository: .\setup.ps1
#
# -Force skips the confirmation prompt shown when an existing gr4spdb would be
# destroyed (for unattended/CI use).
param([switch]$Force)

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
    # Refuse to silently destroy an existing database. A first-time reader has no
    # gr4spdb and sails past this; anyone who has refreshed their data with the
    # scripts in scripts/data/ gets a chance to back it up first.
    $psql = Join-Path (Split-Path (Get-Command pg_restore).Source) "psql.exe"
    $exists = ""
    if (Test-Path $psql) {
        $exists = (& $psql -U postgres -tAc "SELECT 1 FROM pg_database WHERE datname='gr4spdb'" 2>$null) -join ""
    }

    if ($exists.Trim() -eq "1" -and -not $Force) {
        Write-Host ""
        Write-Host "  WARNING: a database named 'gr4spdb' already exists." -ForegroundColor Yellow
        Write-Host "  Continuing DROPS it and restores the 2021 snapshot, discarding any data" -ForegroundColor Yellow
        Write-Host "  loaded since - including refreshes made with scripts/data/*.py." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "  To keep a copy first, cancel and run:" -ForegroundColor Yellow
        Write-Host "    pg_dump -U postgres -Fc -f backupDB\DB-$(Get-Date -Format 'yyyy-MM-dd').sql gr4spdb" -ForegroundColor Gray
        Write-Host ""
        # Read-Host throws in a non-interactive shell; treat that as "cancel" so an
        # unattended run never destroys data by accident (use -Force to mean it).
        $answer = ""
        try {
            $answer = Read-Host "  Type 'yes' to drop and re-restore gr4spdb (anything else cancels)"
        } catch {
            Write-Host "  (non-interactive shell - cannot prompt)" -ForegroundColor Yellow
        }

        if ($answer -ne "yes") {
            Write-Host "  Cancelled - your database was left untouched." -ForegroundColor Green
            Write-Host "  Re-run with -Force to skip this prompt." -ForegroundColor Gray
            exit 0
        }
    }

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
