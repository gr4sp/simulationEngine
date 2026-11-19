"""R3 step 4: the repaired base case over many random seeds, and the spread of every validation statistic.

The model draws breakdowns (forced outages) and other events from its random generator, so one seed is one possible
history. This runs the base case (simulationSettings/ as committed, classes from build/classes/java/main) for seeds
1..N in parallel, then computes the validation statistics of validation_statistics.py for each run, and reports the
reference seed (42) beside the median and the 5-95 % range over the seeds. Parameter uncertainty is not sampled here
(Angela, 11 Nov 2026: Morris pilot and the Saltelli sample-size check decide whether a parameter ensemble is rerun).

Usage: python seed_ensemble.py OUT_DIR [N_SEEDS=100] [PARALLEL=10]   (build first: ./gradlew compileJava)
  OUT_DIR/run_s<seed>/csv/VIC/*.csv   each run's year and monthly summaries (the other outputs are deleted)
  OUT_DIR/seed_statistics.csv         one row per seed and indicator: n, MAE, RMSE, Bias, NRMSE, NSE, r
  OUT_DIR/seed_summary.csv / .md      per indicator: seed 42, median, 5 % and 95 % over the seeds
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

REFERENCE_SEED = 42


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
    rows = [{"seed": seed, "indicator": k, **vs.statistics(s, o)} for k, (s, o, _, _) in ind.items()]
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
    return rows, yr


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

    rows, years = [], []
    for s in seeds:
        r, y = stats_for(out, s)
        rows += r
        years.append(y)
    st = pd.DataFrame(rows)
    st.to_csv(os.path.join(out, "seed_statistics.csv"), index=False)
    pd.concat(years).rename_axis("year").to_csv(os.path.join(out, "seed_years.csv"))
    summ = []
    for ind, g in st.groupby("indicator", sort=False):
        ref = g[g.seed == REFERENCE_SEED].iloc[0]
        for m in ("MAE", "RMSE", "Bias", "NRMSE", "NSE"):
            summ.append({"indicator": ind, "metric": m, "seed42": ref[m], "median": g[m].median(),
                         "p05": g[m].quantile(0.05), "p95": g[m].quantile(0.95), "seeds": len(g)})
    sm = pd.DataFrame(summ)
    sm.to_csv(os.path.join(out, "seed_summary.csv"), index=False)
    piv = sm.assign(cell=sm.apply(lambda r: f"{r.seed42:.2f} ({r.p05:.2f} to {r.p95:.2f})", axis=1)).pivot(
        index="indicator", columns="metric", values="cell")[["MAE", "RMSE", "Bias", "NRMSE", "NSE"]]
    piv = piv.reindex(list(dict.fromkeys(sm.indicator)))
    with open(os.path.join(out, "seed_summary.md"), "w", encoding="utf-8") as f:
        f.write(f"Repaired base case, seed {REFERENCE_SEED} (5-95 % range over {len(seeds)} seeds)\n\n")
        f.write("| Indicator | " + " | ".join(piv.columns) + " |\n|---|" + "---|" * len(piv.columns) + "\n")
        for ind, r in piv.iterrows():
            f.write(f"| {ind} | " + " | ".join(r.values) + " |\n")
    print(open(os.path.join(out, "seed_summary.md"), encoding="utf-8").read())


if __name__ == "__main__":
    main()
