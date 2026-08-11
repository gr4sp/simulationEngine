#!/bin/bash
# GR4SP Setup Script (Linux / macOS)
# Run once after cloning the repository: ./setup.sh
#
# --force skips the confirmation prompt shown when an existing gr4spdb would be
# destroyed (for unattended/CI use).

set -e

FORCE=0
[ "$1" = "--force" ] && FORCE=1

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

# Print the bin/ directory of the newest PostgreSQL install that ships
# pg_restore, or nothing. Homebrew's postgresql@NN formulae are keg-only and are
# never linked onto PATH, and a server-only Linux install leaves the binaries
# under /usr/lib/postgresql/NN/bin -- in both cases a correct installation looks
# missing to command -v.
find_pg_bin() {
    local best_dir="" best_ver=-1 dir ver
    for dir in \
        /opt/homebrew/opt/postgresql@*/bin \
        /usr/local/opt/postgresql@*/bin \
        /opt/homebrew/opt/postgresql/bin \
        /usr/local/opt/postgresql/bin \
        /usr/lib/postgresql/*/bin \
        /usr/pgsql-*/bin \
        /Library/PostgreSQL/*/bin
    do
        [ -x "$dir/pg_restore" ] || continue
        # Trailing version number in the path (postgresql@16, /14/, pgsql-16).
        ver="$(printf '%s' "$dir" | sed -n 's/.*[@/-]\([0-9][0-9]*\)\(\.[0-9]*\)*\/bin$/\1/p')"
        [ -n "$ver" ] || ver=0
        if [ "$ver" -gt "$best_ver" ]; then
            best_ver="$ver"
            best_dir="$dir"
        fi
    done
    printf '%s' "$best_dir"
}

# --- Step 4: Check PostgreSQL ---
step "Checking PostgreSQL..."
if ! command -v pg_restore &> /dev/null; then
    PG_BIN="$(find_pg_bin)"
    if [ -n "$PG_BIN" ]; then
        export PATH="$PG_BIN:$PATH"
        ok "Found PostgreSQL at $PG_BIN (not on PATH; added for this session)"
    fi
fi

if ! command -v pg_restore &> /dev/null; then
    echo "  PostgreSQL not found."
    echo "  Ubuntu/Debian: sudo apt install postgresql"
    echo "  macOS:         brew install postgresql"
    echo "  If it is already installed somewhere unusual, add its bin/ directory"
    echo "  to PATH and re-run this script."
    exit 1
fi
ok "$(pg_restore --version)"

# --- Step 5 & 6: Set up database ---
step "Setting up database 'gr4spdb'..."
read -s -p "Enter PostgreSQL password for user 'postgres': " PGPASSWORD
echo
export PGPASSWORD

# Refuse to silently destroy an existing database. A first-time reader has no
# gr4spdb and sails past this; anyone who has refreshed their data with the
# scripts in scripts/data/ gets a chance to back it up first.
PSQL="$(dirname "$(command -v pg_restore)")/psql"
DB_EXISTS=""
if [ -x "$PSQL" ]; then
    DB_EXISTS="$("$PSQL" -U postgres -tAc "SELECT 1 FROM pg_database WHERE datname='gr4spdb'" 2>/dev/null || true)"
fi

if [ "$DB_EXISTS" = "1" ] && [ "$FORCE" -eq 0 ]; then
    echo
    echo "  WARNING: a database named 'gr4spdb' already exists."
    echo "  Continuing DROPS it and restores the 2021 snapshot, discarding any data"
    echo "  loaded since - including refreshes made with scripts/data/*.py."
    echo
    echo "  To keep a copy first, cancel and run:"
    echo "    pg_dump -U postgres -Fc -f backupDB/DB-$(date +%Y-%m-%d).sql gr4spdb"
    echo
    read -p "  Type 'yes' to drop and re-restore gr4spdb (anything else cancels): " ANSWER
    if [ "$ANSWER" != "yes" ]; then
        echo "  Cancelled - your database was left untouched."
        exit 0
    fi
fi

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
