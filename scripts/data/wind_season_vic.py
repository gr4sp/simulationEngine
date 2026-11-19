"""Seasonal shape of Victorian wind output (N38 c, 19 Nov 2026), for simulationSettings/seasonal_factors_VIC.csv.

Source: gr4spdb table wind30min_mtmercer (half-hourly cleared output of Mt Mercer wind farm, 131 MW, 1 January 2015 to
19 July 2018; DB-2021-8-21). Factor for each calendar month = the farm's mean output in that month (complete years
2015-2017 only, so each month is weighted equally) over its mean over all months. The model keeps its annual wind
capacity factor (0.30); this only shapes it through the year.
Caveat: Mt Mercer is one of about ten farms in the Victorian wind record (OpenNEM), so the shape is not independent of
the comparator for 2015-2017, though it is one farm's shape applied to the whole fleet for 1999-2019.
Usage: python scripts/data/wind_season_vic.py"""
import io, os, subprocess
import pandas as pd

PSQL = r"C:/Program Files/PostgreSQL/18/bin/psql.exe"
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), os.pardir, os.pardir)
q = "select date, wind_cleared from wind30min_mtmercer where date >= '2015-01-01' and date < '2018-01-01' order by 1"
txt = subprocess.run([PSQL, "-h", "localhost", "-U", "postgres", "-d", "gr4spdb", "-A", "-F,", "-P", "footer=off", "-c", q],
                     capture_output=True, text=True, check=True).stdout
w = pd.read_csv(io.StringIO(txt), parse_dates=["date"])
f = w.groupby(w.date.dt.month).wind_cleared.mean()
f = f / f.mean()
out = pd.DataFrame({"fuel": "Wind", "month": f.index, "factor": f.round(4).values,
                    "source": "Mt Mercer half-hourly output 2015-2017 (gr4spdb wind30min_mtmercer); one farm in the record"})
dest = os.path.join(ROOT, "simulationSettings", "seasonal_factors_VIC.csv")
other = pd.read_csv(dest, comment="#") if os.path.exists(dest) else pd.DataFrame(columns=out.columns)
other = other[other.fuel != "Wind"]
pd.concat([other, out]).to_csv(dest, index=False)
print(out.to_string())
