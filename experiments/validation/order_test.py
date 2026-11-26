"""Order of SimPolicies and the spot Arena within a month (R6 finding 1, 26 Nov 2026).

SimPolicies and the arenas share MASON ordering 0 and MASON shuffles equal orderings, so rooftop PV installed in a month
enters the market that month or the next, by the seed. The switch -Dgr4sp.policiesOrder=before|after (Gr4spSim.start,
branch test/schedule-order) runs SimPolicies always before / always after the arenas. Both variants draw the same random
numbers, so after - before is the effect of the order alone: the full effect of rooftop PV arriving a month late. The base
case (switch unset) is late in about half the months.

Usage (from the repository root, build first: ./gradlew compileJava):
  python experiments/validation/order_test.py run OUT_DIR [SEEDS=42,1,2,3,4,5] [PARALLEL=6]
      runs before_s<seed>, after_s<seed> and default_s<seed> (switch unset) into OUT_DIR, keeping the three CSVs
  python experiments/validation/order_test.py compare OUT_DIR [ENSEMBLE_DIR=experiments/validation/ensemble_2026-11-20]
      writes OUT_DIR/order_test.md and order_test_years.csv; checks default_s42 against ENSEMBLE_DIR's run_s42 when present

Committed results: experiments/validation/order_test_2026-11-26/ (seeds 42 and 1-5; default for seed 42 only).
"""
import concurrent.futures as cf, filecmp, glob, os, shutil, subprocess, sys
import pandas as pd

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
COLS = {"Primary Wholesale ($/MWh)": "price", "System GHG Emissions (tCO2-e)": "emissions",
        "System Production Primary Spot": "market", "System Production Rooftop PV": "rooftop",
        "Primary Total Unmet Demand (MWh)": "unserved", "Avg Tariff (c/KWh) per household": "tariff"}
UNITS = {"price": "$/MWh", "emissions": "Mt", "market": "GWh", "rooftop": "GWh", "unserved": "MWh", "tariff": "c/kWh"}


def run_one(out, variant, seed):
    d = os.path.join(out, f"{variant}_s{seed}")
    if glob.glob(os.path.join(d, "VICSimDataYearSummary_seed_*.csv")):
        return f"{variant} s{seed}: done before"
    work = os.path.join(out, "_work", f"{variant}_s{seed}")
    shutil.copytree(os.path.join(REPO, "simulationSettings"), os.path.join(work, "simulationSettings"), dirs_exist_ok=True)
    cp = os.pathsep.join([os.path.join(REPO, "build", "classes", "java", "main"), os.path.join(REPO, "build", "resources", "main"),
                          os.path.join(REPO, "build", "runtime-libs", "*")])
    prop = [] if variant == "default" else [f"-Dgr4sp.policiesOrder={variant}"]
    with open(os.path.join(work, "stdout.txt"), "w") as log:
        r = subprocess.run(["java", "-Xmx3g", *prop, "-cp", cp, "core.Gr4spSim", "-seed", str(seed)], cwd=work,
                           stdout=log, stderr=subprocess.STDOUT)
    os.makedirs(d, exist_ok=True)
    for f in glob.glob(os.path.join(work, "csv", "VIC", "*.csv")):
        shutil.copy(f, d)
    return f"{variant} s{seed}: exit {r.returncode}"


def md(t, fmt):
    """Markdown table without the optional tabulate package."""
    head = [str(t.index.name or "")] + [str(c) for c in t.columns]
    rows = ["| " + " | ".join(head) + " |", "|" + "---|" * len(head)]
    for i, r in t.iterrows():
        rows.append("| " + " | ".join([str(i)] + [fmt.format(v) for v in r]) + " |")
    return "\n".join(rows)


def year(out, variant, seed):
    f = pd.read_csv(os.path.join(out, f"{variant}_s{seed}", f"VICSimDataYearSummary_seed_{seed}.csv"))
    f = f.rename(columns={f.columns[0]: "year"}).set_index("year")[list(COLS)].rename(columns=COLS)
    f["emissions"] /= 1e6; f["market"] /= 1e3; f["rooftop"] /= 1e3
    return f.loc[1999:2019]


