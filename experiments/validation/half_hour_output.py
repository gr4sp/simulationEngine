"""R9 (28 Nov 2026): checks of the half-hourly output switch (settings halfHourOutput, Arena.writeHalfHour).

Usage (from the repository root, build first: ./gradlew compileJava):
  python experiments/validation/half_hour_output.py run OUT_DIR [SEED=1]
      two runs of the base case, one after the other so that their run times compare: off_s<seed> (VIC.yaml as is)
      and on_s<seed> (VIC.yaml plus "halfHourOutput: true"); keeps the CSVs and the wall-clock time of each run
  python experiments/validation/half_hour_output.py analyse OUT_DIR [SEED=1] [ENSEMBLE_DIR=experiments/validation/ensemble_2026-11-24]
      writes OUT_DIR/half_hour_output.md:
      1. byte identity of the monthly and yearly CSVs of off and on against ENSEMBLE_DIR/run_s<seed>
      2. the monthly sums and means of the half-hourly file against the monthly summary, every month of the run
      3. file size and run time, switch on against off
"""
import filecmp, glob, json, os, shutil, subprocess, sys, time
import pandas as pd

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
FUELS = ["Rooftop PV", "Coal", "Water", "Wind", "Gas", "Solar", "Battery"]


def run_one(out, variant, seed):
    d = os.path.join(out, f"{variant}_s{seed}")
    if glob.glob(os.path.join(d, "VICSimDataYearSummary_seed_*.csv")):
        return f"{variant} s{seed}: done before"
    work = os.path.join(out, "_work", f"{variant}_s{seed}")
    shutil.copytree(os.path.join(REPO, "simulationSettings"), os.path.join(work, "simulationSettings"), dirs_exist_ok=True)
    if variant == "on":
        with open(os.path.join(work, "simulationSettings", "VIC.yaml"), "a", encoding="utf-8") as f:
            f.write("\nhalfHourOutput: true\n")
    cp = os.pathsep.join([os.path.join(REPO, "build", "classes", "java", "main"), os.path.join(REPO, "build", "resources", "main"),
                          os.path.join(REPO, "build", "runtime-libs", "*")])
    os.makedirs(d, exist_ok=True)
    t0 = time.time()
    with open(os.path.join(d, "stdout.txt"), "w") as log:
        r = subprocess.run(["java", "-Xmx3g", "-cp", cp, "core.Gr4spSim", "-seed", str(seed)], cwd=work,
                           stdout=log, stderr=subprocess.STDOUT)
    secs = time.time() - t0
    for f in glob.glob(os.path.join(work, "csv", "VIC", "*.csv")):
        shutil.copy(f, d)
    json.dump({"seconds": round(secs, 1), "exit": r.returncode}, open(os.path.join(d, "run_time.json"), "w"))
    return f"{variant} s{seed}: exit {r.returncode}, {secs:.0f} s"


