"""Reproduce the GR4SP simulation-mode validation statistics and figures.

Recomputes the Section 5 validation of the EMS article from the pinned BAU run
and the historical comparators. The point statistics need only the CSVs in
``experiments/simulationData``. The ensemble diagnostics additionally need the
validation ensemble archive, which is not in the repository because of its size.

Usage
-----
    python validation_statistics.py                    # table only
    python validation_statistics.py --figures OUTDIR   # table + RMSE-band figures
    python validation_statistics.py --ensemble PATH    # add ensemble diagnostics
    python validation_statistics.py --run RUN_DIR      # a run folder instead of the pinned BAU
                                                       # (RUN_DIR/csv/VIC/VICSim*Summary_seed_*.csv)

Provenance
----------
Mirrors ``experiments/notebookGr4sp/legacy/ema_gr4sp_EET-3RegimeValidation.ipynb``,
which is the authoritative source for the published numbers.

Wholesale price comparator (request R2, Nov 2026): AEMO's aggregated price and
demand data for VIC1, the monthly demand-weighted price sum(RRP x TOTALDEMAND) /
sum(TOTALDEMAND), each half-hour in the month it starts, January 1999 to June
2020 (``aemo_VIC1_monthly_vwp_199812_202006.csv``, built by
``scripts/data/aemo_vic1_monthly_price.py`` from the raw AEMO files). It replaces
the OpenNEM extract (April 2005 on), which agrees with it over 2005-2020
(correlation 0.999). The annual row resamples both sides from the same months;
``wholesale_annual_windows()`` prints it next to the simulated annual output
column, so the two routes can be compared.

Two ensembles exist and are not interchangeable:
  * validation bands: gr4sp_SOBOLhypopast2021-Mar-03_includ_wholesale_month.tar.gz
    105,000 runs, 24 uncertainties, Saltelli N=2100
  * sensitivity analysis: gr4sp_SOBOL2021-Feb-03.tar.gz
    126,000 runs, 29 uncertainties, Saltelli N=2100
"""
import argparse
import os
import tarfile

import numpy as np
import pandas as pd

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, os.pardir, "simulationData")

# Rows 41357 and 41379 are empty in the validation ensemble. The source notebook
# drops both (cell 13); keeping that behaviour is what makes the numbers match.
EMPTY_ENSEMBLE_ROWS = [41357, 41379]

RENEWABLE = ["Solar (Rooftop) - GWh", "Solar (Utility) - GWh",
             "Hydro - GWh", "Wind - GWh"]
GENERATION = RENEWABLE + ["Battery (Discharging) - GWh", "Gas (OCGT) - GWh",
                          "Gas (Steam) - GWh", "Brown Coal - GWh"]

# Column order of 2001to2019_historicTariffs.csv, whose headers are too long to
# tabulate. The second column is the Oakley Greenwood cost-stack analysis,
# commissioned for the Victorian distribution businesses' submission to the ACCC's
# retail pricing inquiry. It is not an ACCC publication; see README.md.
TARIFF_SOURCES = ("St Vincent de Paul", "Oakley Greenwood")


PRICE_FIRST, PRICE_LAST = "1999-01-01", "2020-06-01"


def load_inputs(run=None):
    """Return the model outputs (pinned BAU, or the run folder `run`) and four comparators."""
    if run is None:
        year_file = os.path.join(DATA, "VICSimDataYearSummary_bau19982051.csv")
        month_file = os.path.join(DATA, "VICSimDataMonthlySummary_bau19982051.csv")
    else:
        import glob
        year_file = glob.glob(os.path.join(run, "csv", "VIC", "VICSimDataYearSummary_seed_*.csv"))[0]
        month_file = glob.glob(os.path.join(run, "csv", "VIC", "VICSimDataMonthlySummary_seed_*.csv"))[0]
    year = pd.read_csv(year_file, index_col="Time (Year)")
    month = pd.read_csv(month_file, index_col="Time (Month)")
    month.index = pd.to_datetime(month.index)
    ghge = pd.read_csv(os.path.join(DATA, "19902018_historic_emissions_Vic.csv"),
                       index_col="Time (Year)")
    opennem = pd.read_csv(os.path.join(DATA, "2005_2020_OpenNemDataV1.csv"),
                          index_col="Time (Month)")
    opennem.index = pd.to_datetime(opennem.index)
    tariffs = pd.read_csv(os.path.join(DATA, "2001to2019_historicTariffs.csv"),
                          index_col="Time (Year)")
    return year, month, ghge, opennem, tariffs


