"""
Download AEMO NEMWeb archives and load VIC1 demand/price into gr4spdb.

AEMO's market design changed on 2021-10-01 (5-minute settlement), which
splits this backfill into two regimes:
  - Jul-Sep 2021: TRADINGREGIONSUM (demand) + TRADINGPRICE (RRP), 30-minute.
  - Oct 2021 onward: DISPATCHREGIONSUM (demand) + DISPATCHPRICE (RRP), 5-minute.
Neither REGIONSUM table carries its own price column — RRP lives in the
matching PRICE table, joined by SETTLEMENTDATE + REGIONID.

The 5-minute data is kept at native resolution in dispatch_region_5min
(for future distribution-network/grid-stability work) AND averaged into
30-minute buckets in total_demand_halfhour, so existing EMA Workbench
scenarios keep reading the same schema they always have.

No API key required. Data is publicly available on AEMO NEMWeb.

Run from project root:
  python scripts/data/fetch_aemo_demand.py

Gap filled: 2021-07-01 to 2025-12-31. The DISPATCH years download ~1-2 GB
across both tables — expect this to take a while on a slow connection.
"""

import io
import os
import csv
import zipfile
import requests
import pandas as pd
import psycopg2
from psycopg2.extras import execute_values
from dotenv import load_dotenv

load_dotenv()

DB_URL = os.environ["DB_URL"]

TRADING_START,  TRADING_END  = (2021, 7), (2021, 9)
DISPATCH_START, DISPATCH_END = (2021, 10), (2025, 12)
DELETE_FROM = "2021-07-01"

# AEMO renamed its monthly archives partway through this range: older months
# use PUBLIC_DVD_{table}_..., newer months (Aug 2024 onward, as of this
# writing) use PUBLIC_ARCHIVE#{table}#FILE01#... instead. Try both.
NEMWEB_DIR = (
    "https://www.nemweb.com.au/Data_Archive/Wholesale_Electricity/MMSDM"
    "/{year}/MMSDM_{year}_{month:02d}/MMSDM_Historical_Data_SQLLoader/DATA"
)
NEMWEB_URL_PATTERNS = [
    NEMWEB_DIR + "/PUBLIC_DVD_{table}_{year}{month:02d}010000.zip",
    NEMWEB_DIR + "/PUBLIC_ARCHIVE%23{table}%23FILE01%23{year}{month:02d}010000.zip",
]


def iter_months(start, end):
    year, month = start
    end_year, end_month = end
    while (year, month) <= (end_year, end_month):
        yield year, month
        month += 1
        if month > 12:
            month = 1
            year += 1


def fetch_table(table, year, month):
    """Download one MMSDM monthly archive and return its D rows as a
    DataFrame, with columns taken from the file's own I (header) row.
    Returns None if the archive doesn't exist for that month."""
    r = None
    for pattern in NEMWEB_URL_PATTERNS:
        url = pattern.format(table=table, year=year, month=month)
        r = requests.get(url, timeout=120)
        if r.status_code != 404:
            break
    if r.status_code == 404:
        return None
    r.raise_for_status()

    with zipfile.ZipFile(io.BytesIO(r.content)) as zf:
        csv_name = next(n for n in zf.namelist() if n.endswith(".CSV"))
        with zf.open(csv_name) as f:
            text = f.read().decode("utf-8", errors="replace")

    header, data_rows = None, []
    for row in csv.reader(text.splitlines()):
        if not row:
            continue
        if row[0] == "I":
            header = row[4:]
        elif row[0] == "D" and header is not None:
            data_rows.append(dict(zip(header, row[4:])))

    return pd.DataFrame(data_rows) if data_rows else None