def analyse(out, seed, ensemble):
    L = [f"# Half-hourly output switch (R9), base case, seed {seed}\n",
         f"Written by `experiments/validation/half_hour_output.py analyse {os.path.relpath(out, REPO)} {seed}`.\n",
         "## 1. Output only\n", f"| run | file | identical to `{os.path.relpath(os.path.join(ensemble, f'run_s{seed}'), REPO)}` |",
         "|---|---|---|"]
    ref = os.path.join(ensemble, f"run_s{seed}", "csv", "VIC")
    for v in ("off", "on"):
        for f in sorted(glob.glob(os.path.join(ref, "*.csv"))):
            g = os.path.join(out, f"{v}_s{seed}", os.path.basename(f))
            L.append(f"| {v}_s{seed} | {os.path.basename(f)} | {os.path.exists(g) and filecmp.cmp(f, g, shallow=False)} |")

    hf = os.path.join(out, f"on_s{seed}", f"VICSimDataHalfHour_seed_{seed}.csv")
    hf = hf if os.path.exists(hf) else hf + ".gz"
    h = pd.read_csv(hf, encoding="cp1252")
    h["month"] = h.time.str[:7]
    m = pd.read_csv(os.path.join(out, f"on_s{seed}", f"VICSimDataMonthlySummary_seed_{seed}.csv"))
    m["month"] = m["Time (Month)"].str[:7]
    m = m.set_index("month")

    # 2. Monthly consistency. Energy: MW x 0.5 h summed. Price: sum over N half-hours / (N + 1) (Arena, R6 finding 2)
    p, o = h[h.market.str.lower() == "primary"].groupby("month"), h[h.market == "off"].groupby("month")
    tot = h.groupby("month")
    dispatch = lambda g, cols: g[cols].sum().sum(axis=1) / 2.0
    checks = {
        "Primary Wholesale ($/MWh)": p["price ($/MWh)"].sum() / (p.size() + 1),
        "System Production Primary Spot": dispatch(p, [f + " (MW)" for f in FUELS + ["Other"]]),
        "System Production Off Spot": dispatch(o, [f + " (MW)" for f in FUELS + ["Other"]]),
        "Primary Total Unmet Demand (MWh)": p["unserved (MW)"].sum() / 2.0,
        "System GHG Emissions (tCO2-e)": tot["emissions (tCO2-e)"].sum(),
    }
    for f in FUELS:
        checks[f"System Production {f}"] = tot[f + " (MW)"].sum() / 2.0
    # Arena leaves half-hours with no market demand (<= 0.0001 MW, "demand in Spots was 0") out of the running mean while
    # still counting them, so the N + 1 rule holds only in months without such half-hours
    zero = h[(h.market.str.lower() == "primary") & (h["market demand (MW)"] <= 1e-4)].groupby("month").size()
    price = checks["Primary Wholesale ($/MWh)"]
    checks["Primary Wholesale ($/MWh), months without a zero-demand half-hour"] = price[~price.index.isin(zero.index)]
    checks["Primary Wholesale ($/MWh), 1999-2019"] = price[price.index.str[:4].astype(int) <= 2019]
    # market energy balance inside the file: dispatched = market demand - unserved - imports
    bal = (p[[f + " (MW)" for f in FUELS + ["Other"]]].sum().sum(axis=1)
           - (p["market demand (MW)"].sum() - p["unserved (MW)"].sum() - p["imports (MW)"].sum()))
    rows = []
    for col, s in checks.items():
        ref_s = m.loc[s.index, col.split(", ")[0]].astype(float)
        diff = (s - ref_s).abs()
        rel = diff / ref_s.abs().where(ref_s.abs() > 0)
        rows.append({"monthly output": col, "months": len(s), "largest |difference|": diff.max(),
                     "largest relative difference": rel.max(), "unit": "$/MWh" if "$" in col else ("t" if "GHG" in col else "MWh")})
    R = pd.DataFrame(rows)
    L.append(f"\n## 2. Monthly sums of the half-hourly file against the monthly summary ({h.month.nunique()} months, "
             f"{h.time.min()} to {h.time.max()})\n")
    L.append("The monthly CSV prints about seven significant figures, so differences are rounding when the relative "
             "difference is below about 1e-6. Price: the half-hourly prices of the month summed and divided by N + 1, "
             "as the model does (R6 finding 2). Rooftop PV is counted in both Rooftop PV and the off-market total.\n")
    L.append("| monthly output | unit | months | largest abs. difference | largest relative difference |")
    L.append("|---|---|---|---|---|")
    for _, r in R.iterrows():
        L.append(f"| {r['monthly output']} | {r['unit']} | {r['months']} | {r['largest |difference|']:.3g} | "
                 f"{r['largest relative difference']:.3g} |")
    L.append(f"\nMonths with a half-hour of no market demand (left out of the model's monthly mean price): {len(zero)} "
             f"({', '.join(zero.index)}).\n")
    L.append(f"Energy balance of the primary rows (dispatch - (market demand - unserved - imports)), largest monthly "
             f"|value|: {bal.abs().max() / 2.0:.3g} MWh. The clearing counts a gap as unserved only when "
             "ceil(offered) < floor(demand) (SpotMarket.computeMarketPrice), so up to about 1 MW in a half-hour is "
             "neither dispatched nor unserved.\n")

    # 3. Size and time
    t = {v: json.load(open(os.path.join(out, f"{v}_s{seed}", "run_time.json"))) for v in ("off", "on")}
    size = os.path.getsize(hf)
    L.append("## 3. Size and run time\n")
    L.append("| | switch off | switch on |\n|---|---|---|")
    L.append(f"| run time, wall clock (s) | {t['off']['seconds']} | {t['on']['seconds']} |")
    L.append(f"| half-hourly file ({'gzip' if hf.endswith('.gz') else 'CSV'}, MB) | - | {size / 1e6:.1f} |")
    L.append(f"| rows | - | {len(h)} |")
    L.append(f"\nThe two runs ran one after the other on the same machine. Period written: the whole run "
             f"(halfHourOutputFrom/To unset), {h.time.min()[:4]}-{h.time.max()[:4]}.\n")
    open(os.path.join(out, "half_hour_output.md"), "w", encoding="utf-8").write("\n".join(L) + "\n")
    print("\n".join(L))


if __name__ == "__main__":
    mode, out = sys.argv[1], os.path.abspath(sys.argv[2])
    seed = int(sys.argv[3]) if len(sys.argv) > 3 else 1
    if mode == "run":
        for v in ("off", "on"):
            print(run_one(out, v, seed), flush=True)
    else:
        analyse(out, seed, os.path.abspath(sys.argv[4]) if len(sys.argv) > 4
                else os.path.join(REPO, "experiments", "validation", "ensemble_2026-11-24"))
