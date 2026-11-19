"""Validation figures with the spread over random seeds (request R6, MethodsX Figures 2-5).

Each figure shows the record, the median over the seed ensemble of seed_ensemble.py, and the band holding 90 % of the
seeds (5th to 95th percentile, year by year or month by month). The seeds differ only in the random draws of the model
(forced outages and other events), so the band is the model's own stochastic spread, not parameter uncertainty and not
a confidence interval for the record. The legend box gives the median and 5-95 % range of the NSE over the seeds, from
seed_statistics.csv. These replace the +/-1 and +/-2 RMSE bands of make_figures() in validation_statistics.py, which
drew a band of constant width around one run.

Usage: python seed_band_figures.py ENSEMBLE_DIR [OUT_DIR] [--runs RUNS_DIR]
  ENSEMBLE_DIR   seed_years.csv and seed_statistics.csv of seed_ensemble.py (e.g. ensemble_2026-11-19)
  OUT_DIR        where the PNG and PDF figures go (default ENSEMBLE_DIR/figures)
  --runs         the seed_ensemble.py OUT_DIR holding run_s<seed>/; needed once, to write ENSEMBLE_DIR/seed_months.csv
                 (simulated monthly wholesale price, one column per seed), which the monthly price panel reads
"""
import argparse, glob, os, re, sys
import numpy as np, pandas as pd

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import validation_statistics as vs  # noqa: E402

SIM = "#2a78d6"        # simulated: categorical slot 1 (blue)
SIM_BAND = "#9ec5f4"   # blue ramp step 200
RECORD = "#2b2b2b"     # record: dark ink, so it never competes with the simulated colour
GRID = "#e6e6e6"


def build_seed_months(runs, out):
    cols = {}
    for d in sorted(glob.glob(os.path.join(runs, "run_s*"))):
        seed = int(re.search(r"run_s(\d+)$", d).group(1))
        f = glob.glob(os.path.join(d, "csv", "VIC", "VICSimDataMonthlySummary_seed_*.csv"))[0]
        m = pd.read_csv(f, index_col="Time (Month)", usecols=["Time (Month)", "Primary Wholesale ($/MWh)"])
        m.index = pd.to_datetime(m.index)
        cols[seed] = m["Primary Wholesale ($/MWh)"].loc[vs.PRICE_FIRST:vs.PRICE_LAST]
    df = pd.DataFrame(cols).sort_index(axis=1)
    df.index.name = "month"
    df.to_csv(out)
    print(f"{out}: {df.shape[1]} seeds x {df.shape[0]} months")


def spread(wide):
    """Median, 5th and 95th percentile across the seed columns of a (time x seed) table."""
    return wide.median(axis=1), wide.quantile(0.05, axis=1), wide.quantile(0.95, axis=1)


def nse_label(stats, indicator):
    g = stats[stats.indicator == indicator].NSE
    return f"NSE {g.median():.2f} ({g.quantile(0.05):.2f} to {g.quantile(0.95):.2f})"