def load_price_comparator():
    """AEMO VIC1 monthly demand-weighted price, January 1999 to June 2020."""
    aemo = pd.read_csv(os.path.join(DATA, "aemo_VIC1_monthly_vwp_199812_202006.csv"),
                       index_col="m", parse_dates=["m"])
    return aemo.loc[PRICE_FIRST:PRICE_LAST, "vwp"].rename("observed")


def monthly_prices(month):
    """Simulated and observed monthly price on the months both cover."""
    return pd.merge(month["Primary Wholesale ($/MWh)"].rename("simulated"),
                    load_price_comparator(), left_index=True, right_index=True)


def renewable_share(opennem):
    """Historical renewable share of local generation, annual, in per cent.

    Imports and exports are excluded, so this is a share of Victorian generation
    rather than of supply.
    """
    annual = {column: opennem[column].resample("YS").mean() for column in GENERATION}
    return sum(annual[column] for column in RENEWABLE) / sum(annual.values()) * 100.0


def tariff_years(tariffs, column):
    """The years one tariff source actually covers, as a compact label."""
    years = tariffs.iloc[:, column].dropna().index.tolist()
    runs, start = [], years[0]
    for previous, current in zip(years, years[1:] + [None]):
        if current != previous + 1:
            runs.append(str(start) if start == previous else f"{start}-{previous}")
            start = current
    return years, ", ".join(runs)


def tariff_indicator(year, tariffs, column):
    """Simulated and observed tariffs over one source's own coverage.

    Each source is compared on its own years rather than against the row-wise
    mean of the two. The mean changes composition across the window - one source
    to 2009, both to 2017, the other after - and because the sources sit about
    15 c/kWh apart that composition alone steps the composite, which shows up in
    the bias and the NSE as if it were model error.
    """
    years, label = tariff_years(tariffs, column)
    simulated = year["Avg Tariff (c/KWh) per household"].loc[years]
    return simulated.values, tariffs.iloc[:, column].loc[years].values, label, "c/kWh"


def series(year, month, ghge, opennem, tariffs):
    """Build the (simulated, observed, period, unit) tuple for each indicator."""
    # System scope 1 emissions (each generator's output x its emission factor) since solar step 2 (14 Nov 2026).
    # Earlier runs: emissions per household x households / 0.3, which holds only while the residential share is 0.3
    if "System GHG Emissions (tCO2-e)" in year.columns:
        ghge_total = year["System GHG Emissions (tCO2-e)"] / 1e6
    else:
        ghge_total = (year["GHG Emissions (tCO2-e) per household"]
                      * year["Number of Domestic Consumers (households)"] / 1e6 / 0.3)
    ghge_observed = ghge[ghge.index > 1997]["Hist_GHGE_MtCO2e"].values
    monthly = monthly_prices(month)
    return {
        "Wholesale price (monthly)": (
            monthly.simulated.values, monthly.observed.values,
            "1999-01 to 2020-06", "$/MWh"),
        # Both sides are resampled from `monthly`, so each annual figure averages
        # exactly the same months (2020: January to June). wholesale_annual_windows()
        # compares this with the simulated annual output column.
        "Wholesale price (annual)": (
            monthly.simulated.resample("YE").mean().values,
            monthly.observed.resample("YE").mean().values,
            "1999-2020", "$/MWh"),
        "RE share (annual)": (
            year["Percentage Renewable Production"].iloc[7:23].values * 100.0,
            renewable_share(opennem).values,
            "2005-2020", "pp"),
        "Tariffs vs Oakley Greenwood": tariff_indicator(year, tariffs, 1),
        "Tariffs vs St Vincent de Paul": tariff_indicator(year, tariffs, 0),
        "GHGE (annual)": (
            ghge_total.iloc[7:21].values,
            ghge_observed[7:],
            "2005-2018", "Mt"),
        "GHGE (annual, full)": (
            ghge_total.iloc[:-32].values,
            ghge_observed,
            "1998-2018", "Mt"),
    }


def statistics(simulated, observed):
    """Point statistics for one indicator, ignoring gaps in the comparator."""
    simulated = np.asarray(simulated, float)
    observed = np.asarray(observed, float)
    keep = ~(np.isnan(simulated) | np.isnan(observed))
    simulated, observed = simulated[keep], observed[keep]
    error = simulated - observed
    return {
        "n": len(error),
        "MAE": np.abs(error).mean(),
        "RMSE": np.sqrt((error ** 2).mean()),
        "Bias": error.mean(),
        "NRMSE": 100.0 * np.sqrt((error ** 2).mean()) / observed.mean(),
        "NSE": 1.0 - (error ** 2).sum() / ((observed - observed.mean()) ** 2).sum(),
        "r": np.corrcoef(simulated, observed)[0, 1],
    }


