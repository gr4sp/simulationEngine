"""Plot a GR4SP run from the CSVs it wrote.

Reproduces the figures the Java run used to render through JFreeChart, but from
the committed CSV output instead of from the simulation's own memory. That means
a run's figures can be regenerated, or re-styled, without re-running the model --
and that the plots and the notebooks read exactly the same numbers.

Usage
-----
    python plot_run.py csv/VIC                  # write PNGs next to the CSVs, in plots/
    python plot_run.py csv/VIC --out figures    # choose the output directory
    python plot_run.py csv/VIC --seed 42        # pick one run when several share a folder
    python plot_run.py csv/VIC --list           # show what would be plotted, write nothing

The run directory is the one holding <AREA>SimDataMonthlySummary_seed_<N>.csv,
normally csv/<AREA>/ under the folderOutput in simulationSettings.

Which file feeds which figure
-----------------------------
    MonthlySummary          the aggregate series -- consumption, tariff, wholesale,
                            emissions, households, actors, production, unmet demand
    MonthlyPrices           per-market and per-fuel average prices
    MonthlyGensCapFactor*   per-generator capacity factors, in and off spot
    MonthlySystemProduction* per-generator production, in and off spot

The per-generator files are only written on a "full" reportGeneration run; those
figures are skipped with a note when the files are absent.
"""
import argparse
import glob
import os
import sys

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import pandas as pd

FIGSIZE = (16, 9)   # 1920x1080 at 120 dpi, matching the Java output
DPI = 120

# name -> (title, y-axis label, [columns]) against the monthly summary.
# Titles match the JFreeChart originals so figures stay recognisable.
SUMMARY_FIGURES = {
    "HouseholdConsumption": (
        "Total Households Consumption (MWh)", "MWh",
        ["Consumption (KWh) per household"]),
    "HouseholdTariff": (
        "Tariff (c/KWh)", "c/KWh",
        ["Avg Tariff (c/KWh) per household"]),
    "PrimaryWholesalePrice": (
        "30min Monthly Average Primary Wholesale ($/MWh)", "$/MWh",
        ["Primary Wholesale ($/MWh)"]),
    "HouseholdGHG": (
        "Total Households GHG (tCO2-e)", "tCO2-e",
        ["GHG Emissions (tCO2-e) per household"]),
    "NumberHouseholds": (
        "Number of Domestic Consumers (households)", "Num. Households",
        ["Number of Domestic Consumers (households)"]),
    "numActors": (
        "Number of Actors", "Num. Actors",
        ["Number of Active Actors"]),
    "SystemProductionAggregated": (
        "Aggregated System Production", "MWh",
        ["System Production Rooftop PV", "System Production Coal",
         "System Production Water", "System Production Wind",
         "System Production Gas", "System Production Solar",
         "System Production Battery"]),
    "SystemProductionSpot": (
        "In-Spot System Production", "MWh",
        ["System Production Primary Spot", "System Production Secondary Spot"]),
    "SystemProductionOffSpot": (
        "Off-Spot System Production", "MWh",
        ["System Production Off Spot"]),
    "unmetDemandMwh": (
        "Total Unmet Demand per Month (MWh)", "MWh",
        ["Primary Total Unmet Demand (MWh)", "Secondary Total Unmet Demand (MWh)"]),
    "unmetDemandHours": (
        "Number of hours with Unmet Demand per Month", "Num. Hours",
        ["Primary Total Unmet Demand (Hours)", "Secondary Total Unmet Demand (Hours)"]),
    "unmetDemandDays": (
        "Number of Days with Unmet Demand per Month", "Num. Days",
        ["Primary Total Unmet Demand (Days)", "Secondary Total Unmet Demand (Days)"]),
    "maxUnmetDemandMwhPerDay": (
        "For every month, maximum Unmet Demand within a single hour (MWh)", "MWh",
        ["Primary Max Unmet Demand Per Hour (MWh)",
         "Secondary Max Unmet Demand Per Hour (MWh)"]),
}


