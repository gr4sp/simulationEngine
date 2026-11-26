"""R6 finding 3 (26 Nov 2026): the tariff's wholesale term is the all-markets price, not the primary price alone.

EndUserUnit.computeTariff averages the last six months of Arena.getWholesalePrice: the primary price and the off-market
(LCOE-rule) price weighted by their mean demand. The monthly prices file holds it as "Tariff_WA_AllMarkets ($/MWh)"; the
monthly summary holds the primary price, "Primary Wholesale ($/MWh)". This writes, per year 1999-2019, the median and
5-95 % over the seeds of (all-markets - primary), as yearly means of the monthly values.

Usage: python experiments/validation/tariff_wholesale_basis.py ENSEMBLE_DIR   (run folders run_s<seed>/csv/VIC/)
Writes ENSEMBLE_DIR/tariff_wholesale_basis.csv (per seed and year) and tariff_wholesale_basis.md (summary).
"""
import glob, os, re, sys
import pandas as pd

out = sys.argv[1]
rows = []
for d in sorted(glob.glob(os.path.join(out, "run_s*"))):
    seed = int(re.search(r"run_s(\d+)$", d).group(1))
    p = glob.glob(os.path.join(d, "csv", "VIC", "VICSimDataMonthlyPrices_seed_*.csv"))
    m = glob.glob(os.path.join(d, "csv", "VIC", "VICSimDataMonthlySummary_seed_*.csv"))
    if not (p and m):
        continue
    p = pd.read_csv(p[0]).rename(columns={"Date": "t"})
    m = pd.read_csv(m[0]).rename(columns={"Time (Month)": "t"})
    x = p[["t", "Tariff_WA_AllMarkets ($/MWh)"]].merge(m[["t", "Primary Wholesale ($/MWh)"]], on="t")
    x["year"] = x.t.str[:4].astype(int)
    x = x[(x.year >= 1999) & (x.year <= 2019)].dropna()
    y = x.groupby("year")[["Tariff_WA_AllMarkets ($/MWh)", "Primary Wholesale ($/MWh)"]].mean()
    y["difference ($/MWh)"] = y.iloc[:, 0] - y.iloc[:, 1]
    rows.append(y.assign(seed=seed).reset_index())
D = pd.concat(rows)
D.to_csv(os.path.join(out, "tariff_wholesale_basis.csv"), index=False)
g = D.groupby("year")["difference ($/MWh)"]
s = pd.DataFrame({"median": g.median(), "p05": g.quantile(0.05), "p95": g.quantile(0.95)})
seed1 = D[D.seed == 1].set_index("year")["difference ($/MWh)"]
lines = [f"# Tariff wholesale term: all-markets price minus primary price ($/MWh), yearly means, {D.seed.nunique()} seeds\n",
         f"Written by `experiments/validation/tariff_wholesale_basis.py {os.path.basename(os.path.normpath(out))}`. "
         "Negative: the off-market (LCOE-rule) price is below the primary price. Divide by 10 for c/kWh before GST.\n",
         "| year | median | 5 % | 95 % | seed 1 |", "|---|---|---|---|---|"]
for yv, r in s.iterrows():
    lines.append(f"| {yv} | {r['median']:+.2f} | {r['p05']:+.2f} | {r['p95']:+.2f} | {seed1.get(yv, float('nan')):+.2f} |")
open(os.path.join(out, "tariff_wholesale_basis.md"), "w", encoding="utf-8").write("\n".join(lines) + "\n")
print("\n".join(lines))
