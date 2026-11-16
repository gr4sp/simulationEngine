"""Hourly 2 m air temperature for Melbourne from ERA5, 1999-2020, for the load temperature response (solar step 3, D4b).

Source: ECMWF ERA5 reanalysis, single levels, variable 2m_temperature (Copernicus Climate Data Store), the same four
grid points as fetch_era5.py (box 37.5-37.75 S, 144.75-145.0 E: Melbourne Airport to the inner north), averaged.
ERA5 times are UTC; they are converted to market time (AEST, UTC+10, no daylight saving, as the NEM) and stamped at
the end of the hour, as the half-hour demand is (period ending).

This script writes a CSV only; it does not touch the database (fetch_era5.py replaces solar_ghi rows).
Downloads are cached by year in scripts/data/era5_downloads/ (git-ignored), so a re-run skips what is there.

Output: experiments/solarAdoption/data/era5_t2m_melbourne_hourly.csv (time_aest, t2m_c)
Usage: python fetch_era5_temperature.py [--start 1999] [--end 2020]
Needs CDS_URL and CDS_KEY in scripts/data/.env (or ~/.cdsapirc).
"""
import argparse, glob, os, shutil, tempfile, zipfile
from concurrent.futures import ThreadPoolExecutor, as_completed
import numpy as np
import pandas as pd
import netCDF4 as nc
from dotenv import load_dotenv

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, os.pardir, os.pardir))
load_dotenv(os.path.join(HERE, ".env"))
AREA = [-37.5, 144.75, -37.75, 145.0]  # N, W, S, E (as fetch_era5.py)
CACHE = os.path.join(HERE, "era5_downloads")
OUT = os.path.join(ROOT, "experiments", "solarAdoption", "data", "era5_t2m_melbourne_hourly.csv")


def download(year):
    path = os.path.join(CACHE, f"era5_t2m_{year}.nc")
    if os.path.exists(path):
        return path
    import cdsapi
    c = cdsapi.Client(url=os.environ.get("CDS_URL", "https://cds.climate.copernicus.eu/api"), key=os.environ.get("CDS_KEY"),
                      quiet=True)
    tmp = path + ".part"
    c.retrieve("reanalysis-era5-single-levels", {
        "product_type": ["reanalysis"], "variable": ["2m_temperature"], "year": [str(year)],
        "month": [f"{m:02d}" for m in range(1, 13)], "day": [f"{d:02d}" for d in range(1, 32)],
        "time": [f"{h:02d}:00" for h in range(24)], "area": AREA,
        "data_format": "netcdf", "download_format": "unarchived"}, tmp)
    os.replace(tmp, path)
    return path


def read(path):
    """Hourly grid-mean t2m (deg C) indexed by UTC time; the file may be a NetCDF or a zip holding one."""
    tmpdir = None
    try:
        if zipfile.is_zipfile(path):
            tmpdir = tempfile.mkdtemp(prefix="era5t_")
            with zipfile.ZipFile(path) as z:
                z.extractall(tmpdir)
            path = glob.glob(os.path.join(tmpdir, "*.nc"))[0]
        ds = nc.Dataset(path)
        tname = "valid_time" if "valid_time" in ds.variables else "time"
        tv = ds.variables[tname]
        t = pd.DatetimeIndex([pd.Timestamp(str(x)) for x in nc.num2date(tv[:], tv.units, calendar="standard")])
        v = np.squeeze(np.array(ds.variables["t2m"][:], dtype=float))
        if v.ndim > 1:
            v = v.reshape(v.shape[0], -1).mean(axis=1)
        ds.close()
        return pd.Series(v - 273.15, index=t)
    finally:
        if tmpdir:
            shutil.rmtree(tmpdir, ignore_errors=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--start", type=int, default=1999)
    ap.add_argument("--end", type=int, default=2020)
    a = ap.parse_args()
    os.makedirs(CACHE, exist_ok=True)
    years = list(range(a.start, a.end + 1))
    paths, failed = {}, []
    with ThreadPoolExecutor(max_workers=6) as pool:
        fut = {pool.submit(download, y): y for y in years}
        for f in as_completed(fut):
            y = fut[f]
            try:
                paths[y] = f.result()
                print(y, "ok", flush=True)
            except Exception as e:
                failed.append(y)
                print(y, "FAILED", e, flush=True)
    s = pd.concat([read(paths[y]) for y in sorted(paths)]).sort_index()
    s = s[~s.index.duplicated()]
    # ERA5 stamps the instant at the top of the hour (UTC) -> AEST
    s.index = s.index + pd.Timedelta(hours=10)
    out = pd.DataFrame({"time_aest": s.index.strftime("%Y-%m-%d %H:%M"), "t2m_c": s.round(2).values})
    with open(OUT, "w", newline="\n") as fh:
        fh.write("# ERA5 hourly 2 m temperature (deg C), mean of the 4 grid points 37.5-37.75 S, 144.75-145.0 E (Melbourne),\n"
                 "# instantaneous at the stated time, AEST (UTC+10, no daylight saving). Copernicus Climate Data Store,\n"
                 "# reanalysis-era5-single-levels; built by scripts/data/fetch_era5_temperature.py.\n")
        out.to_csv(fh, index=False)
    print("written", OUT, len(out), "hours", out.time_aest.iloc[0], "to", out.time_aest.iloc[-1], "failed:", failed)


if __name__ == "__main__":
    main()
