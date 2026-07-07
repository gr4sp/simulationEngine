"""
Fetch ERA5 reanalysis data from Copernicus CDS and load into gr4spdb.solar_ghi.
Also writes monthly_temp.csv for use by fetch_openelectricity.py.

Replaces BOM station 086282 (Melbourne Airport) one-minute solar data,
which is no longer publicly accessible.

Setup:
  Copy .env.example to ../../.env and fill in CDS_URL, CDS_KEY, and DB_URL.
  pip install -r requirements.txt

  The CDS API also reads credentials from ~/.cdsapirc:
    url: https://cds.climate.copernicus.eu/api
    key: your_uid:your_api_key

Run from project root:
  python scripts/data/fetch_era5.py

Note: CDS enforces a per-request "cost" limit that a full 5-year, all-hours
request exceeds ("cost limits exceeded — Your request is too large"), so
this fetches one month at a time instead, submitting several months
concurrently (CDS allows multiple jobs to queue per account) to keep total
wall-clock time down. Each month is cached under era5_downloads/ so a
re-run skips months already fetched.

Also note: CDS's newer backend returns a zip containing two separate NetCDF
files (instantaneous variables vs. accumulated variables) rather than one
combined file, and renamed the time dimension from "time" to "valid_time" —
process_month() below unzips and reads both.
"""

import os
import glob
import shutil
import zipfile
import tempfile
import calendar
from concurrent.futures import ThreadPoolExecutor, as_completed
import cdsapi
import numpy as np
import pandas as pd
import psycopg2
from psycopg2.extras import execute_values
import netCDF4 as nc
from dotenv import load_dotenv

load_dotenv()

DB_URL  = os.environ["DB_URL"]
CDS_URL = os.environ.get("CDS_URL", "https://cds.climate.copernicus.eu/api")
CDS_KEY = os.environ.get("CDS_KEY")

# Melbourne Airport (BOM station 086282): lat=-37.67, lon=144.83
# ERA5 grid spacing is 0.25 degrees; this bounding box captures the nearest grid point
AREA = [-37.5, 144.75, -37.75, 145.0]  # N, W, S, E

START_YEAR,  START_MONTH = 2020, 8
END_YEAR,    END_MONTH   = 2025, 12
DOWNLOAD_DIR = os.path.join(os.path.dirname(__file__), "era5_downloads")
TEMP_CSV     = os.path.join(os.path.dirname(__file__), "monthly_temp.csv")
DELETE_FROM  = "2020-08-01"
CONCURRENCY  = 8


def iter_months(start_year, start_month, end_year, end_month):
    year, month = start_year, start_month
    while (year, month) <= (end_year, end_month):
        yield year, month
        month += 1
        if month > 12:
            month = 1
            year += 1


def month_zip_path(year, month):
    return os.path.join(DOWNLOAD_DIR, f"era5_{year}_{month:02d}.zip")


def download_month(year, month):
    zip_path = month_zip_path(year, month)
    if os.path.exists(zip_path):
        return zip_path  # already downloaded on a previous run

    c = cdsapi.Client(url=CDS_URL, key=CDS_KEY)
    days_in_month = calendar.monthrange(year, month)[1]
    c.retrieve(
        "reanalysis-era5-single-levels",
        {
            "product_type": "reanalysis",
            "variable": ["surface_solar_radiation_downwards", "2m_temperature"],
            "year":  str(year),
            "month": f"{month:02d}",
            "day":   [f"{d:02d}" for d in range(1, days_in_month + 1)],
            "time":  [f"{h:02d}:00" for h in range(24)],
            "area":  AREA,
            "format": "netcdf",
        },
        zip_path,
    )
    return zip_path


def download_all():
    os.makedirs(DOWNLOAD_DIR, exist_ok=True)
    months = list(iter_months(START_YEAR, START_MONTH, END_YEAR, END_MONTH))
    print(f"Submitting {len(months)} monthly ERA5 requests ({CONCURRENCY} concurrent)...")

    failures = []
    with ThreadPoolExecutor(max_workers=CONCURRENCY) as pool:
        futures = {pool.submit(download_month, y, m): (y, m) for y, m in months}
        for future in as_completed(futures):
            y, m = futures[future]
            try:
                future.result()
                print(f"  {y}-{m:02d} done")
            except Exception as e:
                print(f"  {y}-{m:02d} FAILED: {e}")
                failures.append((y, m))

    if failures:
        print(f"WARNING: {len(failures)} month(s) failed to download: {failures}")