def print_table(indicators):
    """Print the Section 5 summary table."""
    header = (f"{'Indicator':30s}{'Period':22s}{'n':>5}{'Units':>8}"
              f"{'MAE':>9}{'RMSE':>9}{'Bias':>9}{'NRMSE':>9}{'NSE':>9}")
    print(header)
    print("-" * len(header))
    for name, (simulated, observed, period, unit) in indicators.items():
        result = statistics(simulated, observed)
        print(f"{name:30s}{period:22s}{result['n']:>5}{unit:>8}"
              f"{result['MAE']:>9.2f}{result['RMSE']:>9.2f}{result['Bias']:>+9.2f}"
              f"{result['NRMSE']:>8.1f}%{result['NSE']:>9.2f}")


def wholesale_annual_windows(year, month):
    """The annual wholesale row by two routes, and the monthly RMSE-band coverage.

    Tabled: both sides resampled from the monthly series over the same months.
    Alternative: the simulated annual output column, which covers whole calendar
    years, against the same observed annual means (differs only in 2020, where the
    comparator ends in June).
    """
    monthly = monthly_prices(month)
    observed = monthly.observed.resample("YE").mean()
    resampled = monthly.simulated.resample("YE").mean()
    output = pd.Series(year["Primary Wholesale ($/MWh)"].reindex(observed.index.year).values,
                       index=observed.index)
    months = monthly.simulated.resample("YE").count()

    print("\nWholesale price, annual: monthly series resampled vs annual output column")
    print("-" * 68)
    print(f"  {'year':>6}{'months':>8}{'observed':>11}{'resampled':>11}"
          f"{'annual col':>11}{'diff':>8}")
    for stamp in observed.index:
        n = int(months.loc[stamp])
        flag = "" if n == 12 else "  <- partial"
        print(f"  {stamp.year:>6}{n:>8}{observed.loc[stamp]:>11.2f}"
              f"{resampled.loc[stamp]:>11.2f}{output.loc[stamp]:>11.2f}"
              f"{output.loc[stamp] - resampled.loc[stamp]:>+8.2f}{flag}")
    print()
    for label, simulated in (("monthly series resampled (tabled)", resampled),
                             ("annual output column", output)):
        result = statistics(simulated.values, observed.values)
        print(f"    {label:<36}MAE {result['MAE']:6.2f}  RMSE {result['RMSE']:6.2f}"
              f"  Bias {result['Bias']:+6.2f}  NRMSE {result['NRMSE']:5.1f}%  NSE {result['NSE']:5.2f}")

    # Cross-check: the simulated monthly price is a time-weighted mean of half-hours, so also compare it with the
    # observed time-weighted mean (column `mean`), which carries no price-demand covariance (README, Resolution)
    aemo = pd.read_csv(os.path.join(DATA, "aemo_VIC1_monthly_vwp_199812_202006.csv"),
                       index_col="m", parse_dates=["m"]).loc[monthly.index, "mean"]
    for label, obs in (("demand weighted (tabled)", monthly.observed), ("time weighted (cross-check)", aemo)):
        r = statistics(monthly.simulated.values, obs.values)
        print(f"  Monthly vs AEMO {label:<28}MAE {r['MAE']:6.2f}  RMSE {r['RMSE']:6.2f}"
              f"  Bias {r['Bias']:+6.2f}  NSE {r['NSE']:5.2f} (not comparable across rows)")

    rmse = statistics(monthly.simulated.values, monthly.observed.values)["RMSE"]
    error = (monthly.observed - monthly.simulated).abs()
    print(f"\n  Monthly price: observed within 1 RMSE of the simulation in "
          f"{100 * (error <= rmse).mean():.0f}% of months, within 2 RMSE in "
          f"{100 * (error <= 2 * rmse).mean():.0f}%")


