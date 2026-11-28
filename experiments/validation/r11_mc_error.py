"""R11 (28 Nov 2026): Monte Carlo error of the reported validation statistics, and coverage of the seed band.

Practices from Morris, White and Crowther (2019), Using simulation studies to evaluate statistical methods, Statistics
in Medicine 38:2074-2102 (doi 10.1002/sim.8086), sections 4.1 and 5.2-5.4.

1. Monte Carlo SE. Each reported statistic is a median or a 5th/95th percentile over the seeds of a per-seed value
   (NSE, bias, RMSE, r, MAE, NRMSE; the price-duration shares; the index NSE), or a statistic of the median series over
   the seeds (index bias and r). Its Monte Carlo SE is the standard deviation of that summary over B bootstrap
   resamples of the seeds (with replacement, same size). Percentiles as in seed_ensemble.py (linear interpolation).
   Each cell is flagged when its SE exceeds half a unit of the last decimal the article prints.
2. Seeds needed. The SE of a median or percentile falls as 1/sqrt(seeds), so the seeds that give an SE of t are
   about seeds x (SE / t)^2. Reported for t = half a unit of the printed decimal, and for 0.05 (R11 point 2).
3. Coverage of the seed band. For each indicator, on the same series as the tables (validation_statistics.series and
   fuel_series, emissions on July-June years as seed_ensemble.py, intensity as emissions_index.py), the periods in
   which the record lies inside the 5-95 % band of the seeds, and the periods outside with their side. This is the
   spread of one parameter set over its random draws, not a prediction interval.

Inputs: ENSEMBLE_DIR as written by seed_ensemble.py and the scripts after it (seed_statistics.csv,
price_basis_statistics.csv, seed_price_duration.csv, seed_halfhour_years.csv, record_price_duration.csv,
record_halfhour_years.csv), and its local run folders run_s<seed>/ (for points 1, index, and 3).

Usage: python experiments/validation/r11_mc_error.py ENSEMBLE_DIR [B=2000]
Writes ENSEMBLE_DIR/r11_mc_error.csv, r11_coverage.csv and r11_mc_error.md.
"""
import glob
import os
import re
import sys

import numpy as np
import pandas as pd

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import validation_statistics as vs  # noqa: E402
import emissions_year_basis as eyb  # noqa: E402
import emissions_index as ei  # noqa: E402

BOOT_SEED = 20261128
Q = {"median": 0.5, "p05": 0.05, "p95": 0.95}

# decimals the article prints (gr4sp_methodsx.md, papers 7ceeef5): table -> {row: {metric: decimals}}
D2, D1 = 2, 1
STATS_ROWS = {  # tbl:stats: indicator in seed_statistics.csv -> (article row, decimals of Bias and RMSE)
    "Wholesale price (monthly)": ("Wholesale price, monthly", D1),
    "Wholesale price (annual)": ("Wholesale price, annual", D1),
    "RE share (annual)": ("Renewable energy share, annual", D2),
    "Tariffs vs Oakley Greenwood": ("Tariff, against Oakley Greenwood", D1),
    "Tariffs vs St Vincent de Paul": ("Tariff, against St Vincent de Paul", D1),
    "GHGE (annual, July-June)": ("Emissions, 2005-2018", D2),
    "GHGE (annual, July-June, full)": ("Emissions, 2000-2018", D2),
}
FUEL_ROWS = {
    "Coal generation (monthly)": ("Brown coal", D1), "Gas generation (monthly)": ("Gas", D1),
    "Wind generation (monthly)": ("Wind", D1), "Hydro generation (monthly, input)": ("Hydro", D1),
    "Rooftop PV generation (monthly, input)": ("Rooftop solar", D1), "RE share (monthly)": ("Renewable energy share, monthly", D2),
}
DURATION_PERIODS = ["1999-2008", "2009-2016", "2017-2019", "1999-2019"]


def boot(values, fn, rng, B):
    """SE of fn(sample) over B resamples of the rows (seeds) of `values` (1-D array, or 2-D with seeds on axis 1)."""
    v = np.asarray(values, float)
    n = v.shape[-1]
    idx = rng.integers(0, n, size=(B, n))
    reps = np.array([fn(v[..., i]) for i in idx])
    return reps.std(axis=0, ddof=1)


def per_seed_summaries(x, rng, B):
    x = np.asarray(x, float)
    x = x[~np.isnan(x)]
    out = {}
    for k, q in Q.items():
        out[k] = np.quantile(x, q)
        out[k + "_se"] = boot(x, lambda s, q=q: np.quantile(s, q), rng, B)
    out["seeds"] = len(x)
    return out


def flag(se, dec):
    half = 0.5 * 10.0 ** (-dec)
    return se > half