def find_run(run_dir, seed=None):
    """Locate one run's files. Returns (area, seed, {kind: path})."""
    pattern = os.path.join(run_dir, "*SimDataMonthlySummary_seed_*.csv")
    matches = sorted(glob.glob(pattern))
    if not matches:
        sys.exit("no *SimDataMonthlySummary_seed_*.csv in %s\n"
                 "Point this at a run directory, normally csv/<AREA>/." % run_dir)
    if seed is not None:
        matches = [m for m in matches if m.endswith("_seed_%s.csv" % seed)]
        if not matches:
            sys.exit("no run with seed %s in %s" % (seed, run_dir))
    if len(matches) > 1:
        seeds = [os.path.basename(m).split("_seed_")[1][:-4] for m in matches]
        sys.exit("several runs in %s (seeds %s); choose one with --seed"
                 % (run_dir, ", ".join(seeds)))

    base = os.path.basename(matches[0])
    area, rest = base.split("SimDataMonthlySummary_seed_", 1)
    run_seed = rest[:-len(".csv")]

    def path(kind):
        p = os.path.join(run_dir, "%sSimData%s_seed_%s.csv" % (area, kind, run_seed))
        return p if os.path.isfile(p) else None

    return area, run_seed, {
        "monthly": matches[0],
        "prices": path("MonthlyPrices"),
        "capfactor_in": path("MonthlyGensCapFactorInSpot"),
        "capfactor_off": path("MonthlyGensCapFactorOFFspot"),
        "production_in": path("MonthlySystemProductionInSpot"),
        "production_off": path("MonthlySystemProductionOFFspot"),
    }


def read_dated(path, date_col):
    df = pd.read_csv(path, na_values=["-", ""])
    df[date_col] = pd.to_datetime(df[date_col])
    return df.set_index(date_col)


def draw(out_dir, name, title, ylabel, frame, area, seed, legend):
    """One figure per call. Columns entirely absent from the run are dropped."""
    frame = frame.dropna(axis=1, how="all")
    if frame.empty or not len(frame.columns):
        return False
    fig, ax = plt.subplots(figsize=FIGSIZE)
    for col in frame.columns:
        ax.plot(frame.index, frame[col], linewidth=1.2, label=str(col))
    ax.set_title("%s area code: %s" % (title, area), fontsize=15)
    ax.set_xlabel("Year", fontsize=12)
    ax.set_ylabel(ylabel, fontsize=12)
    ax.grid(True, alpha=0.3)
    if legend and len(frame.columns) > 1:
        # Per-generator figures carry ~95 series; keep the legend readable.
        ncol = 1 if len(frame.columns) <= 12 else 3
        ax.legend(loc="upper left", fontsize=7 if ncol > 1 else 10, ncol=ncol)
    fig.tight_layout()
    dest = os.path.join(out_dir, "%s_seed_%s.png" % (name, seed))
    fig.savefig(dest, dpi=DPI)
    plt.close(fig)
    return True


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("run_dir", help="directory holding one run's CSVs, e.g. csv/VIC")
    ap.add_argument("--out", default=None,
                    help="output directory (default: <run_dir>/plots)")
    ap.add_argument("--seed", default=None,
                    help="seed to plot, when the directory holds several runs")
    ap.add_argument("--list", action="store_true",
                    help="report what would be plotted and exit")
    args = ap.parse_args()

    area, seed, files = find_run(args.run_dir, args.seed)
    print("run: %s seed %s" % (area, seed))
    for kind, path in files.items():
        print("  %-14s %s" % (kind, os.path.basename(path) if path else "(not written)"))
    if args.list:
        return

    out_dir = args.out or os.path.join(args.run_dir, "plots")
    os.makedirs(out_dir, exist_ok=True)

    monthly = read_dated(files["monthly"], "Time (Month)")
    written = skipped = 0

    for name, (title, ylabel, cols) in SUMMARY_FIGURES.items():
        present = [c for c in cols if c in monthly.columns]
        if not present:
            print("  skip %s (no matching columns)" % name)
            skipped += 1
            continue
        if draw(out_dir, name, title, ylabel, monthly[present], area, seed,
                legend=len(present) > 1):
            written += 1
        else:
            print("  skip %s (all values missing)" % name)
            skipped += 1

    if files["prices"]:
        prices = read_dated(files["prices"], "Date")
        if draw(out_dir, "PriceGenAvg", "Average Price ($/MWh)", "$/MWh",
                prices, area, seed, legend=True):
            written += 1
    else:
        print("  skip PriceGenAvg (no prices file)")
        skipped += 1

    per_generator = [
        ("GenCapFactorsSpot", "capfactor_in",
         "Spot Historic Generators Capacity Factors", "% Cap. Factor"),
        ("GenCapFactorsOffSpot", "capfactor_off",
         "Off-Spot Historic Generators Capacity Factors", "% Cap. Factor"),
    ]
    for name, key, title, ylabel in per_generator:
        if not files[key]:
            print("  skip %s (needs a reportGeneration \"full\" run)" % name)
            skipped += 1
            continue
        frame = read_dated(files[key], "Date")
        if draw(out_dir, name, title, ylabel, frame, area, seed, legend=True):
            written += 1

    print("wrote %d figures to %s%s"
          % (written, out_dir, " (%d skipped)" % skipped if skipped else ""))


if __name__ == "__main__":
    main()