def tariff_comparators(year, tariffs):
    """Cross-checks behind the two tariff rows, and the cost of the composite.

    The composite is no longer tabled. It is kept here so the size of the
    artefact it introduced stays on the record, next to the disagreement between
    the sources that produced it.
    """
    simulated = year["Avg Tariff (c/KWh) per household"]
    both = tariffs.dropna()
    union = tariffs.dropna(how="all").index

    print()
    print("Tariff comparators")
    print("-" * 68)
    for column, name in enumerate(TARIFF_SOURCES):
        years, label = tariff_years(tariffs, column)
        print(f"  {name:<26s}n={len(years):<3d} {label}")
    print(f"  {'union of both sources':<26s}n={len(union):<3d} "
          f"{union.min()}-{union.max()}, no observation 2003-2006")

    print()
    print(f"  Overlap ({both.index.min()}-{both.index.max()}, n={len(both)})")
    gap = (both.iloc[:, 1] - both.iloc[:, 0]).values
    print(f"    {'mean, ' + TARIFF_SOURCES[0]:<38s}{both.iloc[:, 0].mean():8.2f}")
    print(f"    {'mean, ' + TARIFF_SOURCES[1]:<38s}{both.iloc[:, 1].mean():8.2f}")
    print(f"    {'mean gap between the sources':<38s}{gap.mean():8.2f}")
    print(f"    {'gap as a share of the row-wise mean':<38s}"
          f"{100.0 * gap.mean() / both.mean(axis=1).mean():7.1f}%")
    overlap = simulated.loc[both.index]
    for label, observed in ((TARIFF_SOURCES[0], both.iloc[:, 0]),
                            (TARIFF_SOURCES[1], both.iloc[:, 1]),
                            ("row-wise mean (retired)", both.mean(axis=1))):
        result = statistics(overlap.values, observed.values)
        print(f"    vs {label:<30s} RMSE{result['RMSE']:7.2f}"
              f"   Bias{result['Bias']:+7.2f}")

    print()
    print("  Correlation with the simulated series")
    for column, name in enumerate(TARIFF_SOURCES):
        sim, obs, label, _ = tariff_indicator(year, tariffs, column)
        print(f"    {name + ' (' + label + ')':<48s}"
              f"r ={statistics(sim, obs)['r']:6.2f}")
    composite = tariffs.mean(axis=1).loc[union]
    result = statistics(simulated.loc[union].values, composite.values)
    print(f"    {'row-wise mean, retired (' + str(union.min()) + '-' + str(union.max()) + ')':<48s}"
          f"r ={result['r']:6.2f}")
    print(f"    {'-- as tabled before: MAE ' + format(result['MAE'], '.2f')}"
          f", RMSE {result['RMSE']:.2f}, Bias {result['Bias']:+.2f}, "
          f"NRMSE {result['NRMSE']:.1f}%, NSE {result['NSE']:.2f}")


def read_ensemble(archive, member):
    """Read one outcome matrix from the ensemble tarball, dropping its empty rows."""
    with tarfile.open(archive, "r:gz") as tar:
        handle = tar.extractfile(member + ".csv")
        frame = pd.read_csv(handle, header=None)
    return np.delete(frame.values.astype(float), EMPTY_ENSEMBLE_ROWS, axis=0)


def ensemble_diagnostics(archive, indicators):
    """Show why the ensemble standard deviation is not a validation statistic.

    The bands published in earlier drafts were the spread of this ensemble, not
    the model's error. Printing sigma beside the spread of the observations
    themselves makes the difference visible.
    """
    simulated, observed, _, _ = indicators["GHGE (annual, full)"]
    ghge = read_ensemble(archive, "GHGYear")[:, 0:-32]
    consumers = read_ensemble(archive, "numConsumersYear")[:, 0:-32]
    ensemble = np.vstack([ghge * consumers / 1e6 / 0.3, simulated])
    sigma = np.median(ensemble.std(axis=0))
    spread = np.asarray(observed, float).std(ddof=1)
    print("\nEnsemble diagnostics (GHGE)")
    print("-" * 60)
    print(f"  median ensemble sigma        {sigma:8.2f} Mt")
    print(f"  sd of the observations       {spread:8.2f} Mt")
    print(f"  ratio                        {sigma / spread:8.1f}x")
    print("  The band is wider than the historical variation it is asked to")
    print("  contain, so its coverage is guaranteed rather than earned.")


