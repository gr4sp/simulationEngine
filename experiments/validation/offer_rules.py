"""Which offer rule prices each generator, and what the off-market LCOE rule adds to the tariff (R6 question, 28 Nov 2026).

The diagnostic -Dgr4sp.offerRuleLog=<csv> (Arena.writeOfferRuleLog, branch feature/output-logs, output only) writes one row
per month and active generator: market (primary / secondary / off), the half-hours in which it offered under each branch
of Generator.offerPriceMWh (Generator.offerRule: arbitrage, srmc, shortRunCost, waterValue, storageMarkup, lcoe), its
generation and its LCOE-rule price (Generator.priceMWhLCOE, the price of off-market output).

Usage (from the repository root, build first: ./gradlew compileJava):
  python experiments/validation/offer_rules.py run OUT_DIR [SEED=1]
      two runs of the base case: off_s<seed> (switch unset) and log_s<seed> (switch set), keeping the CSVs and the log
  python experiments/validation/offer_rules.py analyse OUT_DIR [SEED=1] [ENSEMBLE_DIR=experiments/validation/ensemble_2026-11-24]
      checks that off and log reproduce ENSEMBLE_DIR/run_s<seed> byte for byte, then writes OUT_DIR/offer_rules.md and
      the tables behind it; the 100-seed part of the tariff split reads ENSEMBLE_DIR's run folders
"""
import concurrent.futures as cf, filecmp, glob, os, shutil, subprocess, sys
import pandas as pd

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RULES = ["arbitrage", "srmc", "shortRunCost", "waterValue", "storageMarkup", "lcoe"]
HH = [r + " (half-hours)" for r in RULES]


def run_one(out, variant, seed):
    d = os.path.join(out, f"{variant}_s{seed}")
    if glob.glob(os.path.join(d, "VICSimDataYearSummary_seed_*.csv")):
        return f"{variant} s{seed}: done before"
    work = os.path.join(out, "_work", f"{variant}_s{seed}")
    shutil.copytree(os.path.join(REPO, "simulationSettings"), os.path.join(work, "simulationSettings"), dirs_exist_ok=True)
    cp = os.pathsep.join([os.path.join(REPO, "build", "classes", "java", "main"), os.path.join(REPO, "build", "resources", "main"),
                          os.path.join(REPO, "build", "runtime-libs", "*")])
    os.makedirs(d, exist_ok=True)
    prop = [f"-Dgr4sp.offerRuleLog={os.path.join(d, 'offer_rules_log.csv')}"] if variant == "log" else []
    with open(os.path.join(d, "stdout.txt"), "w") as log:
        r = subprocess.run(["java", "-Xmx3g", *prop, "-cp", cp, "core.Gr4spSim", "-seed", str(seed)], cwd=work,
                           stdout=log, stderr=subprocess.STDOUT)
    for f in glob.glob(os.path.join(work, "csv", "VIC", "*.csv")):
        shutil.copy(f, d)
    return f"{variant} s{seed}: exit {r.returncode}"


def md(t, fmt="{}"):
    """Markdown table without the optional tabulate package."""
    head = [str(t.index.name or "")] + [str(c) for c in t.columns]
    rows = ["| " + " | ".join(head) + " |", "|" + "---|" * len(head)]
    for i, r in t.iterrows():
        rows.append("| " + " | ".join([str(i)] + [(fmt.format(v) if isinstance(v, float) else str(v)) for v in r]) + " |")
    return "\n".join(rows)


