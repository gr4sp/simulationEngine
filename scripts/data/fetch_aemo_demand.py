"""
Download AEMO NEMWeb TRADINGREGIONSUM archive files and load VIC1 30-minute
demand and spot price into gr4spdb.total_demand_halfhour.

No API key required. Data is publicly available on AEMO NEMWeb.

Run from project root:
  python scripts/data/fetch_aemo_demand.py

Gap to fill: 2021-07-01 to 2025-12-31 (~79,000 rows).
"""

import io
import os
import zipfile
import requests
import pandas as pd
import psycopg2
from dotenv import load_dotenv

load_dotenv()

DB_URL = os.environ["DB_URL"]

START_YEAR,  START_MONTH = 2021, 7
END_YEAR,    END_MONTH   = 2025, 12
DELETE_FROM              = "2021-07-01"

NEMWEB_URL = (
    "https://www.nemweb.com.au/Data_Archive/Wholesale_Electricity/MMSDM"
    "/{year}/MMSDM_{year}_{month:02d}"
    "/MMSDM_Historical_Data_SQLLoader/DATA"
    "/PUBLIC_DVD_TRADINGREGIONSUM_{year}{month:02d}010000.zip"
)


def iter_months(start_year, start_month, end_year, end_month):
    year, month = start_year, start_month
    while (year, month) <= (end_year, end_month):
        yield year, month
        month += 1
        if month > 12:
            month = 1
            year += 1


def fetch_month(year, month):
    url = NEMWEB_URL.format(year=year, month=month)
    print(f"  Downloading {year}-{month:02d}...", end=" ", flush=True)
    r = requests.get(url, timeout=120)
    if r.status_code == 404:
        print("not found (skipping)")
        return None
    r.raise_for_status()
    print(f"{len(r.content) // 1024} KB")
    return r.content


def parse_zip(zip_bytes):
    """Extract CSV from zip and return VIC1 rows as DataFrame."""
    with zipfile.ZipFile(io.BytesIO(zip_bytes)) as zf:
        csv_name = next(n for n in zf.namelist() if n.endswith(".CSV"))
        with zf.open(csv_name) as f:
            # AEMO CSVs have a header row starting with 'D' for data rows
            raw = pd.read_csv(f, header=None, low_memory=False)

    data_rows = raw[raw[0] == "D"]

    # Column layout for TRADINGREGIONSUM (positions are fixed in AEMO MMS format)
    # 0:record_type 1:table 2:subtype 3:run_no 4:REGIONID 5:SETTLEMENTDATE
    # 6:RUNNO 7:TOTALDEMAND 8:AVAILABLEGENERATION ... 11:RRP
    data_rows = data_rows[data_rows[4] == "VIC1"].copy()
    data_rows = data_rows[[5, 7, 11]].copy()
    data_rows.columns = ["settlement_date", "total_demand", "price"]
    data_rows["settlement_date"] = pd.to_datetime(data_rows["settlement_date"])
    data_rows["total_demand"]    = pd.to_numeric(data_rows["total_demand"], errors="coerce")
    data_rows["price"]           = pd.to_numeric(data_rows["price"],        errors="coerce")
    data_rows = data_rows.dropna()

    return data_rows


def load_to_db(all_rows):
    conn = psycopg2.connect(DB_URL)
    cur  = conn.cursor()

    print(f"\nDeleting total_demand_halfhour rows from {DELETE_FROM}...")
    cur.execute("DELETE FROM total_demand_halfhour WHERE settlement_date >= %s", (DELETE_FROM,))

    print(f"Inserting {len(all_rows)} rows...")
    cur.executemany(
        "INSERT INTO total_demand_halfhour (settlement_date, total_demand, price) VALUES (%s, %s, %s)",
        all_rows[["settlement_date", "total_demand", "price"]].itertuples(index=False, name=None),
    )

    conn.commit()
    cur.close()
    conn.close()
    print("total_demand_halfhour loaded successfully.")


if __name__ == "__main__":
    frames = []
    for year, month in iter_months(START_YEAR, START_MONTH, END_YEAR, END_MONTH):
        zip_bytes = fetch_month(year, month)
        if zip_bytes is None:
            continue
        try:
            df = parse_zip(zip_bytes)
            frames.append(df)
            print(f"    -> {len(df)} VIC1 rows")
        except Exception as e:
            print(f"    WARNING: failed to parse {year}-{month:02d}: {e}")

    if not frames:
        print("No data fetched. Check AEMO NEMWeb URLs and network access.")
    else:
        all_rows = pd.concat(frames, ignore_index=True)
        all_rows = all_rows.sort_values("settlement_date").drop_duplicates("settlement_date")
        load_to_db(all_rows)
        print(f"Done. {len(all_rows)} rows loaded for {START_YEAR}-{START_MONTH:02d} to {END_YEAR}-{END_MONTH:02d}.")