def rows_from_seed_statistics(ens, rng, B):
    st = pd.read_csv(os.path.join(ens, "seed_statistics.csv"))
    rows = []
    for table, spec, metrics in (("tbl:stats", STATS_ROWS, ("NSE", "Bias", "RMSE", "r")),
                                 ("tbl:fuel", FUEL_ROWS, ("NSE", "Bias", "RMSE", "r")),
                                 ("supplement", {**STATS_ROWS, **FUEL_ROWS}, ("MAE", "NRMSE"))):
        for ind, (label, dec) in spec.items():
            g = st[st.indicator == ind]
            for m in metrics:
                d = D2 if m in ("NSE", "r", "NRMSE") else dec
                rows.append({"table": table, "row": label, "indicator": ind, "metric": m, "decimals": d,
                             **per_seed_summaries(g[m].values, rng, B)})
    pb = pd.read_csv(os.path.join(ens, "price_basis_statistics.csv"))
    for (case, scale), g in pb.groupby(["case", "scale"], sort=False):
        for m in ("NSE", "Bias", "RMSE"):
            rows.append({"table": "text (price basis)", "row": f"{case}, {scale}", "indicator": "price", "metric": m,
                         "decimals": D2 if m == "NSE" else D1, **per_seed_summaries(g[m].values, rng, B)})
    return rows


def duration_per_seed(ens):
    """Per seed and period: % of half-hours below 15 $/MWh, median half-hour price, % above 300 $/MWh."""
    c = pd.read_csv(os.path.join(ens, "seed_price_duration.csv"))
    hy = pd.read_csv(os.path.join(ens, "seed_halfhour_years.csv"))
    rows = []
    for (seed, period), g in c.groupby(["seed", "period"]):
        g = g.sort_values("exceedance_pct")
        exc, price = g.exceedance_pct.values, g.price.values  # price falls as exceedance rises
        below15 = 100.0 - np.interp(15.0, price[::-1], exc[::-1])
        med = np.interp(50.0, exc, price)
        a, b = map(int, period.split("-"))
        y = hy[(hy.seed == seed) & (hy.year >= a) & (hy.year <= b)]
        hh = np.where(pd.Series(range(a, b + 1)).map(lambda yr: yr % 4 == 0).values, 17568, 17520)
        share = y.set_index("year").share_above_300.reindex(range(a, b + 1)).values
        above300 = 100.0 * np.nansum(share * hh) / hh[~np.isnan(share)].sum()
        rows.append({"seed": seed, "period": period, "below15": below15, "median_price": med, "above300": above300})
    return pd.DataFrame(rows)


def duration_record(ens):
    r = pd.read_csv(os.path.join(ens, "record_price_duration.csv"))
    out = {}
    for period, g in r.groupby("period"):
        g = g.sort_values("exceedance_pct")
        exc, price = g.exceedance_pct.values, g.price.values
        out[period] = {"below15": 100.0 - np.interp(15.0, price[::-1], exc[::-1]), "median_price": np.interp(50.0, exc, price)}
    return out


def rows_duration(ens, rng, B):
    d = duration_per_seed(ens)
    rows = []
    for period in DURATION_PERIODS:
        g = d[d.period == period]
        for m, dec in (("below15", 0), ("median_price", 0), ("above300", 2)):
            rows.append({"table": "tbl:duration", "row": period, "indicator": "half-hourly price", "metric": m,
                         "decimals": dec, **per_seed_summaries(g[m].values, rng, B)})
    return rows


def index_series(ens):
    files = sorted(glob.glob(os.path.join(ens, "run_s*", "csv", "VIC", "*MonthlySummary*.csv")))
    idx, sysi = {}, {}
    for f in files:
        seed = int(re.search(r"run_s(\d+)", f).group(1))
        _, fy = ei.run_series(f)
        idx[seed], sysi[seed] = fy["index"], fy.system
    yrs = list(range(ei.FIRST_FY, ei.LAST_FY + 1))
    idx, sysi = pd.DataFrame(idx), pd.DataFrame(sysi)
    return idx.loc[idx.index.isin(yrs)], sysi.loc[sysi.index.isin(yrs)]


def rows_index(ens, rng, B):
    idx, sysi = index_series(ens)
    n, c = ei.nga(), ei.cdeii()
    rows = []
    for label, sim, obs in (("Recursive intensity vs NGA scope 2 plus losses", idx, n.scope2_plus_losses),
                            ("System intensity vs NGA scope 2", sysi, n.scope2),
                            ("System intensity vs AEMO CDEII", sysi, c.cdeii)):
        o = obs.reindex(sim.index).dropna()
        s = sim.loc[o.index]
        nse = [vs.statistics(s[k], o)["NSE"] for k in s.columns]
        rows.append({"table": "tbl:index", "row": label, "indicator": "intensity", "metric": "NSE", "decimals": D2,
                     **per_seed_summaries(nse, rng, B)})
        arr = s.values  # years x seeds

        def med_stats(a):
            m = np.median(a, axis=1)
            return np.array([100 * (m / o.values - 1).mean(), np.corrcoef(m, o.values)[0, 1]])
        point = med_stats(arr)
        se = boot(arr, med_stats, rng, B)
        for j, (m, dec) in enumerate((("mean relative bias (%)", 1), ("r", D2))):
            rows.append({"table": "tbl:index", "row": label, "indicator": "intensity (median series)", "metric": m,
                         "decimals": dec, "median": point[j], "median_se": se[j], "seeds": arr.shape[1]})
    return rows, idx, sysi


