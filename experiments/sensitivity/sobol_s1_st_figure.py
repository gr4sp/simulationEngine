"""Regenerate the Sobol S1-vs-ST figure that replaces the EET screening scatter.

The article's sensitivity argument is the gap between first-order (S1) and
total-order (ST) indices: emissions and renewable share respond to single
levers, while wholesale prices and tariffs move only under combinations. The
Elementary Effects scatter it replaces cannot show that - screening only
justifies which parameters were carried forward.

Source
------
The indices come from the *sensitivity* ensemble: Saltelli, N = 2100, k = 29
parameters, C = N(2k+2) = 126,000 runs, archived as
`gr4sp_SOBOL2021-Feb-03.tar.gz` (the afterBaseYear set). They are NOT from the
105,000-run hypothetical-past ensemble used for the validation bands - see
`../validation/README.md`, which explains why the two are not interchangeable.

`ema_gr4sp_SOBOL_ABY.ipynb` reads that archive, runs `SALib.analyze.sobol` per
analysis year and writes the median across years from the 2019 base year onward
to `../notebookGr4sp/outputs/data/SOBOL__median_sensitivity_Indices.xlsx`. That
workbook is committed, so this script reproduces the figure without the 589 MB
archive.

    python sobol_s1_st_figure.py                 # write sobolS1STFourOoi.png here
    python sobol_s1_st_figure.py --outdir ../notebookGr4sp/outputs/figs
    python sobol_s1_st_figure.py --check         # verify against Table 5, no figure

Requires pandas and openpyxl; matplotlib as well unless --check is given.
"""
import argparse
import os
import sys

import pandas as pd

HERE = os.path.dirname(os.path.abspath(__file__))
WORKBOOK = os.path.join(
    HERE, os.pardir, "notebookGr4sp", "outputs", "data",
    "SOBOL__median_sensitivity_Indices.xlsx")

# Panel order matches the EET figure this one replaces.
PANELS = [
    ("GHGYear", "GHGE"),
    ("renewableContributionYear", "Renewable energy share"),
    ("PrimarySpot-WholesalePriceYear", "Wholesale price"),
    ("tariffsYear", "Retail tariffs"),
]

TOP_N = 10

# The published table uses prose names, so the figure must match it rather than
# the code identifiers. Note includePublicallyAnnouncedGen is misspelled in the
# model definition; the label spells "announced" correctly.
LABELS = {
    "scheduleMinCapMarketGen": "Scheduled gen. capacity threshold",
    "includePublicallyAnnouncedGen": "Include announced generation",
    "nameplateCapacityChangeBrownCoal": "Brown coal capacity change",
    "generationRolloutPeriod": "New gen. rollout period",
    "semiScheduleGenSpotMarket": "Semi-scheduled gen. in spot market",
    "domesticConsumptionPercentage": "Domestic consumption share",
    "priceChangePercentageBrownCoal": "Brown coal price change",
    "nameplateCapacityChangeWind": "Wind capacity change",
    "consumption": "Annual consumption",
    "generatorRetirement": "Generator retirement",
    "importPriceFactor": "Import price factor",
    "annualInflation": "Annual inflation",
    "annualCpi": "Annual CPI",
    "energyEfficiency": "Energy efficiency",
    "learningCurve": "Learning curve",
    "nonScheduleGenSpotMarket": "Non-scheduled gen. in spot market",
    "nameplateCapacityChangeWater": "Hydro capacity change",
    "nameplateCapacityChangeBattery": "Battery capacity change",
    "priceChangePercentageBattery": "Battery price change",
    "priceChangePercentageWind": "Wind price change",
    # Not in the article's list, but it appears in two panels and the listed
    # names give it unambiguously: Water is Hydro, priceChangePercentage is
    # "price change" (cf. Brown coal / Wind / Battery price change).
    "priceChangePercentageWater": "Hydro price change",
    "wholesaleTariffContribution": "Wholesale cost share of tariff",
}

# Table 5 of the article: the top five by ST for three of the four outputs.
TABLE5 = {
    "PrimarySpot-WholesalePriceYear": [
        ("scheduleMinCapMarketGen", 0.053, 0.309),
        ("includePublicallyAnnouncedGen", 0.020, 0.308),
        ("nameplateCapacityChangeBrownCoal", 0.025, 0.304),
        ("generationRolloutPeriod", 0.029, 0.271),
        ("semiScheduleGenSpotMarket", 0.019, 0.183),
    ],
    "GHGYear": [
        ("semiScheduleGenSpotMarket", 0.147, 0.314),
        ("includePublicallyAnnouncedGen", 0.156, 0.244),
        ("domesticConsumptionPercentage", 0.128, 0.182),
        ("priceChangePercentageBrownCoal", 0.017, 0.098),
        ("nameplateCapacityChangeWind", 0.042, 0.091),
    ],
    "renewableContributionYear": [
        ("semiScheduleGenSpotMarket", 0.186, 0.437),
        ("includePublicallyAnnouncedGen", 0.165, 0.275),
        ("priceChangePercentageBrownCoal", 0.042, 0.157),
        ("nameplateCapacityChangeWind", 0.055, 0.107),
        ("nameplateCapacityChangeBrownCoal", 0.054, 0.104),
    ],
}


def pretty(name):
    """Prose label for a parameter, falling back to sentence case."""
    if name in LABELS:
        return LABELS[name]
    out = name[0].upper()
    for ch in name[1:]:
        out += " " + ch.lower() if ch.isupper() else ch
    return out.replace("generation", "gen.")