def fetch_region_and_price(region_table, price_table, year, month):
    """Fetch and join VIC1 demand + price for one month."""
    demand_df = fetch_table(region_table, year, month)
    price_df  = fetch_table(price_table, year, month)
    if demand_df is None or price_df is None:
        return None

    demand_df = demand_df.loc[demand_df["REGIONID"] == "VIC1", ["SETTLEMENTDATE", "TOTALDEMAND"]]
    price_df  = price_df.loc[price_df["REGIONID"] == "VIC1", ["SETTLEMENTDATE", "RRP"]]

    merged = demand_df.merge(price_df, on="SETTLEMENTDATE", how="left")
    merged.columns = ["settlement_date", "total_demand", "price"]
    merged["settlement_date"] = pd.to_datetime(merged["settlement_date"])
    merged["total_demand"]    = pd.to_numeric(merged["total_demand"], errors="coerce")
    merged["price"]           = pd.to_numeric(merged["price"], errors="coerce")
    return merged.dropna(subset=["settlement_date", "total_demand"])


def resample_to_halfhour(fivemin_df):
    """Average 5-minute VIC1 rows into 30-minute buckets."""
    return (
        fivemin_df.set_index("settlement_date")
        .resample("30min", label="right", closed="right")
        .mean()
        .dropna()
        .reset_index()
    )


def insert_rows(conn, table, rows):
    cur = conn.cursor()
    print(f"Deleting {table} rows from {DELETE_FROM}...")
    cur.execute(f"DELETE FROM {table} WHERE settlement_date >= %s", (DELETE_FROM,))
    print(f"Inserting {len(rows)} rows into {table}...")
    execute_values(
        cur,
        f"INSERT INTO {table} (settlement_date, total_demand, price) VALUES %s",
        rows[["settlement_date", "total_demand", "price"]].itertuples(index=False, name=None),
    )
    conn.commit()
    cur.close()


def ensure_dispatch_5min_table(conn):
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS dispatch_region_5min (
            settlement_date timestamp without time zone,
            total_demand double precision,
            price double precision
        )
    """)
    conn.commit()
    cur.close()


if __name__ == "__main__":
    halfhour_frames = []

    print("Fetching 30-minute TRADING data (pre 5-min settlement, Jul-Sep 2021)...")
    for year, month in iter_months(TRADING_START, TRADING_END):
        print(f"  {year}-{month:02d}...", end=" ", flush=True)
        df = fetch_region_and_price("TRADINGREGIONSUM", "TRADINGPRICE", year, month)
        if df is None:
            print("not found (skipping)")
            continue
        print(f"{len(df)} VIC1 rows")
        halfhour_frames.append(df)

    print("\nFetching 5-minute DISPATCH data (post 5-min settlement, Oct 2021 onward)...")
    fivemin_frames = []
    for year, month in iter_months(DISPATCH_START, DISPATCH_END):
        print(f"  {year}-{month:02d}...", end=" ", flush=True)
        df = fetch_region_and_price("DISPATCHREGIONSUM", "DISPATCHPRICE", year, month)
        if df is None:
            print("not found (skipping)")
            continue
        print(f"{len(df)} VIC1 rows")
        fivemin_frames.append(df)

    fivemin_all = None
    if fivemin_frames:
        fivemin_all = pd.concat(fivemin_frames, ignore_index=True)
        fivemin_all = fivemin_all.sort_values("settlement_date").drop_duplicates("settlement_date")
        halfhour_frames.append(resample_to_halfhour(fivemin_all))

    if not halfhour_frames:
        print("No data fetched. Check AEMO NEMWeb URLs and network access.")
    else:
        all_rows = pd.concat(halfhour_frames, ignore_index=True)
        all_rows = all_rows.sort_values("settlement_date").drop_duplicates("settlement_date")

        conn = psycopg2.connect(DB_URL)
        insert_rows(conn, "total_demand_halfhour", all_rows)
        if fivemin_all is not None:
            ensure_dispatch_5min_table(conn)
            insert_rows(conn, "dispatch_region_5min", fivemin_all)
        conn.close()

        print(f"\nDone. {len(all_rows)} half-hour rows loaded into total_demand_halfhour.")
        if fivemin_all is not None:
            print(f"Also stored {len(fivemin_all)} native 5-minute rows in dispatch_region_5min.")