def coverage(ens):
    """Record inside the 5-95 % seed band, period by period, on the tables' series."""
    runs = sorted(glob.glob(os.path.join(ens, "run_s*")), key=lambda p: int(p.rsplit("_s", 1)[1]))
    sims, obs, periods = {}, {}, {}
    for d in runs:
        seed = int(d.rsplit("_s", 1)[1])
        year, month, ghge, opennem, tariffs = vs.load_inputs(d)
        ind = vs.series(year, month, ghge, opennem, tariffs)
        ind.update(vs.fuel_series(month, opennem))
        mp = vs.monthly_prices(month)
        lab = {"Wholesale price (monthly)": mp.index.strftime("%Y-%m"),
               "Wholesale price (annual)": mp.resample("YE").mean().index.year.astype(str)}
        months = month.index[month.index.isin(opennem.index)].strftime("%Y-%m")
        for k in vs.FUEL_MONTHLY:
            lab[k] = months
        lab["RE share (monthly)"] = months
        lab["RE share (annual)"] = vs.renewable_share(opennem).index.year.astype(str)
        for k, col in (("Tariffs vs Oakley Greenwood", 1), ("Tariffs vs St Vincent de Paul", 0)):
            lab[k] = [str(y) for y in vs.tariff_years(tariffs, col)[0]]
        fy = eyb.years_for(d).july_june
        inv = ghge["Hist_GHGE_MtCO2e"]
        for name, (lo, hi) in (("GHGE (annual, July-June)", (2005, 2018)), ("GHGE (annual, July-June, full)", (2000, 2018))):
            r = range(lo, hi + 1)
            ind[name] = (fy.reindex(r).values, inv.reindex(r).values, f"{lo}-{hi}", "Mt")
            lab[name] = [str(y) for y in r]
        for k, (s, o, per, unit) in ind.items():
            if k in ("GHGE (annual)", "GHGE (annual, full)"):
                continue  # calendar-year emissions: supplement only, not in the article's tables
            sims.setdefault(k, {})[seed] = np.asarray(s, float)
            obs[k] = np.asarray(o, float)
            periods[k] = list(lab.get(k)) if lab.get(k) is not None else [f"{per} #{i + 1}" for i in range(len(o))]
    tables = {}
    for k in sims:
        w = pd.DataFrame(sims[k])
        tables[k] = (w, obs[k], periods[k])
    idx, sysi = index_series(ens)
    n, c = ei.nga(), ei.cdeii()
    for label, sim, o in (("Recursive intensity vs NGA scope 2 plus losses", idx, n.scope2_plus_losses),
                          ("System intensity vs NGA scope 2", sysi, n.scope2),
                          ("System intensity vs AEMO CDEII", sysi, c.cdeii)):
        o = o.reindex(sim.index).dropna()
        s = sim.loc[o.index]
        tables[label] = (s.reset_index(drop=True), o.values, [str(y) for y in o.index])
    rows = []
    for k, (w, o, per) in tables.items():
        lo, hi = w.quantile(0.05, axis=1).values, w.quantile(0.95, axis=1).values
        for i, p in enumerate(per):
            if np.isnan(o[i]) or np.isnan(lo[i]):
                continue
            side = "inside" if lo[i] <= o[i] <= hi[i] else ("above" if o[i] > hi[i] else "below")
            rows.append({"indicator": k, "period": p, "record": o[i], "p05": lo[i], "median": np.median(w.values[i]),
                         "p95": hi[i], "record_is": side})
    return pd.DataFrame(rows)