def read_indices(path=WORKBOOK):
    """Return {outcome: DataFrame(param, S1, ST, S1_conf, ST_conf)}.

    The workbook is a concat of S1/ST/S2/S1_conf/ST_conf blocks written with a
    two-row MultiIndex header, so the scalar columns sit at fixed offsets: S1
    and ST first, then the k-wide S2 matrix, then their confidence intervals.
    """
    book = pd.ExcelFile(path)
    frames = {}
    for sheet in book.sheet_names:
        raw = book.parse(sheet)
        k = (raw.shape[1] - 6) // 2      # width of one S2 block
        cols = [0, 1, 2, 3 + k + 1, 3 + k + 2]
        frame = raw.iloc[2:, cols].copy()
        frame.columns = ["param", "S1", "ST", "S1_conf", "ST_conf"]
        frame = frame.dropna(subset=["param"]).reset_index(drop=True)
        for col in ("S1", "ST", "S1_conf", "ST_conf"):
            frame[col] = pd.to_numeric(frame[col])
        frames[sheet] = frame
    return frames


def check(frames):
    """Verify the top five by ST against Table 5. Returns a list of problems."""
    problems = []
    for outcome, expected in TABLE5.items():
        frame = frames[outcome]
        top = frame.sort_values("ST", ascending=False).head(len(expected))
        got = list(zip(top["param"], top["S1"], top["ST"]))
        for rank, ((ename, es1, est), (gname, gs1, gst)) in enumerate(
                zip(expected, got), start=1):
            if gname != ename:
                problems.append(
                    "{} rank {}: expected {}, got {}".format(
                        outcome, rank, ename, gname))
                continue
            if abs(round(gs1, 3) - es1) > 5e-4 or abs(round(gst, 3) - est) > 5e-4:
                problems.append(
                    "{} {}: expected S1 {:.3f} / ST {:.3f}, got {:.3f} / {:.3f}".format(
                        outcome, ename, es1, est, gs1, gst))
    # Acceptance check 2: no plotted parameter may have ST of zero.
    for outcome, _ in PANELS:
        top = frames[outcome].sort_values("ST", ascending=False).head(TOP_N)
        zeros = top.loc[top["ST"] <= 0, "param"].tolist()
        if zeros:
            problems.append("{}: ST <= 0 among the top {}: {}".format(
                outcome, TOP_N, ", ".join(zeros)))
    return problems


def figure(frames, outdir, filename="sobolS1STFourOoi.png"):
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    import numpy as np

    os.makedirs(outdir, exist_ok=True)
    # Sized for about 0.9\linewidth in the single-column cas-sc layout: 16 cm
    # across at 150 dpi is roughly 1750 px.
    fig, axes = plt.subplots(2, 2, figsize=(11.7, 9.5))
    bar_h = 0.38

    for axis, (outcome, title) in zip(axes.ravel(), PANELS):
        top = frames[outcome].sort_values("ST", ascending=False).head(TOP_N)
        # Largest at the top: matplotlib's y axis grows upward, so plot the
        # reversed order and let position 0 be the smallest.
        top = top.iloc[::-1]
        pos = np.arange(len(top))

        axis.barh(pos + bar_h / 2, top["ST"], height=bar_h,
                  xerr=top["ST_conf"], color="#4c72b0", label="$S_T$",
                  error_kw={"elinewidth": 1.0, "capsize": 2.0, "ecolor": "0.3"})
        axis.barh(pos - bar_h / 2, top["S1"], height=bar_h,
                  xerr=top["S1_conf"], color="#dd8452", label="$S_1$",
                  error_kw={"elinewidth": 1.0, "capsize": 2.0, "ecolor": "0.3"})

        axis.set_yticks(pos)
        axis.set_yticklabels([pretty(p) for p in top["param"]], size=11)
        axis.set_title(title, size=14)
        axis.tick_params(axis="x", labelsize=11)
        axis.set_xlim(left=0)
        axis.grid(axis="x", color="0.85", linewidth=0.7)
        axis.set_axisbelow(True)
        for side in ("top", "right"):
            axis.spines[side].set_visible(False)

    for axis in axes[1]:
        axis.set_xlabel("Sobol index", size=13)

    handles, labels = axes[0][0].get_legend_handles_labels()
    order = [labels.index("$S_1$"), labels.index("$S_T$")]
    fig.legend([handles[i] for i in order], [labels[i] for i in order],
               loc="lower center", ncol=2, prop={"size": 14}, frameon=False,
               bbox_to_anchor=(0.5, -0.005))

    fig.tight_layout(rect=(0, 0.035, 1, 1))
    path = os.path.join(outdir, filename)
    fig.savefig(path, bbox_inches="tight", dpi=150)
    plt.close(fig)
    return path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--outdir", default=HERE,
                        help="where to write the figure (default: this folder)")
    parser.add_argument("--check", action="store_true",
                        help="verify against Table 5 and exit without plotting")
    parser.add_argument("--workbook", default=WORKBOOK)
    args = parser.parse_args()

    frames = read_indices(args.workbook)
    problems = check(frames)
    if problems:
        print("Indices do NOT match Table 5 - not regenerating the figure:")
        for problem in problems:
            print("  " + problem)
        return 1
    print("Table 5 check passed: top five by ST reproduce for wholesale price, "
          "GHGE and renewable share; no plotted parameter has ST <= 0.")

    if args.check:
        return 0
    print("wrote " + figure(frames, args.outdir))
    return 0


if __name__ == "__main__":
    sys.exit(main())
