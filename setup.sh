#!/bin/bash
# GR4SP Setup Script (Linux / macOS)
# Run once after cloning the repository: ./setup.sh

set -e

step() { echo; echo "[GR4SP] $1"; }
ok()   { echo "  OK: $1"; }
fail() { echo "  ERROR: $1" >&2; exit 1; }

echo
echo "GR4SP Setup"
echo "==========="

# --- Step 1: Check Java ---
step "Checking Java..."
java -version 2>&1 | head -1 || fail "Java not found. Install JDK 17 (e.g. sudo apt install default-jdk) and re-run."
ok "Java found"

# --- Step 2: Make gradlew executable and build ---
step "Building GR4SP (this may take a minute on first run)..."
chmod +x gradlew
./gradlew build || fail "Gradle build failed. Check the output above."
ok "Build successful"

# --- Step 3: Create output directories ---
step "Creating output directories..."
mkdir -p logs csv plots
ok "logs/, csv/, plots/ ready"

# --- Step 4: Check PostgreSQL ---
step "Checking PostgreSQL..."
if ! command -v pg_restore &> /dev/null; then
    echo "  PostgreSQL not found."
    echo "  Ubuntu/Debian: sudo apt install postgresql"
    echo "  macOS:         brew install postgresql"
    echo "  Then re-run this script."
    exit 1
fi
ok "$(pg_restore --version)"

# --- Step 5 & 6: Set up database ---
step "Setting up database 'gr4spdb'..."
read -s -p "Enter PostgreSQL password for user 'postgres': " PGPASSWORD
echo
export PGPASSWORD

dropdb --if-exists -U postgres gr4spdb 2>/dev/null && ok "Dropped existing gr4spdb (if any)" || true
createdb -U postgres gr4spdb
ok "Created database gr4spdb"

step "Restoring database from backup (this may take a minute)..."
pg_restore -U postgres -d gr4spdb backupDB/DB-2021-8-21.sql
ok "Database restored from backupDB/DB-2021-8-21.sql"

unset PGPASSWORD

# --- Step 7: Validate ---
step "Validating installation..."
if [ ! -f "build/classes/java/main/core/Gr4spSim.class" ]; then
    fail "Gr4spSim.class not found — build may not have completed correctly."
fi
ok "Gr4spSim.class found"

echo
echo "Setup complete!"
echo "Run the simulation with:  ./runGr4sp.sh"
echo "Run with GUI:             ./runGr4spUI.sh"
