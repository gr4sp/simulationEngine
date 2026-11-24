"""Solar adoption, step 1: households by tenure for each census cell (dwelling x income bracket x household type).

Reads the same ABS TableBuilder rent and mortgage wafers as the EVA's housing-cost builder
(../energyVulnerability/build_ahc_census.py, `housing`), so the cells match the EVA profiles exactly.

Tenure, per (census, dw, lb, type):
  renting   households in the rent wafer whose rent is stated, nil or not stated (that is, not 'Not applicable')
  mortgage  households in the mortgage wafer whose repayment is stated, nil or not stated
  outright  the rest: owned outright, plus other tenures (life tenure, rent/buy schemes) and tenure not stated
Households = the rent wafer summed over all its rows (the census count of the cell).
TableBuilder perturbs cells, so renting + mortgage can exceed the total by a few households in small cells; outright
is then floored at 0.

Income quintile: the EVA's HIED brackets are ordered by lower bound; each bracket goes to the quintile that holds its
middle household (cumulative share of all Victorian households of that census, stated incomes only).

Output: data/tenure_VIC.csv (census, dw, lb, type, quintile, households, renting, mortgage, outright).

Usage: python build_tenure_census.py DATA_DIR
"""
import argparse, os, sys

import numpy as np
import pandas as pd

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, os.pardir, "energyVulnerability"))
from build_ahc_census import DWELLINGS, HOUSING, housing  # noqa: E402


def tenure(data, year):
    h = housing(data, year)
    keys = ["dw", "lb", "type"]
    total = h[h.tenure == "rent"].groupby(keys).n.sum().rename("households")
    rent = h[(h.tenure == "rent") & (h.status != "na")].groupby(keys).n.sum().rename("renting")
    mort = h[(h.tenure == "mort") & (h.status != "na")].groupby(keys).n.sum().rename("mortgage")
    t = pd.concat([total, rent, mort], axis=1).fillna(0).reset_index()
    t["outright"] = np.maximum(t.households - t.renting - t.mortgage, 0)
    t.insert(0, "census", year)
    return t


def quintiles(t):
    out = []
    for year, g in t.groupby("census"):
        by_lb = g.groupby("lb").households.sum().sort_index()
        cum = by_lb.cumsum() / by_lb.sum()
        mid = cum - by_lb / by_lb.sum() / 2
        q = np.minimum((mid * 5).astype(int) + 1, 5)
        out.append(g.assign(quintile=g.lb.map(q)))
    return pd.concat(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("data_dir")
    ap.add_argument("--out", default=os.path.join(HERE, "data", "tenure_VIC.csv"))
    a = ap.parse_args()
    t = pd.concat([tenure(a.data_dir, y) for y in sorted(HOUSING)])
    # the EVA's four dwelling types; caravans, improvised homes, flats over shops and not stated -> OTHER
    t["dw"] = t.dw.where(t.dw.isin(DWELLINGS), "OTHER")
    t = t.groupby(["census", "dw", "lb", "type"], as_index=False)[["households", "renting", "mortgage", "outright"]].sum()
    t = quintiles(t)
    t = t[["census", "dw", "lb", "type", "quintile", "households", "renting", "mortgage", "outright"]]
    os.makedirs(os.path.dirname(a.out), exist_ok=True)
    t.to_csv(a.out, index=False, float_format="%.0f")
    s = t.groupby("census")[["households", "renting", "mortgage", "outright"]].sum()
    for c in ["renting", "mortgage", "outright"]:
        s[c + "_%"] = (100 * s[c] / s.households).round(1)
    print(s.to_string())
    print(t.groupby(["census", "dw"])[["renting", "households"]].sum().assign(
        renting_pct=lambda d: (100 * d.renting / d.households).round(1)).renting_pct.unstack().to_string())


if __name__ == "__main__":
    main()
