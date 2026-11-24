"""The recursive emission intensity index of the SPMs: consistency check and independent comparators (24 Nov 2026).

The index (Spm.computeIndicators) is the emissions attributed to each MWh consumed: each SPM's generation emissions
(generator MWh x emission factor), scaled up by 1 / (1 - network losses) at every level they pass, summed over the
nested SPMs and divided by the total generation. The validation tables test the system emissions (generator MWh x
factor, the inventory comparator), not the index. This script tests the index itself, on the 100-seed ensemble:

1. Consistency. Index = household emissions / household consumption (monthly output columns). Divided by the
   system intensity (System GHG Emissions / system generation, same months) it must be the network-loss uplift alone,
   steady from month to month and identical across seeds; anything else means the recursion loses or adds emissions.
2. NGA Factors (DISER, National Greenhouse Accounts Factors, August 2021, Table 46, Victoria; the last edition with
   the time series): scope 2 = combustion emissions of the state's electricity / electricity sent out, interstate
   flows accounted for, three-year moving average, July-June years. Scope 3 for end users = fuel extraction and
   transport + the emissions of electricity lost in transmission and distribution; the fuel part alone is the network
   operators' scope 3 (0.01 kg CO2-e/kWh for Victoria, latest estimate), so scope 2 + scope 3 - 0.01 approximates
   emissions per kWh delivered. The kg/GJ columns are used (/ 277.78): the kg/kWh column reads 0.102 for 2017/18,
   a typo for 1.02 (283 kg/GJ).
3. AEMO CDEII, VIC1 (experiments/simulationData/co2eii_summary_results_<year>.csv, daily, from June 2011): emissions
   of the region's generation per MWh sent out, no losses, no interstate flows. Summed over July-June years.

Basis of the model's emission factors: NGER scope 1 emissions / NGER electricity production, which is "as generated"
(Acil Allen 2016, Emission factors assumptions update, report to AEMO, s. 2.5 and 3.1), while NGA scope 2 and CDEII
divide by electricity sent out (generation less the stations' own use).

Usage: python emissions_index.py RUNS_DIR OUT_DIR [--nga-xlsx PATH]
  RUNS_DIR   seed_ensemble.py OUT_DIR with run_s<seed>/csv/VIC/*MonthlySummary*.csv holding System GHG Emissions
             (runs from 23 Nov 2026 on)
  OUT_DIR    gets emissions_index.csv (July-June year: index and system intensity, median and 5-95 % over the seeds;
             NGA and CDEII) and emissions_index.md
  --nga-xlsx national-greenhouse-accounts-factors-tables-2021.xlsx: rebuilds experiments/simulationData/
             nga_factors_2021_table46_vic.csv from the workbook (otherwise the CSV is read)
"""
import glob, os, re, sys
import numpy as np, pandas as pd

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import validation_statistics as vs  # noqa: E402

DATA = os.path.join(HERE, os.pardir, "simulationData")
NGA_CSV = os.path.join(DATA, "nga_factors_2021_table46_vic.csv")
FUEL_SCOPE3 = 0.01      # kg CO2-e/kWh, NGA 2021 Table 46 note: Victorian scope 3 for network operators (fuel only)
FIRST_FY, LAST_FY = 2000, 2020   # first whole July-June year after the 1998 spin-up; the runs end in 2020+
GEN_COLS = ["System Production Rooftop PV", "System Production Coal", "System Production Water",
            "System Production Wind", "System Production Gas", "System Production Solar", "System Production Battery"]


def nga_from_xlsx(path):
    import openpyxl
    ws = openpyxl.load_workbook(path, data_only=True, read_only=True)["Table 46"]
    on, rows = False, []
    for r in ws.iter_rows(values_only=True):
        r = [c for c in r if c is not None]
        if r and isinstance(r[0], str) and r[0].strip().upper().startswith("VICTORIA"):
            on, r = True, r[1:]
        elif r and isinstance(r[0], str) and r[0].strip().upper().startswith("QUEENSLAND"):
            on = False
        if on and r and isinstance(r[0], str) and re.match(r"\d{4}/", r[0].strip()):
            rows.append({"fy": int(r[0].strip()[:4]) + 1, "scope2_kg_per_GJ": r[2], "scope3_kg_per_GJ": r[4]})
    t = pd.DataFrame(rows)
    t["scope2"] = t.scope2_kg_per_GJ / 277.78
    t["scope3"] = t.scope3_kg_per_GJ / 277.78
    t.round(4).to_csv(NGA_CSV, index=False)
    return t