def make_figures(indicators, year, opennem, month, tariffs, outdir):
    """Write the four validation figures with +/-1 and +/-2 RMSE bands."""
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    import matplotlib.patches as mpatches
    from matplotlib.ticker import FormatStrFormatter, MultipleLocator

    os.makedirs(outdir, exist_ok=True)
    inner = mpatches.Patch(color="darkgray", label="+/- 1 RMSE")
    outer = mpatches.Patch(color="lightgray", label="+/- 2 RMSE")

    def panel(x, simulated, observed, ylabel, filename, ylim=None,
              legend_loc="best", extra=None, legend_size=15):
        rmse = statistics(simulated, observed)["RMSE"]
        plt.figure(figsize=(10, 8), dpi=80, facecolor="w", edgecolor="k")
        plt.fill_between(x, simulated - rmse, simulated + rmse, color="gray", alpha=0.2)
        plt.fill_between(x, simulated - 2 * rmse, simulated + 2 * rmse,
                         color="darkgray", alpha=0.2)
        handles = []
        if extra is None:
            handles.append(plt.plot(x, observed, "g--", label="Historic")[0])
        else:
            for line in extra:
                plt.plot(x, line, "g--", lw=1.0, alpha=0.75)
            handles.append(plt.plot([], [], "g--", lw=1.0, alpha=0.75,
                                    label="Historic (Oakley Greenwood; St Vincent de Paul)")[0])
            handles.append(plt.plot(x, observed, "g-", lw=1.8,
                                    label="Historic (mean of the two series)")[0])
        handles.append(plt.plot(x, simulated, "r-", label="Simulated")[0])
        plt.legend(handles=handles + [inner, outer], loc=legend_loc,
                   prop={"size": legend_size})
        plt.ylabel(ylabel, size=legend_size)
        if ylim:
            plt.gca().set_ylim(ylim)
        axis = plt.gca()
        axis.xaxis.set_major_locator(MultipleLocator(2))
        axis.xaxis.set_major_formatter(FormatStrFormatter("%d"))
        plt.xticks(size=13)
        plt.yticks(size=13)
        plt.savefig(os.path.join(outdir, filename), bbox_inches="tight", dpi=150)
        plt.close()

    simulated, observed, _, _ = indicators["GHGE (annual, full)"]
    panel(np.arange(1998, 2019), simulated, observed, "$MtCO_2e$",
          "ghgeRMSEValidation.png", ylim=[35, 80])

    simulated, observed, _, _ = indicators["RE share (annual)"]
    panel(np.arange(2005, 2021), simulated, observed, "% RE",
          "reRMSEValidation.png", legend_loc="upper left")

    # The panel spans every year either source covers, so its band is still the
    # composite. The tabled statistics are per source; these two differ on purpose.
    simulated = year["Avg Tariff (c/KWh) per household"].loc[tariffs.index].values
    observed = tariffs.mean(axis=1).values
    panel(tariffs.index.values, simulated, observed, "$c/kWh$",
          "tariffsRMSEValidation.png", legend_loc="upper left", legend_size=14,
          extra=[tariffs.iloc[:, 0].values, tariffs.iloc[:, 1].values])

    simulated, observed, _, _ = indicators["Wholesale price (monthly)"]
    dates = monthly_prices(month).index
    rmse = statistics(simulated, observed)["RMSE"]
    plt.figure(figsize=(18, 10))
    plt.plot(dates, simulated, color="r", lw=2,
             label="Wholesale Primary Spot Market - Simulated")
    plt.plot(dates, observed, color="g", ls="--", lw=2,
             label="Wholesale Primary Spot Market - Historic")
    plt.fill_between(dates, simulated - rmse, simulated + rmse,
                     color="gray", alpha=0.2, label="+/- 1 RMSE")
    plt.fill_between(dates, simulated - 2 * rmse, simulated + 2 * rmse,
                     color="darkgray", alpha=0.2, label="+/- 2 RMSE")
    plt.xlabel("Months", size=18)
    plt.ylabel("$/MWh", size=18)
    plt.xticks(size=16)
    plt.yticks(size=16)
    plt.legend(prop={"size": 18}, loc="upper left")
    plt.savefig(os.path.join(outdir, "wholesaleMonthRMSEValidation.png"),
                bbox_inches="tight", dpi=110)
    plt.close()
    print(f"\nFigures written to {outdir}")


def main():
    parser = argparse.ArgumentParser(
        description="Reproduce the GR4SP validation statistics and figures.")
    parser.add_argument("--figures", metavar="OUTDIR",
                        help="write the four RMSE-band figures to this directory")
    parser.add_argument("--ensemble", metavar="PATH",
                        help="validation ensemble archive, for band diagnostics")
    parser.add_argument("--run", metavar="RUN_DIR",
                        help="model run folder to validate instead of the pinned BAU")
    args = parser.parse_args()

    year, month, ghge, opennem, tariffs = load_inputs(args.run)
    indicators = series(year, month, ghge, opennem, tariffs)
    print_table(indicators)
    wholesale_annual_windows(year, month)
    tariff_comparators(year, tariffs)

    if args.ensemble:
        ensemble_diagnostics(args.ensemble, indicators)
    if args.figures:
        make_figures(indicators, year, opennem, month, tariffs, args.figures)


if __name__ == "__main__":
    main()
