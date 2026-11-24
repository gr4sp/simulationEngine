"""Emissions compared on the inventory's own year: July to June (23 Nov 2026).

The Victorian inventory (State and Territory Greenhouse Gas Inventories, as published in the Victorian Greenhouse Gas
Emissions Reports) labels financial years: "2017" is 1 July 2016 to 30 June 2017. The model's year summary is a
calendar year. Hazelwood closed on 31 March 2017, so inventory "2017" has it for nine months and model 2017 for three.

This script sums each run's monthly system emissions (column "System GHG Emissions (tCO2-e)" of the monthly summary,
written since 23 Nov 2026; each generator's monthly output x its emission factor, so the months add up to the year
summary exactly) over July-June years, and compares both bases with the inventory over the same windows.

Usage: python emissions_year_basis.py ENSEMBLE_DIR
  writes ENSEMBLE_DIR/emissions_year_basis.csv (per seed, year: calendar and July-June Mt)
         ENSEMBLE_DIR/emissions_year_basis.md  (median and 5-95 % of NSE, bias, RMSE over the seeds, both bases)
"""
import glob
import os
import sys

import numpy as np
import pandas as pd

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import validation_statistics as vs  # noqa: E402

COL = "System GHG Emissions (tCO2-e)"
WINDOWS = ((2005, 2018), (1999, 2018))  # as in the validation tables


def years_for(run):
    f = glob.glob(os.path.join(run, "csv", "VIC", "VICSimDataMonthlySummary_seed_*.csv"))[0]
    m = pd.read_csv(f, index_col="Time (Month)", parse_dates=True)
    if COL not in m.columns:
        raise SystemExit(f"{f}: no monthly '{COL}' column (runs before 23 Nov 2026)")
    e = m[COL] / 1e6
    cy = e.groupby(e.index.year)
    fy = e.groupby(e.index.year + (e.index.month >= 7))
    out = pd.DataFrame({"calendar": cy.sum(), "calendar_months": cy.size(),
                        "july_june": fy.sum(), "july_june_months": fy.size()})
    # only whole years on each basis; 1998 is spin-up (the validation drops it), so July-June 1999 is incomplete
    out.loc[out.calendar_months < 12, "calendar"] = np.nan
    out.loc[out.july_june_months < 12, "july_june"] = np.nan
    out.loc[out.index <= 1998, "calendar"] = np.nan
    out.loc[out.index <= 1999, "july_june"] = np.nan
    return out[["calendar", "july_june"]]


def main():
    ens = sys.argv[1]
    inv = pd.read_csv(os.path.join(vs.DATA, "19902018_historic_emissions_Vic.csv"), index_col="Time (Year)").iloc[:, 0]
    rows, stats = [], []
    for run in sorted(glob.glob(os.path.join(ens, "run_s*"))):
        seed = int(run.rsplit("run_s", 1)[1])
        y = years_for(run)
        rows.append(y.assign(seed=seed))
        for basis in ("calendar", "july_june"):
            for lo, hi in WINDOWS:
                yrs = range(lo, hi + 1)
                s = vs.statistics(y[basis].reindex(yrs), inv.reindex(yrs))
                stats.append({"seed": seed, "basis": basis, "window": f"{lo}-{hi}", **s})
    per_year = pd.concat(rows).rename_axis("year")
    per_year.to_csv(os.path.join(ens, "emissions_year_basis.csv"))
    st = pd.DataFrame(stats)

    def q(g, m):
        return f"{g[m].median():.2f} ({g[m].quantile(0.05):.2f} to {g[m].quantile(0.95):.2f})"

    lines = ["# Emissions: calendar years against the inventory's July-June years",
             "", f"{per_year.seed.nunique()} seeds; median (5 % to 95 %) over the seeds.", "",
             "| Basis | Window | n (years) | NSE (-) | Bias (Mt CO2-e) | RMSE (Mt CO2-e) |", "|---|---|---|---|---|---|"]
    for (basis, window), g in st.groupby(["basis", "window"], sort=False):
        lines.append(f"| {basis} | {window} | {int(g.n.median())} | {q(g, 'NSE')} | {q(g, 'Bias')} | {q(g, 'RMSE')} |")
    med = per_year.groupby(level=0)[["calendar", "july_june"]].median()
    med["inventory"] = inv
    med["gap_calendar"] = med.calendar - med.inventory
    med["gap_july_june"] = med.july_june - med.inventory
    lines += ["", "Seed medians by year (Mt CO2-e; inventory year = July-June ending in that year):", "",
              "| Year | Model, calendar (Mt CO2-e) | Model, July-June (Mt CO2-e) | Inventory (Mt CO2-e) "
              "| Gap, calendar (Mt CO2-e) | Gap, July-June (Mt CO2-e) |", "|---|---|---|---|---|---|"]
    for yr, r in med.loc[1999:2018].iterrows():
        lines.append(f"| {yr} | " + " | ".join("" if pd.isna(v) else f"{v:.1f}" for v in r.values) + " |")
    open(os.path.join(ens, "emissions_year_basis.md"), "w", encoding="utf-8").write("\n".join(lines) + "\n")
    print("\n".join(lines))


if __name__ == "__main__":
    main()