def nga():
    t = pd.read_csv(NGA_CSV).set_index("fy")
    t["scope2_plus_losses"] = t.scope2 + t.scope3 - FUEL_SCOPE3
    return t


def cdeii():
    """AEMO CDEII, VIC1, summed over July-June years (complete years only)."""
    parts = []
    for f in sorted(glob.glob(os.path.join(DATA, "co2eii_summary_results_*.csv"))):
        d = pd.read_csv(f, skiprows=1, header=0, usecols=range(4, 11), on_bad_lines="skip")
        d = d[d.REGIONID == "VIC1"]
        parts.append(d)
    d = pd.concat(parts)
    d["date"] = [pd.to_datetime(s, dayfirst="/" in s[:3] or s[2] == "/") for s in d.SETTLEMENTDATE.astype(str)]
    d = d.drop_duplicates("date", keep="last").set_index("date").sort_index()
    d["fy"] = d.index.year + (d.index.month >= 7)
    g = d.groupby("fy")
    out = pd.DataFrame({"cdeii": g.TOTAL_EMISSIONS.sum() / g.TOTAL_SENT_OUT_ENERGY.sum(), "days": g.size()})
    return out[out.days >= 365]


def run_series(f):
    """Monthly index, system intensity and their ratio for one run; and the July-June aggregates."""
    m = pd.read_csv(f, parse_dates=["Time (Month)"]).set_index("Time (Month)")
    hh = m["Number of Domestic Consumers (households)"]
    d = pd.DataFrame({"e_hh": m["GHG Emissions (tCO2-e) per household"] * hh,
                      "c_hh": m["Consumption (KWh) per household"] * hh / 1000.0,
                      "e_sys": m["System GHG Emissions (tCO2-e)"], "gen": m[GEN_COLS].sum(axis=1)})
    d = d[(d.c_hh > 0) & (d.gen > 0) & (d.e_sys > 0)]
    monthly_ratio = (d.e_hh / d.c_hh) / (d.e_sys / d.gen)
    d["fy"] = d.index.year + (d.index.month >= 7)
    g = d.groupby("fy").sum()
    fy = pd.DataFrame({"index": g.e_hh / g.c_hh, "system": g.e_sys / g.gen})
    return monthly_ratio, fy


