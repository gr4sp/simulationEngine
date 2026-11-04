"""Half-hourly shape of Victorian net exports for the netExportsShapeFile setting.

Monthly totals come from scripts/data/net_exports_opennem.py. (OpenElectricity's free static monthly file was
checked as a longer source: its VIC1 flows are zero before mid-2009, and from 2009 it is within a monthly
correlation of 0.92 of the OpenNEM extract, so the extract stays.)
Shape: AEMO MMSDM TRADINGINTERCONNECT (half-hourly METEREDMWFLOW) for 2015-01 to 2020-12 from
   NEMweb, the years AEMO archives as single tables. Victoria's net export in each half-hour is VIC1-NSW1 + V-SA +
   V-S-MNSP1 - T-V-MNSP1 (positive flow is in the direction of the name). The shape is the mean deviation of each
   half-hour from its month's mean, by calendar month and half-hour of day. Writes
   simulationSettings/net_exports_shape_VIC.csv. Applying it before 2015 is an assumption (the network changed:
   Basslink from 2006).
Usage: python scripts/data/net_exports_shape_vic.py CACHE_DIR   (raw zips are cached there, not in the repository)"""
import io, os, sys, zipfile
import pandas as pd
import requests

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), os.pardir, os.pardir)
CACHE = sys.argv[1]
os.makedirs(CACHE, exist_ok=True)
SIGN = {"VIC1-NSW1": 1, "V-SA": 1, "V-S-MNSP1": 1, "T-V-MNSP1": -1}

# 2. half-hourly shape
frames = []
for m in pd.period_range("2015-01", "2020-12", freq="M"):
    name = f"PUBLIC_DVD_TRADINGINTERCONNECT_{m.year}{m.month:02d}010000.zip"
    path = os.path.join(CACHE, name)
    if not os.path.exists(path):
        url = (f"https://nemweb.com.au/Data_Archive/Wholesale_Electricity/MMSDM/{m.year}/MMSDM_{m.year}_{m.month:02d}/"
               f"MMSDM_Historical_Data_SQLLoader/DATA/{name}")
        r = requests.get(url, timeout=300)
        r.raise_for_status()
        open(path, "wb").write(r.content)
    with zipfile.ZipFile(path) as z:
        raw = z.read(z.namelist()[0]).decode("utf-8", errors="replace")
    rows = [l for l in raw.splitlines() if l.startswith("D,")]
    head = next(l for l in raw.splitlines() if l.startswith("I,"))
    t = pd.read_csv(io.StringIO("\n".join([head] + rows)), header=0)
    t = t[t.INTERCONNECTORID.isin(SIGN.keys())]
    t["flow"] = t.METEREDMWFLOW * t.INTERCONNECTORID.map(SIGN)
    frames.append(t.groupby("SETTLEMENTDATE").flow.sum())
flow = pd.concat(frames)
flow.index = pd.to_datetime(flow.index) - pd.Timedelta(minutes=30)  # interval start
flow = flow[~flow.index.duplicated()].sort_index()
df = flow.to_frame("mw")
df["ym"] = df.index.to_period("M")
df["dev"] = df.mw - df.groupby("ym").mw.transform("mean")
df["month"] = df.index.month
df["slot"] = df.index.hour * 2 + df.index.minute // 30
shape = df.groupby(["month", "slot"]).dev.mean().rename("deviationMW").reset_index()
shape["source"] = "AEMO MMSDM TRADINGINTERCONNECT 2015-2020 mean deviation from the monthly mean"
shape.to_csv(os.path.join(ROOT, "simulationSettings", "net_exports_shape_VIC.csv"), index=False, float_format="%.1f")
print(f"shape from {len(df)} half-hours; deviation range {shape.deviationMW.min():.0f} to {shape.deviationMW.max():.0f} MW")
print(shape[shape.month == 1].set_index("slot").deviationMW.iloc[::6].round(0).to_string())
