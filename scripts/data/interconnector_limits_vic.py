"""Import capability into Victoria at peak demand, per link, for simulationSettings/interconnectors_VIC.csv.

Source: AEMO MMSDM DISPATCHINTERCONNECTORRES (5-minute dispatch, INTERVENTION = 0) for 2015-01 to 2020-12 from
NEMweb. Capability into VIC is -IMPORTLIMIT for links named VIC->X (VIC1-NSW1, V-SA, V-S-MNSP1) and EXPORTLIMIT for
T-V-MNSP1 (Basslink). Each half-hour takes the mean of its dispatch intervals (half-hour ending). Victorian demand is
total_demand_halfhour in gr4spdb, passed as a CSV (hh,total_demand) exported with psql:
  \\copy (SELECT settlement_date AS hh, total_demand FROM total_demand_halfhour WHERE settlement_date >= '2015-01-01'
          AND settlement_date < '2021-01-01' ORDER BY 1) TO 'dem_2015_2020.csv' CSV HEADER
Prints the median and 10th percentile capability per link and year in the top 5% demand half-hours, and the sum.
Earlier years have no dispatch limits on NEMweb (archive starts July 2009 and pre-2015 months are only whole-month
zips); interconnectors_VIC.csv holds the 2015-2020 values back to each link's start (see its header).
Usage: python scripts/data/interconnector_limits_vic.py CACHE_DIR DEMAND_CSV   (raw zips are cached, not committed)"""
import io, os, sys, zipfile
import pandas as pd
import requests

CACHE, DEMAND = sys.argv[1], sys.argv[2]
os.makedirs(CACHE, exist_ok=True)
LINKS = ["VIC1-NSW1", "V-SA", "V-S-MNSP1", "T-V-MNSP1"]
frames = []
for m in pd.period_range("2015-01", "2020-12", freq="M"):
    name = f"PUBLIC_DVD_DISPATCHINTERCONNECTORRES_{m.year}{m.month:02d}010000.zip"
    path = os.path.join(CACHE, name)
    if not os.path.exists(path):
        url = (f"https://nemweb.com.au/Data_Archive/Wholesale_Electricity/MMSDM/{m.year}/MMSDM_{m.year}_{m.month:02d}/"
               f"MMSDM_Historical_Data_SQLLoader/DATA/{name}")
        r = requests.get(url, timeout=300)
        r.raise_for_status()
        open(path, "wb").write(r.content)
    with zipfile.ZipFile(path) as z:
        raw = z.read(z.namelist()[0]).decode("utf-8", errors="replace")
    lines = raw.splitlines()
    head = next(l for l in lines if l.startswith("I,"))
    t = pd.read_csv(io.StringIO("\n".join([head] + [l for l in lines if l.startswith("D,")])))
    t = t[(t.INTERVENTION == 0) & t.INTERCONNECTORID.isin(LINKS)]
    frames.append(t[["SETTLEMENTDATE", "INTERCONNECTORID", "IMPORTLIMIT", "EXPORTLIMIT"]])
d = pd.concat(frames).drop_duplicates(["SETTLEMENTDATE", "INTERCONNECTORID"])
d["cap"] = d.EXPORTLIMIT.where(d.INTERCONNECTORID == "T-V-MNSP1", -d.IMPORTLIMIT)
d["hh"] = pd.to_datetime(d.SETTLEMENTDATE).dt.ceil("30min")
h = d.groupby(["hh", "INTERCONNECTORID"]).cap.mean().reset_index()
h = h.merge(pd.read_csv(DEMAND, parse_dates=["hh"]), on="hh")
h["y"] = h.hh.dt.year
peak = h[h.total_demand >= h.groupby("y").total_demand.transform(lambda x: x.quantile(0.95))]
for q, label in [(0.5, "median"), (0.1, "10th percentile")]:
    print(f"capability into VIC (MW), {label}, top 5% VIC demand half-hours")
    print(peak.pivot_table(index="y", columns="INTERCONNECTORID", values="cap",
                           aggfunc=lambda x: x.quantile(q)).round(0).to_string())
total = peak.pivot_table(index="hh", columns="INTERCONNECTORID", values="cap").sum(axis=1)
print("sum of links (MW), median by year:", total.groupby(total.index.year).median().round(0).to_dict())
