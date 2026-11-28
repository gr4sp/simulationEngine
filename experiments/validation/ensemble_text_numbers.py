"""The numbers the methods article quotes in its validation text, from an ensemble's committed summaries (28 Nov 2026).

From ENSEMBLE_DIR/seed_years.csv (seed_ensemble.py), the record of validation_statistics.load_price_comparator() (as
seed_band_figures.py Figure 5) and ENSEMBLE_DIR/emissions_year_basis.csv with the inventory file
validation_statistics reads:
  - years 1999-2020 in which the median annual price over the seeds is below the recorded annual mean;
  - width of the 5-95 % band of the annual price over the seeds ($/MWh): in the median year of 1999-2019 and in 2017;
  - July-June emissions, median over the seeds minus the inventory, 2000-2004 (Mt CO2-e).

Usage: python experiments/validation/ensemble_text_numbers.py ENSEMBLE_DIR [ENSEMBLE_DIR ...]
"""
import os
import sys

import pandas as pd

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import validation_statistics as vs  # noqa: E402


def numbers(ens):
    years = pd.read_csv(os.path.join(ens, "seed_years.csv"))
    w = years.pivot(index="year", columns="seed", values="price")
    med = w.median(axis=1)
    width = w.quantile(0.95, axis=1) - w.quantile(0.05, axis=1)
    obs = vs.load_price_comparator().resample("YE").mean()
    obs.index = obs.index.year
    yrs = [y for y in range(1999, 2021) if y in med.index and y in obs.index]
    below = [y for y in yrs if med[y] < obs[y]]
    out = {"years compared (price)": f"{yrs[0]}-{yrs[-1]} ({len(yrs)})",
           "years with median annual price below the record (count)": len(below),
           "years with median annual price at or above the record": ", ".join(str(y) for y in yrs if y not in below),
           "annual price band width, median year 1999-2019 ($/MWh)": round(width.loc[1999:2019].median(), 1),
           "annual price band width, 2017 ($/MWh)": round(width.loc[2017], 1)}
    fy = pd.read_csv(os.path.join(ens, "emissions_year_basis.csv"))
    jj = fy.pivot(index="year", columns="seed", values="july_june").median(axis=1)
    inv = pd.read_csv(os.path.join(vs.DATA, "19902018_historic_emissions_Vic.csv"), index_col="Time (Year)")["Hist_GHGE_MtCO2e"]
    gap = jj.loc[2000:2004] - inv.loc[2000:2004]
    out["July-June emissions gap 2000-2004, median minus inventory (Mt CO2-e)"] = ", ".join(
        f"{y}: {g:+.1f}" for y, g in gap.items())
    return out


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    for ens in sys.argv[1:]:
        print(f"## {os.path.basename(os.path.normpath(ens))}")
        for k, v in numbers(ens).items():
            print(f"- {k}: {v}")