def main():
    ens = sys.argv[1]
    B = int(sys.argv[2]) if len(sys.argv) > 2 else 2000
    rng = np.random.default_rng(BOOT_SEED)
    rows = rows_from_seed_statistics(ens, rng, B) + rows_duration(ens, rng, B)
    ir, _, _ = rows_index(ens, rng, B)
    mc = pd.DataFrame(rows + ir)
    for k in Q:
        se = mc.get(k + "_se")
        if se is None:
            continue
        mc[k + "_flag"] = [bool(flag(s, d)) if pd.notna(s) else None for s, d in zip(se, mc.decimals)]
    n = mc.seeds
    worst = mc[["median_se", "p05_se", "p95_se"]].max(axis=1)
    half = 0.5 * 10.0 ** (-mc.decimals)
    # decimals the MCSE supports: the most with MCSE <= half a unit of the last one (negative: tens, hundreds)
    mc["decimals_supported"] = np.floor(-np.log10(2 * worst.clip(lower=1e-12))).astype(int).clip(upper=6)
    mc["seeds_for_printed_decimal"] = np.ceil(n * (worst / half) ** 2).astype("Int64")
    mc["seeds_for_se_0.05"] = np.ceil(n * (worst / 0.05) ** 2).astype("Int64")
    mc.round(6).to_csv(os.path.join(ens, "r11_mc_error.csv"), index=False)
    cov = coverage(ens)
    cov.round(6).to_csv(os.path.join(ens, "r11_coverage.csv"), index=False)

    f = lambda v, d: "" if pd.isna(v) else f"{v:.{max(int(d), 0) + 1}f}"
    L = [f"# R11: Monte Carlo error and coverage of the seed band ({os.path.basename(os.path.normpath(ens))})", "",
         f"Written by `experiments/validation/r11_mc_error.py`. {int(mc.seeds.max())} seeds; bootstrap of the seeds, "
         f"B = {B} resamples, numpy default_rng({BOOT_SEED}). Monte Carlo SE (MCSE) = standard deviation of the summary "
         "over the resamples. 'Flag' = MCSE above half a unit of the last decimal the article prints. Seeds needed: "
         "seeds x (largest MCSE of the row / target)^2. 'Decimals supported': the most decimals with the row's largest "
         "MCSE at most half a unit of the last one (0: whole units). A band of width 0 (an indicator the seeds do not "
         "move: wind, which runs at a constant capacity factor) cannot cover the record by construction. Units: NSE and r none; Bias, RMSE and MAE in the indicator's "
         "unit (price $/MWh, tariff c/kWh, emissions Mt CO2-e, generation GWh, shares pp); NRMSE and the duration "
         "shares in %; median half-hourly price $/MWh.", "",
         "## 1-2. Monte Carlo SE of every reported value", "",
         "| Table | Row | Metric | Printed decimals | Median (MCSE) | 5th percentile (MCSE) | 95th percentile (MCSE) | Flag | Decimals supported | Seeds for printed decimal | Seeds for MCSE 0.05 |",
         "|---|---|---|---|---|---|---|---|---|---|---|"]
    for _, r in mc.iterrows():
        d = r.decimals
        cell = lambda k: (f"{f(r[k], d)} ({r[k + '_se']:.4f})" if pd.notna(r.get(k)) and pd.notna(r.get(k + '_se')) else "-")
        flags = [k for k in Q if r.get(k + "_flag") is True]
        L.append(f"| {r.table} | {r.row} | {r.metric} | {int(d)} | {cell('median')} | {cell('p05')} | {cell('p95')} | "
                 f"{', '.join(flags) or '-'} | {r.decimals_supported} | {r.seeds_for_printed_decimal} | {r['seeds_for_se_0.05']} |")
    L += ["", "### Largest MCSE per column of each table", "",
          "| Table | Metric | Largest MCSE, median | Largest MCSE, 5th percentile | Largest MCSE, 95th percentile |",
          "|---|---|---|---|---|"]
    for (t, m), g in mc.groupby(["table", "metric"], sort=False):
        mx = lambda k: f"{g[k].max():.4f} ({g.loc[g[k].idxmax(), 'row']})" if k in g and g[k].notna().any() else "-"
        L.append(f"| {t} | {m} | {mx('median_se')} | {mx('p05_se')} | {mx('p95_se')} |")
    L += ["", "## 3. Coverage: record inside the 5-95 % band of the seeds", "",
          "| Indicator | Periods (count) | Inside (count) | Inside (%) | Record above the band (count) | Record below the band (count) | Periods outside, above | Periods outside, below |",
          "|---|---|---|---|---|---|---|---|"]
    for k, g in cov.groupby("indicator", sort=False):
        ab, be = g[g.record_is == "above"].period.tolist(), g[g.record_is == "below"].period.tolist()
        short = lambda xs: ", ".join(xs) if len(xs) <= 12 else ", ".join(xs[:12]) + f", ... ({len(xs)} in all; r11_coverage.csv)"
        ins = (g.record_is == "inside").sum()
        L.append(f"| {k} | {len(g)} | {ins} | {100 * ins / len(g):.0f} | {len(ab)} | {len(be)} | {short(ab) or '-'} | {short(be) or '-'} |")
    open(os.path.join(ens, "r11_mc_error.md"), "w", encoding="utf-8", newline="\n").write("\n".join(L) + "\n")
    print("\n".join(L))


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    main()