def main():
    runs, out = sys.argv[1], sys.argv[2]
    if "--nga-xlsx" in sys.argv:
        nga_from_xlsx(sys.argv[sys.argv.index("--nga-xlsx") + 1])
    files = sorted(glob.glob(os.path.join(runs, "run_s*", "csv", "VIC", "*MonthlySummary*.csv")))
    ratios, idx, sysi = {}, {}, {}
    for f in files:
        seed = int(re.search(r"run_s(\d+)", f).group(1))
        r, fy = run_series(f)
        ratios[seed], idx[seed], sysi[seed] = r, fy["index"], fy.system
    ratios, idx, sysi = pd.DataFrame(ratios), pd.DataFrame(idx), pd.DataFrame(sysi)
    ratios = ratios.loc[(ratios.index >= "1999-01-01") & (ratios.index < f"{LAST_FY + 1}-01-01")]  # runs go on to 2050
    yrs = list(range(FIRST_FY, LAST_FY + 1))
    idx, sysi = idx.loc[idx.index.isin(yrs)], sysi.loc[sysi.index.isin(yrs)]
    n, c = nga(), cdeii()
    t = pd.DataFrame({"index_median": idx.median(axis=1), "index_p5": idx.quantile(.05, axis=1),
                      "index_p95": idx.quantile(.95, axis=1),
                      "index_3yr_median": idx.rolling(3).mean().median(axis=1),
                      "system_median": sysi.median(axis=1)})
    t = t.join(n[["scope2", "scope3", "scope2_plus_losses"]]).join(c.cdeii)
    t.index.name = "fy"
    t.round(4).to_csv(os.path.join(out, "emissions_index.csv"))

    def row(name, sim, obs):
        k = pd.concat([sim.rename("s"), obs.rename("o")], axis=1).dropna()
        per_seed = None
        if sim.name in ("index", "system", "index_3yr"):
            src = {"index": idx, "system": sysi, "index_3yr": idx.rolling(3).mean()}[sim.name]
            per_seed = [vs.statistics(src.loc[k.index, s], k.o)["NSE"] for s in src.columns]
        r = vs.statistics(k.s, k.o)
        rel = 100 * (k.s / k.o - 1).mean()
        nse = (f"{np.median(per_seed):.2f} ({np.percentile(per_seed, 5):.2f} to {np.percentile(per_seed, 95):.2f})"
               if per_seed else f"{r['NSE']:.2f}")
        return f"| {name} | {k.index.min()}-{k.index.max()} | {r['n']} | {nse} | {r['Bias']:+.3f} | {rel:+.1f} % | {r['r']:.2f} |"

    med = lambda x, nm: x.median(axis=1).rename(nm)
    lines = ["# Recursive emission intensity index: consistency and independent comparators", "",
             f"{idx.shape[1]} seeds. July-June years ('2019' = July 2018 to June 2019). t CO2-e/MWh = kg CO2-e/kWh.", "",
             "## 1. Consistency of the recursion", "",
             "Index / system intensity (household emissions / household consumption, over system emissions / system "
             "generation), month by month:", "",
             f"- range over all months and seeds: {np.nanmin(ratios.values):.4f} to {np.nanmax(ratios.values):.4f}",
             f"- largest spread across seeds in one month: {np.nanmax(ratios.max(axis=1) - ratios.min(axis=1)):.2e}",
             f"- by year (median over seeds): " + ", ".join(
                 f"{y}: {v:.3f}" for y, v in ratios.median(axis=1).groupby(ratios.index.year).mean().items()
                 if FIRST_FY - 1 <= y <= LAST_FY), "",
             "## 2. Independent comparators", "",
             "Statistics over the years both sides cover. NSE: median over seeds (5 % to 95 %) for the model series; "
             "bias and mean relative bias of the median series.", "",
             "| Model series vs comparator | Years | n | NSE | Bias (t/MWh) | Mean relative bias | r |",
             "|---|---|---|---|---|---|---|",
             row("Index vs NGA scope 2 (sent out)", med(idx, "index"), n.scope2),
             row("Index vs NGA scope 2 + losses (delivered)", med(idx, "index"), n.scope2_plus_losses),
             row("Index, 3-yr mean vs NGA scope 2 + losses", med(idx.rolling(3).mean(), "index_3yr"),
                 n.scope2_plus_losses),
             row("System intensity vs NGA scope 2", med(sysi, "system"), n.scope2),
             row("System intensity vs AEMO CDEII VIC1 (sent out)", med(sysi, "system"), c.cdeii),
             "", "## Years (t CO2-e/MWh)", "",
             "| July-June year | Index, median (t/MWh) | Index, 5-95 % (t/MWh) | System intensity, median (t/MWh) | "
             "NGA scope 2 (t/MWh) | NGA scope 2 + losses (t/MWh) | AEMO CDEII VIC1 (t/MWh) |",
             "|---|---|---|---|---|---|---|"]
    f3 = lambda v: "" if pd.isna(v) else f"{v:.3f}"
    for y, r in t.iterrows():
        lines.append(f"| {y} | {f3(r.index_median)} | {f3(r.index_p5)}-{f3(r.index_p95)} | {f3(r.system_median)} | "
                     f"{f3(r.scope2)} | {f3(r.scope2_plus_losses)} | {f3(r.cdeii)} |")
    open(os.path.join(out, "emissions_index.md"), "w", encoding="utf8").write("\n".join(lines) + "\n")
    print("\n".join(lines))


if __name__ == "__main__":
    main()
