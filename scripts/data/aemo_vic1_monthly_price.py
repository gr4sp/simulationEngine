"""Monthly VIC1 wholesale price from AEMO's aggregated price and demand files, December 1998 to June 2020.

Input: the 259 monthly files PRICE_AND_DEMAND_YYYYMM_VIC1.csv (columns REGION, SETTLEMENTDATE, TOTALDEMAND, RRP,
PERIODTYPE) from AEMO's "Aggregated price and demand data" page (aemo.com.au, NEM data dashboard). AEMO's site
blocks scripted downloads, so the files are downloaded by hand and kept outside the repository
(Sandbox/papers/GR4SPArticle_ERSS/data/aemo_price_demand_VIC1/).

Each half-hour is assigned to the month in which it starts (SETTLEMENTDATE marks the interval end). Per month:
  vwp  = sum(RRP x TOTALDEMAND) / sum(TOTALDEMAND)   demand-weighted price, the validation comparator (request R2)
  mean = mean(RRP)                                    time-weighted price
  n    = number of half-hours
Writes experiments/simulationData/aemo_VIC1_monthly_vwp_199812_202006.csv.
Usage: python scripts/data/aemo_vic1_monthly_price.py RAW_DIR"""
import glob, os, sys
import pandas as pd

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), os.pardir, os.pardir)
files = sorted(glob.glob(os.path.join(sys.argv[1], "PRICE_AND_DEMAND_*_VIC1.csv")))
d = pd.concat(pd.read_csv(f) for f in files)
d = d[d.REGION == "VIC1"].copy()
d["end"] = pd.to_datetime(d.SETTLEMENTDATE, format="mixed")  # files use both 'yyyy/mm/dd HH:MM' and ':SS'
d = d.drop_duplicates("end")
start = d.end - pd.Timedelta(minutes=30)
d["m"] = start.dt.to_period("M").dt.to_timestamp()
d["rd"] = d.RRP * d.TOTALDEMAND
g = d.groupby("m")
out = pd.DataFrame({"vwp": g.rd.sum() / g.TOTALDEMAND.sum(), "mean": g.RRP.mean(), "n": g.size()})
out = out.loc[:"2020-06-01"]
out.to_csv(os.path.join(ROOT, "experiments", "simulationData", "aemo_VIC1_monthly_vwp_199812_202006.csv"),
           float_format="%.4f", date_format="%Y-%m-%d")
print(f"{len(files)} files, {len(d)} half-hours, months {out.index[0]:%Y-%m} to {out.index[-1]:%Y-%m}")
