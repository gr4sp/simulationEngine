"""Solar adoption, step 3 (item 1): the household cohort table the adoption model reads.

A cohort is one combination of household type (the EVA's 6) x dwelling (the EVA's 4) x main heating fuel (gas, all
electric) x tenure (renting, mortgage, outright) x income quintile (5): 720 cohorts (decision D1, 13 Nov 2026). The
table gives each cohort's share of Victorian households in each year 1999-2021; the model multiplies the shares by its
own household count (`domestic_consumers`), so the census total does not have to match the model's.

Inputs
  data/tenure_VIC.csv                    census 2006-2021 x dwelling x HIED bracket x type, split by tenure (step 1)
  ../assesmentData/fuel_mix_VIC.csv      share of Victorian homes heated mainly by gas (ABS surveys, 1999-2014)
  ../assesmentData/fuel_mix_income_VIC.csv  the 2009 gradient of mains-gas use by household income (ABS 4602.2)

Method
  Income quintile: the quintile of equivalised household income (census HIED, see Income below) of that census. Households
    are taken as spread evenly within each bracket (as the EVA, A29), so a bracket that straddles a quintile boundary
    is split between the two quintiles and every quintile holds 20 % of households. (Step 1's `quintile` column put a
    whole bracket in the quintile of its middle household, which gave quintiles of 11-35 %; it is not used here.)
  Fuel: the EVA's rule (notebook cell 66, `fuel_pop`, FUEL_MIX = 'heating'): the Victorian share heated mainly by gas
    in the year (linear between surveys, held at the ends) with the 2009 income gradient applied at each household's
    place in the income distribution, rescaled so the Victorian share is kept. The gradient is applied to each
    bracket-quintile piece at its middle rank. Fuel is taken as independent of dwelling (2009: separate houses 81.9 %,
    other dwellings 81.6 % on mains gas) and of tenure within a cell (no source for a tenure difference).
  Years: census counts per cohort are linear between censuses (as the EVA), held at the 2006 shares before 2006
    (A32) and at the 2021 shares after 2021. The gas share is applied in each year.
  Dwelling OTHER (caravans, improvised homes, flats over shops, not stated: 0.5-0.8 % of households) is left out and
    the shares are of the four EVA types.
  Income: the census variable HIED is *equivalised* gross household income (modified OECD scale), so the quintiles
    are of equivalised income. The rebates' means tests are on household (taxable) income, so each cohort also gets
    the bounds of its quintile in household income: the equivalised bounds x the cohort's mean equivalence factor eA
    (the EVA's ahc_equivalence_VIC.csv, households-weighted over the cohort's cells), AUD a year, current dollars of
    the census, linear between censuses. Gross income stands in for taxable income. The open top bracket is taken to
    end at twice its lower bound (assumption).

Output: simulationSettings/household_cohorts_VIC.csv
  year, type, dwelling, fuel, tenure, quintile, share, income_lo, income_hi (household income, AUD/yr, see above)

Usage: python build_cohorts.py [--out PATH] [--model-households]
"""
import argparse, os

import numpy as np
import pandas as pd

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, os.pardir, os.pardir))
ASSESS = os.path.join(HERE, os.pardir, "assesmentData")
DWELLINGS = ["DETACHED", "SEMI-DETACHED", "LOW FLAT", "HIGH FLAT"]
TENURES = ["renting", "mortgage", "outright"]
YEARS = range(1999, 2022)
TOP_FACTOR = 2.0   # upper bound of the open top bracket = TOP_FACTOR x its lower bound


def bracket_bounds(lbs):
    """Weekly [lower, upper] of each HIED bracket from the ordered lower bounds: -100 = negative income and 0 = nil
    income are points (at 0 $), 1 is the first positive bracket; each upper bound is the next lower bound."""
    lbs = sorted(lbs)
    pos = [l for l in lbs if l >= 1]
    out = {}
    for l in lbs:
        if l < 1:
            out[l] = (0.0, 0.0)
    for i, l in enumerate(pos):
        lo = 0.0 if l == 1 else float(l)
        hi = float(pos[i + 1]) if i + 1 < len(pos) else TOP_FACTOR * l
        out[l] = (lo, hi)
    return out


