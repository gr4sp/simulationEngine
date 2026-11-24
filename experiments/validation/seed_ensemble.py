"""R3 step 4: the repaired base case over many random seeds, and the spread of every validation statistic.

The model draws breakdowns (forced outages) and other events from its random generator, so one seed is one possible
history. This runs the base case (simulationSettings/ as committed, classes from build/classes/java/main) for seeds
1..N in parallel, then computes the validation statistics of validation_statistics.py for each run, and reports the
reference seed (42) beside the median and the 5-95 % range over the seeds. Parameter uncertainty is not sampled here
(Angela, 11 Nov 2026: Morris pilot and the Saltelli sample-size check decide whether a parameter ensemble is rerun).

Usage: python seed_ensemble.py OUT_DIR [N_SEEDS=100] [PARALLEL=10]   (build first: ./gradlew compileJava)
  OUT_DIR/run_s<seed>/csv/VIC/*.csv   each run's year and monthly summaries (the other outputs are deleted)
  OUT_DIR/seed_statistics.csv         one row per seed and indicator: n, MAE, RMSE, Bias, NRMSE, NSE, r
  OUT_DIR/seed_summary.csv / .md      per indicator: seed 42, median, 5 % and 95 % over the seeds (md: main table
                                      NSE, Bias, RMSE; monthly generation by fuel; supplement MAE, NRMSE)
  OUT_DIR/seed_fuel_months.csv        per seed and month: simulated generation by fuel (GWh), RE share (%)
  OUT_DIR/seed_years.csv              per seed and year: price, RE share (full year and over the record's months),
                                      tariff, GHGE (for the figures' spread)
Runs already finished are not repeated, so the script can be restarted.
"""
import concurrent.futures as cf, glob, os, shutil, subprocess, sys
import numpy as np, pandas as pd

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, os.pardir, os.pardir))
sys.path.insert(0, HERE)
import validation_statistics as vs  # noqa: E402
import emissions_year_basis as eyb  # noqa: E402

REFERENCE_SEED = 42
# Emissions on the inventory's own year, July to June (23 Nov 2026, Angela: main table July-June, calendar years in the
# supplement for comparison with earlier versions). Needs the monthly emissions column (runs from 23 Nov 2026 on).
GHGE_FY = {"GHGE (annual)": ("GHGE (annual, July-June)", 2005, 2018),
           "GHGE (annual, full)": ("GHGE (annual, July-June, full)", 2000, 2018)}


def run(out, seed, cls):
    d = os.path.join(out, f"run_s{seed}")
    if glob.glob(os.path.join(d, "csv", "VIC", "VICSimDataMonthlySummary_seed_*.csv")):
        return seed, "done before"
    shutil.rmtree(d, ignore_errors=True)
    for sub in ("csv", "plots", "logs"):
        os.makedirs(os.path.join(d, sub))
    shutil.copytree(os.path.join(out, "settings"), os.path.join(d, "simulationSettings"))
    cp = f"{cls};{REPO}/build/resources/main;{REPO}/build/runtime-libs/*"
    with open(os.path.join(d, "stdout.txt"), "w") as log:
        r = subprocess.run(["java", "-Xmx3g", "-cp", cp, "core.Gr4spSim", "-seed", str(seed)], cwd=d, stdout=log,
                           stderr=subprocess.STDOUT)
    for keep in ("plots", "logs", "simulationSettings"):
        shutil.rmtree(os.path.join(d, keep), ignore_errors=True)
    return seed, f"exit {r.returncode}"


def stats_for(out, seed):
    d = os.path.join(out, f"run_s{seed}")
    year, month, ghge, opennem, tariffs = vs.load_inputs(d)
    ind = vs.series(year, month, ghge, opennem, tariffs)
    fuel = vs.fuel_series(month, opennem)
    ind.update(fuel)
    rows = [{"seed": seed, "indicator": k, **vs.statistics(s, o)} for k, (s, o, _, _) in ind.items()]
    fy = eyb.years_for(d).july_june
    inv = ghge["Hist_GHGE_MtCO2e"]
    for name, lo, hi in GHGE_FY.values():
        rows.append({"seed": seed, "indicator": name,
                     **vs.statistics(fy.reindex(range(lo, hi + 1)), inv.reindex(range(lo, hi + 1)))})
    ghge_total = (year["System GHG Emissions (tCO2-e)"] / 1e6 if "System GHG Emissions (tCO2-e)" in year.columns
                  else year["GHG Emissions (tCO2-e) per household"]
                  * year["Number of Domestic Consumers (households)"] / 1e6 / 0.3)  # runs before 14 Nov 2026
    yr = pd.DataFrame({"seed": seed, "price": vs.monthly_prices(month).simulated.resample("YE").mean().reindex(
        pd.to_datetime([f"{y}-12-31" for y in year.index])).values,
        "re_pct": year["Percentage Renewable Production"].values * 100,
        "tariff": year["Avg Tariff (c/KWh) per household"].values, "ghge_mt": ghge_total.values}, index=year.index)
    # the renewable share over the months the record covers (2005: April-December, 2020: January-June), as compared
    re_rec = vs.renewable_share_simulated(month, opennem)
    yr["re_pct_record_months"] = pd.Series(re_rec.values, index=re_rec.index.year).reindex(year.index)
    months = month.index[month.index.isin(opennem.index)]
    fm = pd.DataFrame({k: v[0] for k, v in fuel.items()}, index=months).round(3)
    fm.insert(0, "seed", seed)
    return rows, yr, fm