def main():
    p = argparse.ArgumentParser()
    p.add_argument("ensemble")
    p.add_argument("out", nargs="?")
    p.add_argument("--runs")
    a = p.parse_args()
    out = a.out or os.path.join(a.ensemble, "figures")
    months_file = os.path.join(a.ensemble, "seed_months.csv")
    if a.runs:
        build_seed_months(a.runs, months_file)

    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    from matplotlib.patches import Patch
    from matplotlib.lines import Line2D
    plt.rcParams.update({"font.size": 10, "axes.edgecolor": "#888888", "axes.linewidth": 0.8,
                         "xtick.color": "#555555", "ytick.color": "#555555", "axes.labelcolor": "#2b2b2b",
                         "legend.frameon": False, "savefig.dpi": 300})
    os.makedirs(out, exist_ok=True)

    years = pd.read_csv(os.path.join(a.ensemble, "seed_years.csv"))
    stats = pd.read_csv(os.path.join(a.ensemble, "seed_statistics.csv"))
    n_seeds = years.seed.nunique()
    _, _, ghge, opennem, tariffs = vs.load_inputs()

    def wide(col):
        return years.pivot(index="year", columns="seed", values=col)

    def axes_style(ax, ylabel):
        ax.set_ylabel(ylabel)
        ax.grid(axis="y", color=GRID, lw=0.6)
        ax.set_axisbelow(True)
        for s in ("top", "right"):
            ax.spines[s].set_visible(False)

    def draw_sim(ax, x, w, step=False):
        med, lo, hi = spread(w)
        ax.fill_between(x, lo, hi, color=SIM_BAND, alpha=0.7, lw=0)
        ax.plot(x, med, color=SIM, lw=1.8)

    def handles(nse, records):
        h = [Line2D([], [], color=SIM, lw=1.8, label=f"Simulated, median of {n_seeds} seeds"),
             Patch(color=SIM_BAND, alpha=0.7, label="Simulated, 5-95 % of seeds")]
        return records + h + [Line2D([], [], lw=0, label=nse)]

    def save(fig, name):
        for ext in ("png", "pdf"):
            fig.savefig(os.path.join(out, f"{name}.{ext}"), bbox_inches="tight")
        plt.close(fig)
        print(os.path.join(out, name + ".png"))

    rec = lambda label, ls="-", m="o": Line2D([], [], color=RECORD, ls=ls, marker=m, ms=4, lw=1.2, label=label)

    # Figure 2: emissions. 1999-2018: the 1998 spin-up year has no simulated emissions; the 2018 record is the 2017
    # inventory less 11.8 Mt for Hazelwood, not a measurement (README), so it is drawn hollow.
    fig, ax = plt.subplots(figsize=(7, 4.2))
    w = wide("ghge_mt").loc[1999:2019]
    draw_sim(ax, w.index, w)
    obs = ghge["Hist_GHGE_MtCO2e"].loc[1999:2018]
    ax.plot(obs.index[:-1], obs.values[:-1], color=RECORD, marker="o", ms=4, lw=1.2)
    ax.plot(obs.index[-2:], obs.values[-2:], color=RECORD, ls=":", lw=1.2)
    ax.plot([2018], [obs.loc[2018]], marker="o", ms=5, mfc="white", mec=RECORD, lw=0)
    axes_style(ax, "Emissions (Mt CO$_2$-e per year)")
    ax.set_xticks(range(1999, 2020, 2))
    ax.legend(handles=handles(nse_label(stats, "GHGE (annual, full)"),
                              [rec("Record (Victorian inventory)"),
                               Line2D([], [], color=RECORD, marker="o", mfc="white", lw=0, ms=5,
                                      label="Record 2018: 2017 less Hazelwood")]),
              loc="lower left", fontsize=8.5)
    save(fig, "ghgeSeedValidation")

    # Figure 3: renewable share of Victorian generation, 2005-2020; both sides over the record's months (2005:
    # April-December, 2020: January-June), as in the statistics
    fig, ax = plt.subplots(figsize=(7, 4.2))
    w = wide("re_pct_record_months").loc[2005:2020]
    draw_sim(ax, w.index, w)
    obs = vs.renewable_share(opennem)
    ax.plot(obs.index.year, obs.values, color=RECORD, marker="o", ms=4, lw=1.2)
    axes_style(ax, "Renewable share of generation (%)")
    ax.set_title("2005: April to December; 2020: January to June (both sides)", fontsize=9, loc="left", color="#555555")
    ax.set_xticks(range(2005, 2021, 2))
    ax.legend(handles=handles(nse_label(stats, "RE share (annual)"), [rec("Record (OpenNEM)")]),
              loc="upper left", fontsize=8.5)
    save(fig, "reSeedValidation")

    # Figure 4: residential tariff; the two sources apart, each on its own years (no composite; see README)
    fig, ax = plt.subplots(figsize=(7, 4.2))
    w = wide("tariff").loc[1999:2019]
    draw_sim(ax, w.index, w)
    og, sv = tariffs.iloc[:, 1].dropna(), tariffs.iloc[:, 0].dropna()
    for s, ls, m in ((og, "-", "o"), (sv, "--", "s")):
        # break the line across the years a source does not cover (Oakley Greenwood has none for 2003-2006)
        s = s.reindex(range(s.index.min(), s.index.max() + 1))
        ax.plot(s.index, s.values, color=RECORD, ls=ls, marker=m, ms=4, lw=1.2)
    axes_style(ax, "Residential tariff (c/kWh, nominal)")
    ax.set_xticks(range(1999, 2020, 2))
    ax.set_ylim(bottom=0)
    g = stats[stats.indicator == "Tariffs vs St Vincent de Paul"].Bias
    ax.legend(handles=handles(nse_label(stats, "Tariffs vs Oakley Greenwood") + " vs Oakley Greenwood",
                              [rec("Record, Oakley Greenwood"), rec("Record, St Vincent de Paul", "--", "s")])
              + [Line2D([], [], lw=0, label=f"Bias {g.median():+.1f} c/kWh vs St Vincent de Paul (2 years)")],
              loc="lower right", fontsize=8.5)
    save(fig, "tariffsSeedValidation")

    # Figure 5: wholesale price, monthly (top) and annual (bottom; 2020 = January-June on both sides)
    price = vs.load_price_comparator()
    fig, (top, bot) = plt.subplots(2, 1, figsize=(9, 7), gridspec_kw={"height_ratios": [1.2, 1]})
    if os.path.exists(months_file):
        m = pd.read_csv(months_file, index_col="month", parse_dates=["month"])
        draw_sim(top, m.index, m)
        top.legend(handles=handles(nse_label(stats, "Wholesale price (monthly)"), [rec("Record (AEMO VIC1)", m="")]),
                   loc="upper left", fontsize=8.5)
    else:
        print(f"no {months_file}: monthly panel shows the record only (run once with --runs)")
    top.plot(price.index, price.values, color=RECORD, lw=1.0)
    axes_style(top, "Monthly price ($/MWh)")
    top.set_title("Monthly, January 1999 to June 2020", fontsize=10, loc="left")
    w = wide("price").loc[1999:2020]
    draw_sim(bot, w.index, w)
    obs = price.resample("YE").mean()
    bot.plot(obs.index.year, obs.values, color=RECORD, marker="o", ms=4, lw=1.2)
    axes_style(bot, "Annual price ($/MWh)")
    bot.set_xticks(range(1999, 2021, 2))
    bot.set_title("Annual mean of the months (2020: January to June)", fontsize=10, loc="left")
    bot.legend(handles=handles(nse_label(stats, "Wholesale price (annual)"), [rec("Record (AEMO VIC1)")]),
               loc="upper left", fontsize=8.5)
    fig.tight_layout()
    save(fig, "wholesaleSeedValidation")


if __name__ == "__main__":
    main()
