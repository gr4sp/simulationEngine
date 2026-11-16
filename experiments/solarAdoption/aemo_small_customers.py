"""Victorian small-customer half-hourly load from AEMO settlement profiles (solar step 3, D4b: seasonal and weather shape).

Sources (AEMO, downloaded Nov 2026 to Sandbox/papers/GR4SPArticle_ERSS/data/load_profiles/):
  NSLP, Net System Load Profile, yearly files 2002-2020 (no 2008 file): per distribution network (profile area) and
    day, energy in each half-hour (kWh). For Victoria it is the energy entering the network (plus embedded
    generation) minus every interval-metered load (AEMO, Understanding load profiles published from MSATS, v6.1,
    2021, App. A). Before the Victorian smart-meter (AMI) rollout it is the load of the small customers on
    accumulation meters (homes and small businesses); as AMI meters became interval meters (2010-2013) it shrinks to
    a residual of the few remaining basic meters and unaccounted energy.
  MRIM (manually read interval meter) data, quarterly, Jul 2018-Dec 2020 (no Apr-Jun 2020): per network and day,
    half-hourly energy (kWh) of all type 5 sites (mostly the Victorian AMI meters), each under 160 MWh a year.
Victorian profile areas: CITIPOWER (CitiPower), POWERCOR (Powercor), TXU (AusNet Services), UNITED (United Energy),
VICAGL (Jemena). A day that appears more than once keeps its latest version (CreationDT).

Output: <out>/aemo_small_customers_vic.csv.gz (source, area, date, p01..p48 kWh) and a yearly summary on screen.
Usage: python aemo_small_customers.py LOAD_PROFILES_DIR [--out DIR]
"""
import argparse, glob, io, os, zipfile
import pandas as pd

VIC = {"CITIPOWER": "CitiPower", "POWERCOR": "Powercor", "TXU": "AusNet", "UNITED": "United", "VICAGL": "Jemena"}


def parse_dates(s):
    s = s.astype(str).str.strip()
    ymd = s.str.match(r"^\d{4}[/-]")
    out = pd.Series(pd.NaT, index=s.index)
    out[ymd] = pd.to_datetime(s[ymd], format="mixed", yearfirst=True)
    out[~ymd] = pd.to_datetime(s[~ymd], format="mixed", dayfirst=True)
    return out


def nslp(d):
    rows = []
    for z in sorted(glob.glob(os.path.join(d, "*"))):
        b = os.path.basename(z)
        if not b.lower().endswith(".zip") or b.startswith(("VIC_", "Frozen")):
            continue
        with zipfile.ZipFile(z) as f:
            name = [n for n in f.namelist() if "nslp" in n.lower() or "nlsp" in n.lower()][0]
            t = pd.read_csv(io.BytesIO(f.read(name)))
        t.columns = [c.strip() for c in t.columns]
        t = t[t.ProfileArea.isin(VIC)]
        per = [c for c in t.columns if c.lower().startswith("period")]
        assert len(per) == 48, (b, per)
        t = t.assign(date=parse_dates(t.SettlementDate), created=parse_dates(t.CreationDT.astype(str).str[:10]),
                     file=b)
        t = t.rename(columns=dict(zip(per, [f"p{i:02d}" for i in range(1, 49)])))
        rows.append(t[["ProfileArea", "date", "created", "file"] + [f"p{i:02d}" for i in range(1, 49)]])
    n = pd.concat(rows).sort_values(["ProfileArea", "date", "created"])
    n = n.drop_duplicates(["ProfileArea", "date"], keep="last")
    return n.rename(columns={"ProfileArea": "area"}).assign(source="NSLP")


def mrim(d):
    rows = []
    for z in sorted(glob.glob(os.path.join(d, "VIC_*.zip"))):
        with zipfile.ZipFile(z) as f:
            t = pd.read_csv(f.open(f.namelist()[0]))
        t = t[t.DCTC.str.strip() == "MRIM"]
        vals = [c for c in t.columns if c.startswith("VAL")]
        assert len(vals) == 48
        assert ((t[vals].sum(axis=1) / t.DAILYT - 1).abs() < 1e-6).all(), z
        t = t.assign(date=parse_dates(t.SETTD), file=os.path.basename(z))
        t = t.rename(columns=dict(zip(vals, [f"p{i:02d}" for i in range(1, 49)])))
        rows.append(t[["PROFILEAREA", "date", "file"] + [f"p{i:02d}" for i in range(1, 49)]])
    m = pd.concat(rows).rename(columns={"PROFILEAREA": "area"}).drop_duplicates(["area", "date"], keep="last")
    return m.assign(source="MRIM")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dir")
    ap.add_argument("--out", default=None)
    a = ap.parse_args()
    out = a.out or os.path.join(a.dir, "derived")
    os.makedirs(out, exist_ok=True)
    allp = pd.concat([nslp(a.dir), mrim(a.dir)], ignore_index=True)
    p = [f"p{i:02d}" for i in range(1, 49)]
    allp["kwh"] = allp[p].sum(axis=1)
    allp["year"] = allp.date.dt.year
    days = allp.pivot_table(index="year", columns=["source", "area"], values="kwh", aggfunc="count")
    gwh = allp.pivot_table(index="year", columns=["source", "area"], values="kwh", aggfunc="sum") / 1e6
    pd.set_option("display.width", 250)
    print("days per year:\n", days.fillna(0).astype(int).to_string())
    print("\nGWh per year (incomplete years are partial):\n", gwh.round(0).to_string())
    allp[["source", "area", "date"] + p].assign(date=allp.date.dt.strftime("%Y-%m-%d")).to_csv(
        os.path.join(out, "aemo_small_customers_vic.csv.gz"), index=False, float_format="%.3f")
    print("written", os.path.join(out, "aemo_small_customers_vic.csv.gz"), len(allp), "rows")


if __name__ == "__main__":
    main()