def split_quintiles(g, fmi, eq):
    """One census: rows (dw, lb, type, tenure, quintile, n, nr) with each bracket split across quintiles; nr = n x the
    2009 gas-use ratio at the piece's middle rank. Also the quintile income bounds ($/yr)."""
    by_lb = g.groupby("lb").households.sum().sort_index()
    share = by_lb / by_lb.sum()
    c1 = share.cumsum()
    c0 = c1 - share
    bounds = bracket_bounds(by_lb.index)
    p_hi, ratio = fmi.p_hi.to_numpy(), fmi.ratio.to_numpy()
    pieces = []   # (lb, quintile, fraction of the bracket, middle rank)
    for lb in by_lb.index:
        a, b = c0[lb], c1[lb]
        for q in range(1, 6):
            lo, hi = max(a, (q - 1) / 5), min(b, q / 5)
            if hi > lo:
                pieces.append((lb, q, (hi - lo) / (b - a), (lo + hi) / 2))
    pieces = pd.DataFrame(pieces, columns=["lb", "quintile", "frac", "rank"])
    pieces["ratio"] = ratio[np.clip(np.searchsorted(p_hi, pieces["rank"].to_numpy(), side="left"), 0, len(ratio) - 1)]
    long = g.melt(id_vars=["dw", "lb", "type"], value_vars=TENURES, var_name="tenure", value_name="n0")
    long = long.merge(pieces, on="lb")
    long = long.merge(eq, on=["dw", "lb", "type"], how="left")
    assert long.eA.notna().all(), long[long.eA.isna()][["dw", "lb", "type"]].drop_duplicates()
    long["n"] = long.n0 * long.frac
    long["nr"] = long.n * long.ratio
    long["ne"] = long.n * long.eA
    # income at rank x: inverse of the piecewise-uniform distribution ($/week -> $/yr)
    def income_at(x):
        for lb in by_lb.index:
            if c1[lb] >= x - 1e-12:
                lo, hi = bounds[lb]
                f = 0.0 if c1[lb] == c0[lb] else (x - c0[lb]) / (c1[lb] - c0[lb])
                return 52 * (lo + f * (hi - lo))
        return 52 * bounds[by_lb.index[-1]][1]
    qb = pd.DataFrame({"quintile": range(1, 6),
                       "income_lo": [income_at(q / 5 - 0.2 + 1e-9) if q > 1 else 0.0 for q in range(1, 6)],
                       "income_hi": [income_at(q / 5) for q in range(1, 6)]})
    return long.groupby(["dw", "type", "tenure", "quintile"], as_index=False)[["n", "nr", "ne"]].sum(), qb


def gas_share(year, fm):
    s = fm[["year", "heating_gas_pct"]].dropna()
    return float(np.interp(year, s.year, s.heating_gas_pct)) / 100


