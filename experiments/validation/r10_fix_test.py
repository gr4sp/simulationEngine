"""R10 (28 Nov 2026): what the two candidate fixes would change, measured on paired seeds. Test branch test/r10-fixes.

Variants (each against the base case of ensemble_2026-11-24, same seed; the base runs are its local run folders):
  fix1  wind outside the record: -Dgr4sp.windOutsideRecord=true (LoadData.unrecordedOffMarketGWh, test only). In months
        where the record (OpenElectricity, generation_consumption_historic.wind_gwh) holds no wind, to November 2008,
        the expected output of every wind row (capacity x capacity factor) is added to the monthly target.
  fix2  registered classification: minCapMarketGen 0 for scheduled and semi-scheduled units in VIC.yaml and
        VICfuture.yaml (settings only). Every S and SS row is centrally dispatched, whatever its size (NER 2.2.2(b1),
        2.2.7(e)); non-scheduled units stay off-market. Moves Hume Dam VIC (29 MW) and Waubra's 22.5 MW row.
  both  fix1 + fix2.
  base  switch unset, settings as committed: checks that this branch reproduces the ensemble (seed 42 only).

Usage (from the repository root, build first: ./gradlew compileJava):
  python experiments/validation/r10_fix_test.py run OUT_DIR [SEEDS=42,1,2,3,4,5] [PARALLEL=6]
  python experiments/validation/r10_fix_test.py compare OUT_DIR
"""
import concurrent.futures as cf, filecmp, glob, os, re, shutil, subprocess, sys
import pandas as pd

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ENS = os.path.join(REPO, "experiments", "validation", "ensemble_2026-11-24")
COLS = {"Primary Wholesale ($/MWh)": "price", "System Production Coal": "coal", "System Production Gas": "gas",
        "System Production Wind": "wind", "System Production Water": "hydro", "System Production Primary Spot": "market",
        "System GHG Emissions (tCO2-e)": "emissions", "Primary Total Unmet Demand (MWh)": "unserved",
        "Avg Tariff (c/KWh) per household": "tariff"}
UNITS = {"price": "$/MWh", "coal": "GWh", "gas": "GWh", "wind": "GWh", "hydro": "GWh", "market": "GWh",
         "emissions": "Mt", "unserved": "MWh", "tariff": "c/kWh"}
VARIANTS = ("fix1", "fix2", "both")


def settings_fix2(folder):
    for name in ("VIC.yaml", "VICfuture.yaml"):
        p = os.path.join(folder, name)
        s = open(p, encoding="utf-8", newline="").read()  # keep the file's line endings
        s, n = re.subn(r"(\r?\n  (?:scheduled|semiScheduled):\r?\n(?:    #[^\r\n]*\r?\n)?    spotMarket: primary\r?\n"
                       r"    minCapMarketGen: )30", r"\g<1>0", s)
        assert n == 2, (name, n)
        open(p, "w", encoding="utf-8", newline="").write(s)


def run_one(out, variant, seed):
    d = os.path.join(out, f"{variant}_s{seed}")
    if glob.glob(os.path.join(d, "VICSimDataYearSummary_seed_*.csv")):
        return f"{variant} s{seed}: done before"
    work = os.path.join(out, "_work", f"{variant}_s{seed}")
    shutil.copytree(os.path.join(REPO, "simulationSettings"), os.path.join(work, "simulationSettings"), dirs_exist_ok=True)
    if variant in ("fix2", "both"):
        settings_fix2(os.path.join(work, "simulationSettings"))
    prop = ["-Dgr4sp.windOutsideRecord=true"] if variant in ("fix1", "both") else []
    cp = os.pathsep.join([os.path.join(REPO, "build", "classes", "java", "main"), os.path.join(REPO, "build", "resources", "main"),
                          os.path.join(REPO, "build", "runtime-libs", "*")])
    os.makedirs(d, exist_ok=True)
    with open(os.path.join(d, "stdout.txt"), "w") as log:
        r = subprocess.run(["java", "-Xmx3g", *prop, "-cp", cp, "core.Gr4spSim", "-seed", str(seed)], cwd=work,
                           stdout=log, stderr=subprocess.STDOUT)
    for f in glob.glob(os.path.join(work, "csv", "VIC", "*.csv")):
        shutil.copy(f, d)
    return f"{variant} s{seed}: exit {r.returncode}"


