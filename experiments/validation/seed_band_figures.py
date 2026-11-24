"""Validation figures with the spread over random seeds (request R6, methods article Figures 2-5).

Each figure shows the record, the median over the seed ensemble of seed_ensemble.py, and the band holding 90 % of the
seeds (5th to 95th percentile, year by year or month by month). The seeds differ only in the random draws of the model
(forced outages and other events), so the band is the model's own stochastic spread, not parameter uncertainty and not
a confidence interval for the record. The legend box gives the median and 5-95 % range of the NSE over the seeds, from
seed_statistics.csv. These replace the +/-1 and +/-2 RMSE bands of make_figures() in validation_statistics.py, which
drew a band of constant width around one run.

Emissions (Figure 2) use the inventory's July-June year, from ENSEMBLE_DIR/emissions_year_basis.csv (emissions_year_basis.py).

Usage: python seed_band_figures.py ENSEMBLE_DIR [OUT_DIR] [--runs RUNS_DIR]
  ENSEMBLE_DIR   seed_years.csv and seed_statistics.csv of seed_ensemble.py (e.g. ensemble_2026-11-20)
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

    # Figure 2: emissions on the inventory's own year, July to June ("2017" = July 2016 to June 2017; 23 Nov 2026,
    # emissions_year_basis.py). 2000 is the first whole July-June year after the 1998 spin-up; NSE over 2000-2018.
    fig, ax = plt.subplots(figsize=(7, 4.2))
    fy = pd.read_csv(os.path.join(a.ensemble, "emissions_year_basis.csv"))
    w = fy.pivot(index="year", columns="seed", values="july_june").loc[2000:2019]
    draw_sim(ax, w.index, w)
    obs = ghge["Hist_GHGE_MtCO2e"].loc[2000:2018]
    ax.plot(obs.index, obs.values, color=RECORD, marker="o", ms=4, lw=1.2)
    axes_style(ax, "Emissions (Mt CO$_2$-e per year to 30 June)")
    ax.set_xlabel("Year ending 30 June")
    ax.set_xticks(range(2000, 2020, 2))
    nse = pd.Series([vs.statistics(w[s].loc[2000:2018], obs)["NSE"] for s in w.columns])
    ax.legend(handles=handles(f"NSE {nse.median():.2f} ({nse.quantile(0.05):.2f} to {nse.quantile(0.95):.2f}), 2000-2018",
                              [rec("Record (Victorian inventory, electricity generation)")]),
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
    axes_style(ax, "Residential tariff (c/kWh, real January 2019 dollars)")
    ax.set_xticks(range(1999, 2020, 2))
    ax.set_ylim(bottom=0)
    g = stats[stats.indicator == "Tariffs vs St Vincent de Paul"].Bias
    ax.legend(handles=handles(nse_label(stats, "Tariffs vs Oakley Greenwood") + " vs Oakley Greenwood",
                              [rec("Record, Oakley Greenwood"), rec("Record, St Vincent de Paul", "--", "s")])
              + [Line2D([], [], lw=0, label=f"NSE {stats[stats.indicator == 'Tariffs vs St Vincent de Paul'].NSE.median():.0f}, bias {g.median():+.1f} c/kWh vs St Vincent de Paul")],
              loc="lower right", fontsize=8.5)
    save(fig, "tariffsSeedValidation")

    # Figure 5: wholesale price, monthly (top) and annual (bottom; 2020 = January-June on both sides)
    price = vs.load_price_comparator()
    fig, (top, bot) = plt.subplots(2, 1, figsize=(9, 7), gridspec_kw={"height_ratios": [1.2, 1]})
    if os.path.exists(months_file):
        m = pd.read_csv(months_file, index_col="month", parse_dates=["month"])
        draw_sim(top, m.index, m)
        top.legend(handles=handles(nse_label(stats, "Wholesale price (monthly)"), [rec("Record (AEMO VIC1, time-weighted)", m="")]),
                   loc="upper left", fontsize=8.5)
    else:
        print(f"no {months_file}: monthly panel shows the record only (run once with --runs)")
    top.plot(price.index, price.values, color=RECORD, lw=1.0)
    axes_style(top, "Monthly price (nominal $/MWh)")
    top.set_title("Monthly, January 1999 to June 2020", fontsize=10, loc="left")
    w = wide("price").loc[1999:2020]
    draw_sim(bot, w.index, w)
    obs = price.resample("YE").mean()
    bot.plot(obs.index.year, obs.values, color=RECORD, marker="o", ms=4, lw=1.2)
    axes_style(bot, "Annual price (nominal $/MWh)")
    bot.set_xticks(range(1999, 2021, 2))
    bot.set_title("Annual mean of the months (2020: January to June)", fontsize=10, loc="left")
    bot.legend(handles=handles(nse_label(stats, "Wholesale price (annual)"), [rec("Record (AEMO VIC1, time-weighted)")]),
               loc="upper left", fontsize=8.5)
    fig.tight_layout()
    save(fig, "wholesaleSeedValidation")

    # Figure 6: monthly generation by fuel, April 2005 to June 2020 (left) and the mean by calendar month (right).
    # Hydro and rooftop PV are largely inputs (recorded monthly hydro energy, CER installations): labelled as such.
    fm_file = os.path.join(a.ensemble, "seed_fuel_months.csv")
    if os.path.exists(fm_file):
        fm = pd.read_csv(fm_file, parse_dates=["month"])
        rows = [("Coal generation (monthly)", "Brown coal", ["Brown Coal - GWh"]),
                ("Gas generation (monthly)", "Gas", ["Gas (OCGT) - GWh", "Gas (Steam) - GWh"]),
                ("Wind generation (monthly)", "Wind", ["Wind - GWh"]),
                ("Hydro generation (monthly, input)", "Hydro (recorded monthly energy, an input)", ["Hydro - GWh"]),
                ("Rooftop PV generation (monthly, input)", "Rooftop PV (recorded installations)",
                 ["Solar (Rooftop) - GWh"])]
        fig, axs = plt.subplots(len(rows), 2, figsize=(10, 2.3 * len(rows)),
                                gridspec_kw={"width_ratios": [3, 1]})
        months = sorted(fm.month.unique())
        for i, (ind, title, rec_cols) in enumerate(rows):
            w = fm.pivot(index="month", columns="seed", values=ind)
            obs = opennem.loc[w.index, rec_cols].sum(axis=1)
            ax = axs[i, 0]
            draw_sim(ax, w.index, w)
            ax.plot(obs.index, obs.values, color=RECORD, lw=1.0)
            axes_style(ax, "GWh per month")
            ax.set_title(f"{title}: {nse_label(stats, ind)}", fontsize=9.5, loc="left")
            # mean by calendar month, per seed then spread over the seeds
            days = w.index.days_in_month.values
            cw = w.div(days, axis=0).groupby(w.index.month).mean()   # GWh per day: February is not a dip
            co = (obs / days).groupby(obs.index.month).mean()
            ax = axs[i, 1]
            draw_sim(ax, cw.index, cw)
            ax.plot(co.index, co.values, color=RECORD, marker="o", ms=3, lw=1.0)
            axes_style(ax, "GWh per day")
            ax.set_xticks([1, 4, 7, 10])
            ax.set_xticklabels(["Jan", "Apr", "Jul", "Oct"])
            if i == 0:
                ax.set_title("Mean by calendar month", fontsize=9.5, loc="left")
        axs[2, 0].annotate("Record: no wind before 2009 in this extract\n(five early farms, 134 MW, missing; see README)",
                           xy=(pd.Timestamp("2005-06-01"), 420), fontsize=7.5, color="#555555")
        fig.tight_layout(rect=(0, 0, 1, 0.97))
        fig.legend(handles=[rec("Record (OpenNEM)", m=""),
                            Line2D([], [], color=SIM, lw=1.8, label=f"Simulated, median of {n_seeds} seeds"),
                            Patch(color=SIM_BAND, alpha=0.7, label="Simulated, 5-95 % of seeds")],
                   loc="upper center", fontsize=8.5, ncol=3, bbox_to_anchor=(0.5, 1.0))
        save(fig, "fuelSeedValidation")

    # Figure 7: price duration curves, every half-hour of the period ranked from the highest price to the lowest
    pdc_file = os.path.join(a.ensemble, "seed_price_duration.csv")
    if os.path.exists(pdc_file):
        pdc = pd.read_csv(pdc_file)
        rec_pdc = pd.read_csv(os.path.join(a.ensemble, "record_price_duration.csv"))
        periods = ["1999-2008", "2009-2016", "2017-2019"]
        # Linear axis to 300 $/MWh, where nearly all half-hours lie; the spike tail above it is given as a share
        # (seed_halfhour_years.csv for the model; the record curve's own exceedance at 300)
        hh = pd.read_csv(os.path.join(a.ensemble, "seed_halfhour_years.csv"))
        fig, axs = plt.subplots(1, len(periods), figsize=(11, 3.9), sharey=True)
        for ax, per in zip(axs, periods):
            w = pdc[pdc.period == per].pivot(index="exceedance_pct", columns="seed", values="price")
            r = rec_pdc[rec_pdc.period == per].set_index("exceedance_pct").price
            draw_sim(ax, w.index, w)
            ax.plot(r.index, r.values, color=RECORD, lw=1.2)
            ax.set_ylim(-20, 300)
            ax.set_xlim(0, 100)
            y0, y1 = (int(v) for v in per.split("-"))
            spikes = hh[(hh.year >= y0) & (hh.year <= y1)].groupby("seed").share_above_300.mean().median() * 100
            rec_spikes = np.interp(300, r.values[::-1], r.index.values[::-1])
            ax.text(98, 285, f"Above 300 $/MWh: record {rec_spikes:.2f} %,\nmodel {spikes:.2f} % of half-hours",
                    ha="right", va="top", fontsize=8, color="#555555")
            axes_style(ax, "Price (nominal $/MWh)" if per == periods[0] else "")
            ax.set_xlabel("Share of half-hours at or above the price (%)")
            ax.set_title(per, fontsize=10, loc="left")
        axs[0].legend(handles=[rec("Record (AEMO VIC1)", m=""),
                               Line2D([], [], color=SIM, lw=1.8, label=f"Simulated, median of {n_seeds} seeds"),
                               Patch(color=SIM_BAND, alpha=0.7, label="Simulated, 5-95 % of seeds")],
                      loc="center right", fontsize=8)
        fig.tight_layout()
        save(fig, "priceDurationSeedValidation")


if __name__ == "__main__":
    main()