def main():
    out = sys.argv[1]
    n = int(sys.argv[2]) if len(sys.argv) > 2 else 100
    par = int(sys.argv[3]) if len(sys.argv) > 3 else 10
    os.makedirs(out, exist_ok=True)
    cls = os.path.join(out, "cls")
    if not os.path.exists(cls):
        shutil.copytree(os.path.join(REPO, "build", "classes", "java", "main"), cls)
    if not os.path.exists(os.path.join(out, "settings")):
        shutil.copytree(os.path.join(REPO, "simulationSettings"), os.path.join(out, "settings"))
    seeds = sorted(set(range(1, n + 1)) | {REFERENCE_SEED})
    with cf.ThreadPoolExecutor(par) as ex:
        for seed, msg in ex.map(lambda s: run(out, s, cls), seeds):
            print(f"seed {seed}: {msg}", flush=True)

    rows, years, fuel_months = [], [], []
    for s in seeds:
        r, y, fm = stats_for(out, s)
        rows += r
        years.append(y)
        fuel_months.append(fm)
    pd.concat(fuel_months).rename_axis("month").to_csv(os.path.join(out, "seed_fuel_months.csv"))
    st = pd.DataFrame(rows)
    y42, m42, g42, o42, t42 = vs.load_inputs(os.path.join(out, f"run_s{REFERENCE_SEED}"))
    ind_units = {**vs.series(y42, m42, g42, o42, t42), **vs.fuel_series(m42, o42)}
    st.to_csv(os.path.join(out, "seed_statistics.csv"), index=False)
    pd.concat(years).rename_axis("year").to_csv(os.path.join(out, "seed_years.csv"))
    summ = []
    for ind, g in st.groupby("indicator", sort=False):
        ref = g[g.seed == REFERENCE_SEED].iloc[0]
        for m in ("NSE", "Bias", "RMSE", "MAE", "NRMSE"):
            summ.append({"indicator": ind, "metric": m, "seed42": ref[m], "median": g[m].median(),
                         "p05": g[m].quantile(0.05), "p95": g[m].quantile(0.95), "seeds": len(g)})
    sm = pd.DataFrame(summ)
    sm.to_csv(os.path.join(out, "seed_summary.csv"), index=False)
    sm["cell"] = sm.apply(lambda r: f"{r['median']:.2f} ({r.p05:.2f} to {r.p95:.2f})", axis=1)
    piv = sm.pivot(index="indicator", columns="metric", values="cell").reindex(list(dict.fromkeys(sm.indicator)))
    units = {k: v[3] for k, v in ind_units.items()}
    units.update({name: units.get(cal, "") for cal, (name, _, _) in GHGE_FY.items()})
    fuel_names = [k for k in piv.index if k in vs.FUEL_MONTHLY or k == "RE share (monthly)"]
    fy_names = [name for name, _, _ in GHGE_FY.values()]
    # main table: emissions on July-June years in the place of the calendar-year rows
    main_names = [GHGE_FY[k][0] if k in GHGE_FY else k for k in piv.index if k not in fuel_names + fy_names]
    cal_names = list(GHGE_FY)

    def table(f, names, metrics):
        f.write("| Indicator | Unit | " + " | ".join(metrics) + " |\n|---|---|" + "---|" * len(metrics) + "\n")
        for k in names:
            f.write(f"| {k} | {units.get(k, '')} | " + " | ".join(piv.loc[k, metrics].values) + " |\n")

    with open(os.path.join(out, "seed_summary.md"), "w", encoding="utf-8") as f:
        f.write(f"Base case over {len(seeds)} seeds: median (5th to 95th percentile over the seeds). NSE has no unit; "
                "Bias, RMSE and MAE are in the indicator's unit, NRMSE in % of the observed mean. Seed "
                f"{REFERENCE_SEED} is in seed_summary.csv.\n\n## Main table\n\n")
        table(f, main_names, ["NSE", "Bias", "RMSE"])
        f.write("\n## Monthly generation by fuel (OpenNEM, April 2005 to June 2020)\n\n")
        table(f, fuel_names, ["NSE", "Bias", "RMSE"])
        f.write("\n## Supplement\n\n")
        table(f, main_names + fuel_names, ["MAE", "NRMSE"])
        f.write("\n## Supplement: emissions on calendar years (as reported before 23 Nov 2026)\n\n"
                "The inventory counts July to June ('2017' = July 2016 to June 2017); these rows compare it with the "
                "model's calendar years, six months out of step. Windows 2005-2018 and 1999-2018; the July-June rows "
                "of the main table use 2005-2018 and 2000-2018 (2000 is the first whole July-June year after the "
                "1998 spin-up).\n\n")
        table(f, cal_names, ["NSE", "Bias", "RMSE", "MAE", "NRMSE"])
    print(open(os.path.join(out, "seed_summary.md"), encoding="utf-8").read())


if __name__ == "__main__":
    main()
