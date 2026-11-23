"""Price duration curves over the seed ensemble, and the recorded curve (R6, 19 Nov 2026).

A price duration curve ranks every half-hour of a period from the highest price to the lowest. It tests the
distribution of prices (how often prices are low, middling or spiking), which the monthly and annual means hide, and
needs no half-hour-by-half-hour match: the model's demand and weather follow the record, but its breakdowns are random.

Each seed is rerun with the half-hourly diagnostic log of SpotMarket (-Dgr4sp.priceSetterLog), using the compiled
classes and settings kept with the seed ensemble, so the runs are the ensemble's runs (checked: seed 42 writes the same
monthly summary byte for byte). Only summaries are kept; the logs (~60 MB each) are deleted.

Usage: python seed_price_duration.py ENSEMBLE_RUN_DIR OUT_DIR [N_SEEDS=100] [PARALLEL=10]
  ENSEMBLE_RUN_DIR   seed_ensemble.py's OUT_DIR, holding cls/ and settings/
  OUT_DIR            gets seed_price_duration.csv (seed, period, exceedance %, price $/MWh),
                     seed_halfhour_years.csv (seed, year: mean price, share of half-hours above 300 $/MWh, imported GWh,
                     share of half-hours each fuel sets the price) and record_price_duration.csv (AEMO VIC1)
"""
import concurrent.futures as cf, io, os, shutil, subprocess, sys
import numpy as np, pandas as pd

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, os.pardir, os.pardir))
PSQL = r"C:/Program Files/PostgreSQL/18/bin/psql.exe"
FIRST, LAST = 1999, 2019
# exceedance levels, % of half-hours with a price at or above the value: fine steps in the spike tail
EXCEED = np.round(np.concatenate([np.arange(0, 1, 0.01), np.arange(1, 100.01, 0.5)]), 2)
PERIODS = {"1999-2019": (1999, 2019), "1999-2008": (1999, 2008), "2009-2016": (2009, 2016), "2017-2019": (2017, 2019)}


def curve(prices):
    """Price exceeded in the given % of half-hours (exceedance 0 % = the maximum)."""
    return np.quantile(prices, 1 - EXCEED / 100.0)


def curves(df, seed):
    rows = []
    for name, (a, b) in PERIODS.items():
        p = df.loc[(df.year >= a) & (df.year <= b), "price"].values
        rows.append(pd.DataFrame({"seed": seed, "period": name, "exceedance_pct": EXCEED, "price": curve(p)}))
    return pd.concat(rows)


def run(ens, seed, tmp):
    d = os.path.join(tmp, f"pdc_s{seed}")
    shutil.rmtree(d, ignore_errors=True)
    for sub in ("csv", "plots", "logs"):
        os.makedirs(os.path.join(d, sub))
    shutil.copytree(os.path.join(ens, "settings"), os.path.join(d, "simulationSettings"))
    log = os.path.join(d, "ps.csv")
    cp = f"{os.path.join(ens, 'cls')};{REPO}/build/resources/main;{REPO}/build/runtime-libs/*"
    with open(os.path.join(d, "stdout.txt"), "w") as out:
        r = subprocess.run(["java", "-Xmx3g", f"-Dgr4sp.priceSetterLog={log}", f"-Dgr4sp.priceSetterLog.from={FIRST}",
                            f"-Dgr4sp.priceSetterLog.to={LAST}", "-cp", cp, "core.Gr4spSim", "-seed", str(seed)],
                           cwd=d, stdout=out, stderr=subprocess.STDOUT)
    if r.returncode != 0 or not os.path.exists(log):
        return seed, None, None
    df = pd.read_csv(log, usecols=["time", "market", "price", "fuel", "importMW", "unmet"])
    df = df[df.market == "Primary"]
    df["year"] = df.time.str[:4].astype(int)
    df["setter"] = np.where(df.importMW > 0, "Imports", df.fuel.fillna(""))
    df.loc[df.unmet > 0, "setter"] = "Unserved"
    g = df.groupby("year")
    yrs = pd.DataFrame({"seed": seed, "mean_price": g.price.mean(), "share_above_300": g.price.apply(lambda p: (p > 300).mean()),
                        "imports_gwh": g.importMW.sum() * 0.5 / 1000})
    share = pd.crosstab(df.year, df.setter, normalize="index").add_prefix("setter_")
    yrs = yrs.join(share).reset_index()
    pdc = curves(df, seed)
    shutil.rmtree(d, ignore_errors=True)
    return seed, pdc, yrs


def record():
    q = (f"select settlement_date, price from total_demand_halfhour where settlement_date >= '{FIRST}-01-01' "
         f"and settlement_date < '{LAST + 1}-01-01' order by 1")
    txt = subprocess.run([PSQL, "-h", "localhost", "-U", "postgres", "-d", "gr4spdb", "-A", "-F,", "-P", "footer=off",
                          "-c", q], capture_output=True, text=True, check=True).stdout
    df = pd.read_csv(io.StringIO(txt), parse_dates=["settlement_date"])
    df["year"] = df.settlement_date.dt.year
    return curves(df, "record").drop(columns="seed"), df


def record_years(raw):
    """Recorded half-hour price by year (unweighted mean, as the model's mean_price in seed_halfhour_years.csv)."""
    g = raw.groupby("year").price
    return pd.DataFrame({"mean_price": g.mean(), "halfhours": g.size(),
                         "share_above_300": g.apply(lambda p: (p > 300).mean())}).reset_index()


def main():
    if sys.argv[1] == "record":
        # python seed_price_duration.py record OUT: only the recorded side (record_halfhour_years.csv), no runs
        record_years(record()[1]).to_csv(os.path.join(sys.argv[2], "record_halfhour_years.csv"), index=False)
        return
    ens, out = sys.argv[1], sys.argv[2]
    n = int(sys.argv[3]) if len(sys.argv) > 3 else 100
    par = int(sys.argv[4]) if len(sys.argv) > 4 else 10
    tmp = os.path.join(ens, "pdc_tmp")
    os.makedirs(tmp, exist_ok=True)
    rec, raw = record()
    rec.to_csv(os.path.join(out, "record_price_duration.csv"), index=False)
    record_years(raw).to_csv(os.path.join(out, "record_halfhour_years.csv"), index=False)
    print(f"record: {len(raw)} half-hours {FIRST}-{LAST}, share above 300 $/MWh {(raw.price > 300).mean():.4f}")
    pdcs, yrs = [], []
    with cf.ThreadPoolExecutor(par) as ex:
        for seed, pdc, yr in ex.map(lambda s: run(ens, s, tmp), range(1, n + 1)):
            print(f"seed {seed}: {'ok' if pdc is not None else 'FAILED'}", flush=True)
            if pdc is not None:
                pdcs.append(pdc)
                yrs.append(yr)
    pd.concat(pdcs).to_csv(os.path.join(out, "seed_price_duration.csv"), index=False)
    pd.concat(yrs).fillna(0).to_csv(os.path.join(out, "seed_halfhour_years.csv"), index=False)


if __name__ == "__main__":
    main()