def _read_variable(nc_path, var_name):
    """Read one variable averaged over the grid, indexed by its valid_time."""
    ds = nc.Dataset(nc_path)
    time_var = ds.variables["valid_time"]
    times_dt = nc.num2date(time_var[:], time_var.units, calendar="standard")
    times_dt = pd.DatetimeIndex([pd.Timestamp(str(t)) for t in times_dt])

    values = ds.variables[var_name][:]
    values = np.squeeze(values)
    if values.ndim > 1:
        values = values.mean(axis=tuple(range(1, values.ndim)))  # average over grid points
    ds.close()
    return pd.Series(np.array(values), index=times_dt)


def process_month(zip_path):
    """Read one month's ERA5 zip (one NetCDF for accumulated variables, one
    for instantaneous variables) and return hourly (ghi, temp) Series."""
    extract_dir = tempfile.mkdtemp(prefix="era5_")
    try:
        with zipfile.ZipFile(zip_path) as zf:
            zf.extractall(extract_dir)

        accum_file   = glob.glob(os.path.join(extract_dir, "*accum*.nc"))[0]
        instant_file = glob.glob(os.path.join(extract_dir, "*instant*.nc"))[0]

        # ssrd: surface solar radiation downwards (J/m² accumulated per hour)
        # ERA5 stores accumulations — divide by 3600 to get W/m²
        ghi_hourly = _read_variable(accum_file, "ssrd") / 3600.0
        ghi_hourly = ghi_hourly.clip(lower=0)  # no negative irradiance

        # t2m: 2m temperature (K)
        temp_hourly = _read_variable(instant_file, "t2m") - 273.15
    finally:
        shutil.rmtree(extract_dir, ignore_errors=True)

    return ghi_hourly, temp_hourly


def process_all():
    """
    Read every downloaded monthly zip, compute:
    - 30-min GHI in W/m² for solar_ghi table
    - Monthly mean temperature in °C for monthly_temp.csv
    """
    months = list(iter_months(START_YEAR, START_MONTH, END_YEAR, END_MONTH))
    ghi_parts, temp_parts = [], []
    for year, month in months:
        zip_path = month_zip_path(year, month)
        if not os.path.exists(zip_path):
            print(f"  WARNING: {year}-{month:02d} missing (download failed?), skipping")
            continue
        print(f"Processing {year}-{month:02d}...")
        ghi, temp = process_month(zip_path)
        ghi_parts.append(ghi)
        temp_parts.append(temp)

    if not ghi_parts:
        raise RuntimeError("No months processed successfully — nothing to load.")

    ghi_hourly  = pd.concat(ghi_parts).sort_index()
    temp_hourly = pd.concat(temp_parts).sort_index()

    # Resample to 30-min by forward-filling (matches original BOM approach)
    ghi_30min = ghi_hourly.resample("30min").ffill()

    # Monthly average temperature → CSV for fetch_openelectricity.py
    temp_monthly = temp_hourly.resample("MS").mean().reset_index()
    temp_monthly.columns = ["date", "temp_c"]
    temp_monthly.to_csv(TEMP_CSV, index=False)
    print(f"Monthly temperature written to {TEMP_CSV}")

    return ghi_30min


def load_solar_to_db(ghi_30min):
    """Delete existing rows from DELETE_FROM and insert new ones."""
    conn = psycopg2.connect(DB_URL)
    cur  = conn.cursor()

    print(f"Deleting solar_ghi rows from {DELETE_FROM}...")
    cur.execute("DELETE FROM solar_ghi WHERE time >= %s", (DELETE_FROM,))

    rows = [(ts.to_pydatetime(), float(val)) for ts, val in ghi_30min.items()
            if ts >= pd.Timestamp(DELETE_FROM)]

    print(f"Inserting {len(rows)} rows into solar_ghi...")
    execute_values(cur, "INSERT INTO solar_ghi (time, ghi) VALUES %s", rows)

    conn.commit()
    cur.close()
    conn.close()
    print("solar_ghi loaded successfully.")


if __name__ == "__main__":
    download_all()
    ghi_30min = process_all()
    load_solar_to_db(ghi_30min)
    print(f"Done. monthly_temp.csv written for use by fetch_openelectricity.py.")
