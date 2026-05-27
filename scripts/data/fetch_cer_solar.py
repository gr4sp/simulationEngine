"""
Load CER postcode solar installation data into gr4spdb.solar_installation_monthly.

CER files require manual download (no direct API):
  1. Go to: https://www.cleanenergyregulator.gov.au/RET/Forms-and-resources/Postcode-data-for-small-scale-installations
  2. Download all quarterly Excel files covering 2020-01 to 2025-12
  3. Save them to:  scripts/data/cer_downloads/
  4. Run this script from project root:
       python scripts/data/fetch_cer_solar.py

Victoria postcodes: 3000-3999 and 8000-8999.

Gap to fill: 2020-01 to 2025-12 (~72 monthly rows).
"""

import os
import glob
import pandas as pd
import psycopg2
from dotenv import load_dotenv

load_dotenv()

DB_URL       = os.environ["DB_URL"]
CER_DIR      = os.path.join(os.path.dirname(__file__), "cer_downloads")
DELETE_FROM_YEAR = 2020

# Victoria postcode ranges
VIC_POSTCODE_RANGES = [(3000, 3999), (8000, 8999)]


def is_vic_postcode(postcode):
    try:
        pc = int(postcode)
        return any(lo <= pc <= hi for lo, hi in VIC_POSTCODE_RANGES)
    except (ValueError, TypeError):
        return False


def load_cer_file(path):
    """Read one CER Excel file and return Victoria rows."""
    print(f"  Reading {os.path.basename(path)}...", end=" ", flush=True)
    df = pd.read_excel(path, engine="openpyxl")
    df.columns = [str(c).strip().lower().replace(" ", "_") for c in df.columns]

    # Identify postcode column (CER files use 'postcode' or 'small_unit_postcode')
    postcode_col = next((c for c in df.columns if "postcode" in c), None)
    if postcode_col is None:
        print(f"WARNING: no postcode column found in {path}")
        return pd.DataFrame()

    # Identify date columns (CER uses 'year', 'month' or a date column)
    if "year" in df.columns and "month" in df.columns:
        df["year"]  = pd.to_numeric(df["year"],  errors="coerce")
        df["month"] = pd.to_numeric(df["month"], errors="coerce")
    elif "approved_date" in df.columns:
        df["approved_date"] = pd.to_datetime(df["approved_date"], errors="coerce")
        df["year"]  = df["approved_date"].dt.year
        df["month"] = df["approved_date"].dt.month
    elif "date_of_effect" in df.columns:
        df["date_of_effect"] = pd.to_datetime(df["date_of_effect"], errors="coerce")
        df["year"]  = df["date_of_effect"].dt.year
        df["month"] = df["date_of_effect"].dt.month
    else:
        # Try to find any date-like column
        date_col = next((c for c in df.columns if "date" in c), None)
        if date_col:
            df[date_col] = pd.to_datetime(df[date_col], errors="coerce")
            df["year"]  = df[date_col].dt.year
            df["month"] = df[date_col].dt.month
        else:
            print(f"WARNING: cannot find date column in {path}")
            return pd.DataFrame()

    # Identify capacity column
    cap_col = next((c for c in df.columns if "capacity" in c or "kw" in c.lower()), None)
    if cap_col is None:
        print(f"WARNING: no capacity column found in {path}")
        return pd.DataFrame()

    vic = df[df[postcode_col].apply(is_vic_postcode)].copy()
    vic = vic.dropna(subset=["year", "month"])
    vic["year"]  = vic["year"].astype(int)
    vic["month"] = vic["month"].astype(int)
    vic[cap_col] = pd.to_numeric(vic[cap_col], errors="coerce").fillna(0)

    print(f"{len(vic)} Victoria rows")
    return vic[[postcode_col, "year", "month", cap_col]].rename(
        columns={postcode_col: "postcode", cap_col: "capacity_kw"}
    )


def aggregate(frames):
    """Aggregate to monthly Victoria totals."""
    df = pd.concat(frames, ignore_index=True)
    df = df[df["year"] >= DELETE_FROM_YEAR]

    monthly = (
        df.groupby(["year", "month"])
        .agg(
            number_installations=("postcode", "count"),
            aggregated_capacity_kw=("capacity_kw", "sum"),
        )
        .reset_index()
    )
    monthly["system_capacity"] = (
        monthly["aggregated_capacity_kw"] / monthly["number_installations"]
    ).round(4)
    monthly["aggregated_capacity_kw"] = monthly["aggregated_capacity_kw"].round(0).astype(int)
    return monthly


def load_to_db(monthly):
    conn = psycopg2.connect(DB_URL)
    cur  = conn.cursor()

    print(f"\nDeleting solar_installation_monthly rows from year >= {DELETE_FROM_YEAR}...")
    cur.execute("DELETE FROM solar_installation_monthly WHERE year >= %s", (DELETE_FROM_YEAR,))

    print(f"Inserting {len(monthly)} monthly rows...")
    for _, row in monthly.iterrows():
        cur.execute(
            """INSERT INTO solar_installation_monthly
               (year, month, number_installations, aggregated_capacity_kw, system_capacity)
               VALUES (%s, %s, %s, %s, %s)""",
            (int(row["year"]), int(row["month"]),
             int(row["number_installations"]), int(row["aggregated_capacity_kw"]),
             float(row["system_capacity"])),
        )

    conn.commit()
    cur.close()
    conn.close()
    print("solar_installation_monthly loaded successfully.")


if __name__ == "__main__":
    if not os.path.isdir(CER_DIR):
        print(f"ERROR: CER downloads directory not found: {CER_DIR}")
        print("Please create it and place downloaded CER Excel files inside.")
        raise SystemExit(1)

    files = sorted(glob.glob(os.path.join(CER_DIR, "*.xlsx")) +
                   glob.glob(os.path.join(CER_DIR, "*.xls")))
    if not files:
        print(f"ERROR: No Excel files found in {CER_DIR}")
        print("Download quarterly postcode files from CER and place them there.")
        raise SystemExit(1)

    print(f"Found {len(files)} CER file(s):")
    frames = []
    for path in files:
        df = load_cer_file(path)
        if not df.empty:
            frames.append(df)

    if not frames:
        print("No usable data found in CER files.")
        raise SystemExit(1)

    monthly = aggregate(frames)
    load_to_db(monthly)
    print(f"Done. {len(monthly)} months loaded.")