def build():
    t = pd.read_csv(os.path.join(HERE, "data", "tenure_VIC.csv"))
    fm = pd.read_csv(os.path.join(ASSESS, "fuel_mix_VIC.csv"))
    fmi = pd.read_csv(os.path.join(ASSESS, "fuel_mix_income_VIC.csv"))
    eq = pd.read_csv(os.path.join(ASSESS, "ahc_equivalence_VIC.csv")).rename(columns={"dwelling": "dw"})
    censuses = sorted(t.census.unique())
    cells, bounds = {}, {}
    for c in censuses:
        e = eq[eq.census == c][["dw", "lb", "type", "eA"]]
        # dwelling OTHER is not in the EVA's table: it takes the census's all-dwellings factor (ALL)
        e = pd.concat([e, e[e.dw == "ALL"].assign(dw="OTHER")])
        cells[c], bounds[c] = split_quintiles(t[t.census == c], fmi, e)
    keys = ["dw", "type", "tenure", "quintile"]
    frames = []
    for y in YEARS:
        yc = min(max(y, censuses[0]), censuses[-1])
        hi = next(c for c in censuses if c >= yc)
        lo = max(c for c in censuses if c <= yc)
        w = 0.0 if hi == lo else (yc - lo) / (hi - lo)
        m = cells[lo].merge(cells[hi], on=keys, how="outer", suffixes=("_a", "_b")).fillna(0)
        m["n"] = (1 - w) * m.n_a + w * m.n_b
        m["nr"] = (1 - w) * m.nr_a + w * m.nr_b
        m["ne"] = (1 - w) * m["ne_a"] + w * m["ne_b"]
        m = m[m.dw.isin(DWELLINGS)]
        k = gas_share(y, fm) / (m.nr.sum() / m.n.sum())
        gas = m.assign(fuel="GAS", h=k * m.nr)
        elec = m.assign(fuel="ELEC", h=m.n - k * m.nr)
        assert (gas.h <= m.n + 1e-6).all(), "gas share above 1"
        f = pd.concat([gas, elec])
        f["share"] = f.h / f.h.sum()
        b = bounds[lo].merge(bounds[hi], on="quintile", suffixes=("_a", "_b"))
        b["income_lo"] = (1 - w) * b.income_lo_a + w * b.income_lo_b
        b["income_hi"] = (1 - w) * b.income_hi_a + w * b.income_hi_b
        f = f.merge(b[["quintile", "income_lo", "income_hi"]], on="quintile")
        eA = np.where(f["n"] > 0, f["ne"] / f["n"].where(f["n"] > 0, 1), 1.0)
        f["income_lo"] *= eA
        f["income_hi"] *= eA
        f.insert(0, "year", y)
        frames.append(f.rename(columns={"dw": "dwelling"})[
            ["year", "type", "dwelling", "fuel", "tenure", "quintile", "share", "income_lo", "income_hi"]])
    return pd.concat(frames), t


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=os.path.join(ROOT, "simulationSettings", "household_cohorts_VIC.csv"))
    ap.add_argument("--model-households", action="store_true", help="compare the census total with domestic_consumers")
    a = ap.parse_args()
    out, t = build()
    n = out.groupby("year").size()
    assert (n == 720).all(), n[n != 720]
    with open(a.out, "w", newline="") as fh:
        fh.write("# Household cohorts for the rooftop PV adoption model (solar step 3): share of Victorian households in each\n"
                 "# year by household type x dwelling x main heating fuel x tenure x equivalised income quintile (720 cohorts),\n"
                 "# and the household income bounds of the quintile (AUD/yr, current dollars). Built by experiments/solarAdoption/build_cohorts.py\n"
                 "# from the census 2006-2021 (tenure_VIC.csv) and the ABS fuel surveys; held at 2006 before 2006, at 2021 after.\n")
        out.to_csv(fh, index=False, float_format="%.6g")
    s = out.groupby("year").share.sum()
    assert np.allclose(s, 1.0), s
    print("written", a.out, len(out), "rows")
    tot = t[t.dw.isin(DWELLINGS)].groupby("census").households.sum()
    print("census households (four dwelling types, stated incomes):", tot.to_dict())
    for y in (1999, 2006, 2011, 2016, 2019, 2021):
        o = out[out.year == y]
        print(y, "tenure", o.groupby("tenure").share.sum().round(3).to_dict(),
              "gas", round(o[o.fuel == "GAS"].share.sum(), 3),
              "quintiles", o.groupby("quintile").share.sum().round(3).tolist(),
              "q bounds k$", [(round(l / 1e3), round(h / 1e3)) for l, h in
                              o.groupby("quintile").apply(lambda d: pd.Series({
                                  "lo": (d.income_lo * d.share).sum() / d.share.sum(),
                                  "hi": (d.income_hi * d.share).sum() / d.share.sum()})).to_numpy()])
    if a.model_households:
        import subprocess
        psql = r"C:\Program Files\PostgreSQL\18\bin\psql.exe"
        r = subprocess.run([psql, "-U", "postgres", "-d", "gr4spdb", "-A", "-t", "-c",
                            "select extract(year from date)::int, domesticconsumers from domestic_consumers "
                            "where extract(month from date) = 1 and date between '2006-01-01' and '2021-12-31'"],
                           capture_output=True, text=True)
        model = {int(l.split("|")[0]): float(l.split("|")[1]) for l in r.stdout.split()}
        for c in tot.index:
            if c in model:
                print(f"census {c}: {tot[c]:,.0f} households in the table, model {model[c]:,.0f} (ratio {model[c] / tot[c]:.3f})")


if __name__ == "__main__":
    main()