def analyse(out, seed, ensemble):
    L = []
    # 1. Byte identity of the three CSVs against the ensemble run (switch off, and switch on)
    ref = os.path.join(ensemble, f"run_s{seed}", "csv", "VIC")
    L.append(f"# Offer rules by generator, base case, seed {seed} (R6 question of 28 Nov 2026)\n")
    L.append(f"Written by `experiments/validation/offer_rules.py analyse {os.path.relpath(out, REPO)} {seed}`.\n")
    L.append("## Output-only check\n")
    L.append(f"| run | file | identical to `{os.path.relpath(ref, REPO)}` |")
    L.append("|---|---|---|")
    for v in ("off", "log"):
        for f in sorted(glob.glob(os.path.join(ref, "*.csv"))):
            g = os.path.join(out, f"{v}_s{seed}", os.path.basename(f))
            L.append(f"| {v}_s{seed} | {os.path.basename(f)} | {os.path.exists(g) and filecmp.cmp(f, g, shallow=False)} |")

    f = os.path.join(out, f"log_s{seed}", "offer_rules_log.csv")
    f = f if os.path.exists(f) else f + ".gz"  # committed gzipped
    x = pd.read_csv(f, encoding="cp1252")  # FileWriter writes the platform encoding
    x["year"] = x.month.str[:4].astype(int)
    x["offered"] = x[HH].sum(axis=1)

    # 2. Participation: off-market units by fuel and technology, count and capacity in December, energy share in the year
    L.append("\n## Off-market generators (market = off), by fuel and technology\n")
    L.append("Count and capacity: units active in December of the year. Energy: share of the year's generation of every "
             "active generator in the log (market and off-market, rooftop PV included).\n")
    for y in (1999, 2009, 2019):
        a = x[x.year == y]
        tot = a["generated (MWh)"].sum()
        dec = a[a.month == f"{y}-12"]
        o = a[a.market == "off"]
        t = (o.groupby(["fuel", "tech"])["generated (MWh)"].sum() / tot * 100).rename("energy (% of all generation)").to_frame()
        dd = dec[dec.market == "off"].groupby(["fuel", "tech"])
        t["units (count)"] = dd.size()
        t["capacity (MW)"] = dd["maxCapacity (MW)"].sum()
        t = t.fillna(0)[["units (count)", "capacity (MW)", "energy (% of all generation)"]]
        t.index = [f"{f} / {k}" for f, k in t.index]
        t.index.name = f"{y}: fuel / tech"
        t.loc["total"] = t.sum()
        L.append(md(t, "{:.2f}") + "\n")
    small = ["Biogas", "Diesel", "Dry wood", "Landfill"]
    m = x[(x.year <= 2019) & (x.market != "off")]
    sm = m[m.fuel.str.contains("|".join(small), case=False)]
    L.append(f"Market units (primary or secondary) whose fuel contains {', '.join(small)}, 1999-2019: "
             f"{sm.name.nunique()} ({', '.join(sorted(sm.name.unique())) or 'none'}).\n")
    L.append("Market units by fuel, technology and dispatch type, 1999-2019 (distinct names):\n")
    t = m.groupby(["fuel", "tech", "dispatch"]).agg(**{"units (count)": ("name", "nunique"),
                                                        "max capacity (MW)": ("maxCapacity (MW)", "max")})
    t.index = [f"{a} / {b} / {c}" for a, b, c in t.index]
    t.index.name = "fuel / tech / dispatch"
    L.append(md(t, "{:.1f}") + "\n")

    # 3. Rules used by market units, half-hours, by period and fuel
    def rules(a, title):
        t = a[a.market != "off"].groupby("fuel")[HH].sum()
        t.columns = RULES
        t.index.name = "fuel (half-hours offered)"
        L.append(f"\n## {title}\n")
        L.append(md(t, "{:.0f}") + "\n")
        lc = a[(a.market != "off") & (a["lcoe (half-hours)"] > 0)]
        if len(lc):
            u = lc.groupby(["name", "fuel"]).agg(**{"months (count)": ("month", "nunique"), "first": ("month", "min"),
                                                     "last": ("month", "max"), "half-hours": ("lcoe (half-hours)", "sum")})
            u.index = [f"{n} ({f})" for n, f in u.index]
            u.index.name = "market units on the LCOE rule"
            L.append(md(u) + "\n")
            mm = lc.groupby("month")["lcoe (half-hours)"].sum()
            L.append(f"Months with any LCOE offer: {len(mm)} ({', '.join(mm.index[:12])}{' ...' if len(mm) > 12 else ''}).\n")
        else:
            L.append("No market unit offered under the LCOE rule in this period.\n")
        sk = a[(a.market != "off") & (a["storageMarkup (half-hours)"] > 0)]
        if len(sk):
            L.append(f"storageMarkup (hydro without a monthly budget): {sk.month.nunique()} months, "
                     f"{sk.month.min()} to {sk.month.max()}, units {', '.join(sorted(sk.name.unique()))}.\n")
    rules(x[x.year < 1999], f"Offer rules of market units, before 1999 (spin-up), seed {seed}")
    rules(x[(x.year >= 1999) & (x.year <= 2019)], f"Offer rules of market units, 1999-2019, seed {seed}")
    rules(x[x.year >= 2020], f"Offer rules of market units, 2020-{x.year.max()} (VICfuture.yaml), seed {seed}")

    # 4. Off-market LCOE prices and their share of the tariff's wholesale term, 2019
    o = x[(x.market == "off") & (x.year == 2019)]
    o = o[o.fuel.str.lower() != "solar"]  # off-market solar enters the weighting with its surplus, always 0 (Arena)
    p = o.groupby("fuel").apply(lambda g: pd.Series({
        "energy (GWh)": g["generated (MWh)"].sum() / 1e3,
        "LCOE price, energy weighted ($/MWh)": ((g["generated (MWh)"] * g["lcoePrice ($/MWh)"]).sum() / g["generated (MWh)"].sum()
                                                if g["generated (MWh)"].sum() > 0 else float("nan")),
        "LCOE price min ($/MWh)": g["lcoePrice ($/MWh)"].min(), "LCOE price max ($/MWh)": g["lcoePrice ($/MWh)"].max()}))
    p["share of off-market energy (%)"] = 100 * p["energy (GWh)"] / p["energy (GWh)"].sum()
    p.index.name = "2019 off-market fuel (solar excluded)"
    L.append(f"\n## Off-market prices (LCOE rule), 2019, seed {seed}\n")
    L.append("Off-market solar (rooftop PV, small solar) enters the off-market price with its surplus, which is never set "
             "(Arena, off-market loop), so it carries no weight. The other off-market units carry their available capacity.\n")
    L.append(md(p, "{:.2f}") + "\n")

    # 100 seeds: all-markets - primary = w (P_off - P_primary), w = weight of the off-market demand
    rows = []
    for d in sorted(glob.glob(os.path.join(ensemble, "run_s*"))):
        s = int(d.rsplit("_s", 1)[1])
        pr = glob.glob(os.path.join(d, "csv", "VIC", "VICSimDataMonthlyPrices_seed_*.csv"))
        su = glob.glob(os.path.join(d, "csv", "VIC", "VICSimDataMonthlySummary_seed_*.csv"))
        if not (pr and su):
            continue
        a = pd.read_csv(pr[0]).rename(columns={"Date": "t"})[["t", "Tariff_WA_AllMarkets ($/MWh)", "OffSpot ($/MWh)"]]
        b = pd.read_csv(su[0]).rename(columns={"Time (Month)": "t"})[["t", "Primary Wholesale ($/MWh)"]]
        z = a.merge(b, on="t")
        z["year"] = z.t.str[:4].astype(int)
        z = z[z.year == 2019]
        z["diff"] = z["Tariff_WA_AllMarkets ($/MWh)"] - z["Primary Wholesale ($/MWh)"]
        z["gap"] = z["OffSpot ($/MWh)"] - z["Primary Wholesale ($/MWh)"]
        z["w"] = z["diff"] / z["gap"]
        rows.append(pd.Series({"seed": s, "all-markets ($/MWh)": z["Tariff_WA_AllMarkets ($/MWh)"].mean(),
                               "primary ($/MWh)": z["Primary Wholesale ($/MWh)"].mean(),
                               "off-market price ($/MWh)": z["OffSpot ($/MWh)"].mean(),
                               "off-market weight (%)": 100 * z["w"].mean(),
                               "all-markets - primary ($/MWh)": z["diff"].mean()}))
    E = pd.DataFrame(rows)
    E.to_csv(os.path.join(out, "offer_rules_tariff_2019.csv"), index=False)
    q = E.drop(columns="seed").quantile([0.05, 0.5, 0.95]).T
    q.columns = ["5 %", "median", "95 %"]
    q.index.name = f"2019, monthly means, {len(E)} seeds of {os.path.basename(os.path.normpath(ensemble))}"
    L.append(f"\n## The LCOE rule's share of the tariff's wholesale term, 2019\n")
    L.append("All-markets price = (primary price x primary demand + off-market price x off-market demand) / (sum) "
             "(Arena.getWholesalePrice). The weight is recovered per month as (all-markets - primary) / (off-market - primary).\n")
    L.append(md(q, "{:.2f}") + "\n")
    # Split by fuel, seed 1: weight x share of off-market energy x (fuel's LCOE price - primary price), 2019 means
    e1 = E[E.seed == seed].iloc[0]
    w, pp = e1["off-market weight (%)"] / 100, e1["primary ($/MWh)"]
    c = pd.DataFrame({"share of off-market energy (%)": p["share of off-market energy (%)"],
                      "LCOE price ($/MWh)": p["LCOE price, energy weighted ($/MWh)"]})
    c["contribution to all-markets - primary ($/MWh)"] = (w * c.iloc[:, 0] / 100 * (c.iloc[:, 1] - pp)).fillna(0.0)
    c.loc["sum"] = [c.iloc[:, 0].sum(), (c.iloc[:, 0] * c.iloc[:, 1].fillna(0)).sum() / 100, c.iloc[:, 2].sum()]
    c.index.name = f"seed {seed}, 2019 (weight {100 * w:.2f} %, primary {pp:.2f} $/MWh)"
    L.append(f"By fuel, seed {seed}: contribution = weight x share of off-market energy x (fuel's LCOE price - primary "
             f"price). Compare the sum with seed {seed}'s all-markets - primary, {e1['all-markets - primary ($/MWh)']:+.2f} "
             f"$/MWh, and its off-market price, {e1['off-market price ($/MWh)']:.2f} $/MWh (the split uses energy shares; "
             "the model weights each half-hour by available capacity).\n")
    L.append(md(c, "{:.2f}") + "\n")
    open(os.path.join(out, "offer_rules.md"), "w", encoding="utf-8").write("\n".join(L) + "\n")
    print("\n".join(L))


if __name__ == "__main__":
    mode, out = sys.argv[1], os.path.abspath(sys.argv[2])
    seed = int(sys.argv[3]) if len(sys.argv) > 3 else 1
    if mode == "run":
        with cf.ThreadPoolExecutor(2) as ex:
            for r in ex.map(lambda v: run_one(out, v, seed), ["off", "log"]):
                print(r)
    else:
        analyse(out, seed, os.path.abspath(sys.argv[4]) if len(sys.argv) > 4
                else os.path.join(REPO, "experiments", "validation", "ensemble_2026-11-24"))