def compare(out, ensemble):
    seeds = sorted(int(p.rsplit("_s", 1)[1]) for p in glob.glob(os.path.join(out, "before_s*")))
    rows, lines = [], []
    for s in seeds:
        b, a = year(out, "before", s), year(out, "after", s)
        d = a - b
        if (d.loc[:2006].abs() > 1e-9).any().any():
            raise SystemExit(f"seed {s}: before and after differ before 2007")
        d["price_pct"] = 100 * d["price"] / b["price"]
        d["rooftop_pct"] = 100 * d["rooftop"] / b["rooftop"]
        d["market_pct"] = 100 * d["market"] / b["market"]
        rows.append(d.assign(seed=s).reset_index())
    D = pd.concat(rows)
    D.to_csv(os.path.join(out, "order_test_years.csv"), index=False)

    band = None
    yfile = os.path.join(ensemble, "seed_years.csv")
    if os.path.exists(yfile):
        y = pd.read_csv(yfile)
        y = y[(y.year >= 1999) & (y.year <= 2019)].groupby("year").price
        band = y.quantile(0.95) - y.quantile(0.05)

    lines.append("# Order of SimPolicies and the spot Arena: always after minus always before\n")
    lines.append(f"Seeds {', '.join(map(str, seeds))}. Written by `experiments/validation/order_test.py compare`. "
                 "Every seed is identical before 2007 (no rooftop PV). The shuffled base case is late in about half the "
                 "months, so expect about half of these values.\n")
    default = os.path.join(out, "default_s42", "VICSimDataYearSummary_seed_42.csv")
    ref = os.path.join(REPO, "experiments", "validation", "ensemble_2026-11-24", "run_s42", "csv", "VIC",
                       "VICSimDataYearSummary_seed_42.csv")
    if os.path.exists(default) and os.path.exists(ref):
        same = all(filecmp.cmp(default.replace("YearSummary", k), ref.replace("YearSummary", k), shallow=False)
                   for k in ("YearSummary", "MonthlySummary"))
        lines.append(f"Switch unset, seed 42: year and monthly summaries {'byte-identical to' if same else 'DIFFER from'} "
                     "`ensemble_2026-11-24/run_s42` (local run folder, not committed).\n")
    committed = os.path.join(REPO, "experiments", "validation", "ensemble_2026-11-24", "seed_years.csv")
    if os.path.exists(default) and os.path.exists(committed):
        e = pd.read_csv(committed).query("seed == 42").set_index("year").loc[1999:2019]
        dflt = year(out, "default", 42)
        gap = max((dflt["price"] - e["price"]).abs().max(), (dflt["emissions"] - e["ghge_mt"]).abs().max())
        lines.append(f"Switch unset, seed 42, against the committed `ensemble_2026-11-24/seed_years.csv` (price, "
                     f"emissions 1999-2019): largest difference {gap:.2e}.\n")
    for k in ("price", "price_pct", "rooftop", "market", "emissions", "unserved", "tariff"):
        t = D.pivot(index="year", columns="seed", values=k).loc[2007:]
        t["mean"] = t[seeds].mean(axis=1); t["min"] = t[seeds].min(axis=1); t["max"] = t[seeds].max(axis=1)
        unit = "%" if k.endswith("pct") else UNITS[k]
        lines.append(f"\n## {k} ({unit}), after - before\n\n" + md(t, "{:+.3f}") + "\n")
    if band is not None:
        m = D.groupby("year").price.mean().loc[2007:]
        c = pd.DataFrame({"mean effect ($/MWh)": m, "seed band 5-95 % width ($/MWh)": band.loc[2007:],
                          "effect / band (%)": 100 * m / band.loc[2007:]})
        lines.append(f"\n## Price effect against the width of the 100-seed band (`{os.path.relpath(yfile, REPO)}`)\n\n"
                     f"Median width over 1999-2019: {band.median():.1f} $/MWh.\n\n" + md(c, "{:.2f}") + "\n")
    with open(os.path.join(out, "order_test.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print("\n".join(lines))


if __name__ == "__main__":
    mode, out = sys.argv[1], os.path.abspath(sys.argv[2])
    if mode == "run":
        seeds = [int(s) for s in (sys.argv[3] if len(sys.argv) > 3 else "42,1,2,3,4,5").split(",")]
        par = int(sys.argv[4]) if len(sys.argv) > 4 else 6
        jobs = [(v, s) for s in seeds for v in ("before", "after")] + [("default", 42)] * (42 in seeds)
        with cf.ThreadPoolExecutor(par) as ex:
            for msg in ex.map(lambda j: run_one(out, *j), jobs):
                print(msg, flush=True)
    compare(out, sys.argv[3] if mode == "compare" and len(sys.argv) > 3
            else os.path.join(REPO, "experiments", "validation", "ensemble_2026-11-20"))