def md(t, fmt):
    head = [str(t.index.name or "")] + [str(c) for c in t.columns]
    rows = ["| " + " | ".join(head) + " |", "|" + "---|" * len(head)]
    for i, r in t.iterrows():
        rows.append("| " + " | ".join([str(i)] + [fmt.format(v) for v in r]) + " |")
    return "\n".join(rows)


def year(path):
    f = pd.read_csv(path)
    f = f.rename(columns={f.columns[0]: "year"}).set_index("year")[list(COLS)].rename(columns=COLS)
    for k in ("coal", "gas", "wind", "hydro", "market"):
        f[k] /= 1e3
    f["emissions"] /= 1e6
    return f.loc[1999:2019]


def compare(out):
    seeds = sorted({int(p.rsplit("_s", 1)[1]) for p in glob.glob(os.path.join(out, "fix1_s*"))})
    L = ["# R10: what the two candidate fixes change (variant minus base case, same seed)\n",
         f"Seeds {', '.join(map(str, seeds))}. Written by `experiments/validation/r10_fix_test.py compare "
         f"{os.path.relpath(out, REPO)}`. Base: `ensemble_2026-11-24/run_s<seed>` (local run folders). Branch "
         "`test/r10-fixes`; the fixes are not in the release branch.\n"]
    b42 = os.path.join(out, "base_s42", "VICSimDataYearSummary_seed_42.csv")
    if os.path.exists(b42):
        same = all(filecmp.cmp(os.path.join(out, "base_s42", f"VICSimData{k}_seed_42.csv"),
                               os.path.join(ENS, "run_s42", "csv", "VIC", f"VICSimData{k}_seed_42.csv"), shallow=False)
                   for k in ("YearSummary", "MonthlySummary", "MonthlyPrices"))
        L.append(f"This branch with both fixes off, seed 42: the three CSVs are "
                 f"{'identical byte for byte to' if same else 'DIFFERENT from'} `ensemble_2026-11-24/run_s42`.\n")
    y = pd.read_csv(os.path.join(ENS, "seed_years.csv"))
    band = y[(y.year >= 1999) & (y.year <= 2019)].groupby("year").price
    band = band.quantile(0.95) - band.quantile(0.05)
    rows = []
    for v in VARIANTS:
        for s in seeds:
            base = year(os.path.join(ENS, f"run_s{s}", "csv", "VIC", f"VICSimDataYearSummary_seed_{s}.csv"))
            var = year(os.path.join(out, f"{v}_s{s}", f"VICSimDataYearSummary_seed_{s}.csv"))
            rows.append((var - base).assign(variant=v, seed=s).reset_index())
    D = pd.concat(rows)
    D.to_csv(os.path.join(out, "r10_fix_test_years.csv"), index=False)
    for v in VARIANTS:
        d = D[D.variant == v]
        L.append(f"\n## {v}: mean over the {len(seeds)} seeds, by year (variant - base)\n")
        t = d.groupby("year")[list(UNITS)].mean()
        t.columns = [f"{k} ({UNITS[k]})" for k in UNITS]
        L.append(md(t, "{:+.3f}") + "\n")
        p = d.pivot(index="year", columns="seed", values="price")
        c = pd.DataFrame({"mean ($/MWh)": p.mean(axis=1), "min ($/MWh)": p.min(axis=1), "max ($/MWh)": p.max(axis=1),
                          "100-seed band width ($/MWh)": band, "mean / band width (%)": 100 * p.mean(axis=1) / band})
        L.append(f"Price, {v}, against the width of the 5-95 % band of the 100 seeds (`ensemble_2026-11-24/seed_years.csv`):\n")
        L.append(md(c, "{:+.2f}") + "\n")
    open(os.path.join(out, "r10_fix_test.md"), "w", encoding="utf-8").write("\n".join(L) + "\n")
    print("\n".join(L))


if __name__ == "__main__":
    mode, out = sys.argv[1], os.path.abspath(sys.argv[2])
    if mode == "run":
        seeds = [int(s) for s in (sys.argv[3] if len(sys.argv) > 3 else "42,1,2,3,4,5").split(",")]
        par = int(sys.argv[4]) if len(sys.argv) > 4 else 6
        jobs = [(v, s) for s in seeds for v in VARIANTS] + [("base", 42)] * (42 in seeds)
        with cf.ThreadPoolExecutor(par) as ex:
            for msg in ex.map(lambda j: run_one(out, *j), jobs):
                print(msg, flush=True)
    else:
        compare(out)
