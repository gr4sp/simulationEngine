"""Seasonal availability of Victoria's brown coal units from AEMO's recorded dispatch availability (N38 b, 19 Nov 2026).

Source: AEMO MMSDM DISPATCHLOAD (5-minute unit solution, INTERVENTION = 0), field AVAILABILITY (MW the unit made
available to dispatch: planned and forced outages and deratings), January 2015 to December 2019 (one file per table
and month from 2015; earlier months are only whole-month bundles of about 2 GB, so not used: the 2015-2019 seasonal
shape is applied to every year, an assumption), units HWPS1-8 (Hazelwood), LYA1-4 (Loy Yang A), LOYYB1-2 (Loy Yang B), YWPS1-4 (Yallourn), MOR1-3
(Energy Brix). Each monthly zip (~45 MB) is streamed, reduced to daily mean availability per unit and deleted.

Output: simulationSettings/seasonal_factors_VIC.csv rows for Brown Coal (fuel,month,factor,source): for each unit and
complete calendar year, availability in each month over the unit's mean for that year (so closures and long outages
do not tilt the shape), averaged over unit-years weighted by the unit's mean availability. Forced outages are already
drawn at random in the model (forcedOutageRate); this factor is the recorded seasonal shape, mostly planned maintenance.
Also writes the daily table (coal_availability_daily_vic.csv) next to the cache for checking.
Usage: python scripts/data/coal_availability_vic.py CACHE_DIR"""
import io, os, sys, zipfile
import pandas as pd
import requests

CACHE = sys.argv[1]
os.makedirs(CACHE, exist_ok=True)
UNITS = [f"HWPS{i}" for i in range(1, 9)] + [f"LYA{i}" for i in range(1, 5)] + ["LOYYB1", "LOYYB2"] + \
        [f"YWPS{i}" for i in range(1, 5)] + ["MOR1", "MOR2", "MOR3"]
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), os.pardir, os.pardir)
daily_file = os.path.join(CACHE, "coal_availability_daily_vic.csv")


def month_daily(m):
    name = f"PUBLIC_DVD_DISPATCHLOAD_{m.year}{m.month:02d}010000.zip"
    url = (f"https://nemweb.com.au/Data_Archive/Wholesale_Electricity/MMSDM/{m.year}/MMSDM_{m.year}_{m.month:02d}/"
           f"MMSDM_Historical_Data_SQLLoader/DATA/{name}")
    path = os.path.join(CACHE, name)
    for attempt in range(5):  # NEMweb drops long downloads now and then
        try:
            r = requests.get(url, timeout=600)
            r.raise_for_status()
            open(path, "wb").write(r.content)
            break
        except requests.exceptions.RequestException:
            if attempt == 4:
                raise
    keep, head = [], None
    with zipfile.ZipFile(path) as z:
        with z.open(z.namelist()[0]) as f:
            for raw in io.TextIOWrapper(f, encoding="utf-8", errors="replace"):
                if raw.startswith("I,") and head is None:
                    head = raw
                elif raw.startswith("D,") and any(f",{u}," in raw for u in UNITS):
                    keep.append(raw)
    os.remove(path)
    t = pd.read_csv(io.StringIO(head + "".join(keep)), usecols=["SETTLEMENTDATE", "DUID", "INTERVENTION", "AVAILABILITY"])
    t = t[t.INTERVENTION == 0]
    t["day"] = pd.to_datetime(t.SETTLEMENTDATE).dt.floor("D")
    return t.groupby(["day", "DUID"]).AVAILABILITY.mean().reset_index()


done = pd.read_csv(daily_file, parse_dates=["day"]) if os.path.exists(daily_file) else pd.DataFrame()
have = set(done.day.dt.to_period("M")) if len(done) else set()
todo = [m for m in pd.period_range("2015-01", "2019-12", freq="M") if m not in have]
import concurrent.futures as cf
with cf.ThreadPoolExecutor(4) as ex:  # four downloads at a time; results saved as each month finishes
    futures = {ex.submit(month_daily, m): m for m in todo}
    for fut in cf.as_completed(futures):
        d = fut.result()
        done = pd.concat([done, d])
        done.to_csv(daily_file, index=False)
        print(futures[fut], len(d), flush=True)

d = done.copy()
d["year"], d["month"] = d.day.dt.year, d.day.dt.month
um = d.groupby(["DUID", "year", "month"]).AVAILABILITY.mean().reset_index()
full = um.groupby(["DUID", "year"]).month.transform("count") == 12
um = um[full]
um["year_mean"] = um.groupby(["DUID", "year"]).AVAILABILITY.transform("mean")
um = um[um.year_mean > 1]
um["ratio"] = um.AVAILABILITY / um.year_mean
f = um.groupby("month").apply(lambda g: (g.ratio * g.year_mean).sum() / g.year_mean.sum(), include_groups=False)
f = f / f.mean()
n = um.groupby(["DUID", "year"]).ngroups
out = pd.DataFrame({"fuel": "Brown Coal", "month": f.index, "factor": f.round(4).values,
                    "source": f"AEMO MMSDM DISPATCHLOAD AVAILABILITY 2015-01 to 2019-12, {n} complete unit-years"})
dest = os.path.join(ROOT, "simulationSettings", "seasonal_factors_VIC.csv")
other = pd.read_csv(dest, comment="#") if os.path.exists(dest) else pd.DataFrame(columns=out.columns)
other = other[other.fuel != "Brown Coal"]
pd.concat([other, out]).to_csv(dest, index=False)
print(out.to_string())
