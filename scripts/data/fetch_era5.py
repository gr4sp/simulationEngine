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

Note: The ERA5 download is large (~500 MB for 5 years). Run overnight or in the background.
The script downloads to era5_vic_solar.nc in the current directory (gitignored).
"""

import os
import cdsapi
import numpy as np
import pandas as pd
import psycopg2
import netCDF4 as nc
from dotenv import load_dotenv

load_dotenv()

DB_URL  = os.environ["DB_URL"]
CDS_URL = os.environ.get("CDS_URL", "https://cds.climate.copernicus.eu/api")
CDS_KEY = os.environ.get("CDS_KEY")

# Melbourne Airport (BOM station 086282): lat=-37.67, lon=144.83
# ERA5 grid spacing is 0.25 degrees; this bounding box captures the nearest grid point
AREA = [-37.5, 144.75, -37.75, 145.0]  # N, W, S, E

START_YEAR  = 2020
END_YEAR    = 2025
NC_FILE     = "era5_vic_solar.nc"
TEMP_CSV    = os.path.join(os.path.dirname(__file__), "monthly_temp.csv")
DELETE_FROM = "2020-08-01"


def download_era5():
    client_kwargs = {"url": CDS_URL}
    if CDS_KEY:
        client_kwargs["key"] = CDS_KEY

    c = cdsapi.Client(**client_kwargs)

    years  = [str(y) for y in range(START_YEAR, END_YEAR + 1)]
    months = [f"{m:02d}" for m in range(1, 13)]
    days   = [f"{d:02d}" for d in range(1, 32)]
    times  = [f"{h:02d}:00" for h in range(24)]

    print(f"Requesting ERA5 data {START_YEAR}-{END_YEAR} (this may take a while)...")
    c.retrieve(
        "reanalysis-era5-single-levels",
        {
            "product_type": "reanalysis",
            "variable": [
                "surface_solar_radiation_downwards",
                "2m_temperature",
            ],
            "year":  years,
            "month": months,
            "day":   days,
            "time":  times,
            "area":  AREA,
            "format": "netcdf",
        },
        NC_FILE,
    )
    print(f"Downloaded ERA5 data to {NC_FILE}")


def process_era5():
    """
    Read ERA5 NetCDF, compute:
    - 30-min GHI in W/m² for solar_ghi table
    - Monthly mean temperature in °C for monthly_temp.csv
    """
    print(f"Processing {NC_FILE}...")
    ds = nc.Dataset(NC_FILE)

    # Time: hours since epoch
    time_var  = ds.variables["time"]
    times_dt  = nc.num2date(time_var[:], time_var.units, calendar="standard")
    times_dt  = pd.DatetimeIndex([pd.Timestamp(str(t)) for t in times_dt])

    # ssrd: surface solar radiation downwards (J/m² accumulated per hour)
    # ERA5 stores accumulations — divide by 3600 to get W/m²
    ssrd = ds.variables["ssrd"][:]          # shape: (time, lat, lon)
    ssrd = np.squeeze(ssrd)                  # reduce singleton lat/lon dims
    if ssrd.ndim > 1:
        ssrd = ssrd.mean(axis=tuple(range(1, ssrd.ndim)))  # average over grid points
    ghi_wm2 = np.array(ssrd) / 3600.0
    ghi_wm2 = np.maximum(ghi_wm2, 0)        # no negative irradiance

    # t2m: 2m temperature (K)
    t2m = ds.variables["t2m"][:]
    t2m = np.squeeze(t2m)
    if t2m.ndim > 1:
        t2m = t2m.mean(axis=tuple(range(1, t2m.ndim)))
    temp_c = np.array(t2m) - 273.15

    ds.close()

    # Build hourly Series
    ghi_hourly  = pd.Series(ghi_wm2, index=times_dt)
    temp_hourly = pd.Series(temp_c,  index=times_dt)

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
    cur.executemany("INSERT INTO solar_ghi (time, ghi) VALUES (%s, %s)", rows)

    conn.commit()
    cur.close()
    conn.close()
    print("solar_ghi loaded successfully.")


if __name__ == "__main__":
    if not os.path.exists(NC_FILE):
        download_era5()
    else:
        print(f"{NC_FILE} already exists — skipping download. Delete it to re-download.")

    ghi_30min = process_era5()
    load_solar_to_db(ghi_30min)
    print(f"Done. monthly_temp.csv written for use by fetch_openelectricity.py.")
