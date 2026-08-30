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

Provenance
----------
Mirrors ``experiments/notebookGr4sp/legacy/ema_gr4sp_EET-3RegimeValidation.ipynb``,
which is the authoritative source for the published numbers. The other notebook,
``SimulationValidation.ipynb``, derives annual wholesale by resampling the monthly
series rather than reading the annual output column, and does not reproduce them.

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


def load_inputs():
    """Return the pinned BAU outputs and the four historical comparators."""
    year = pd.read_csv(os.path.join(DATA, "VICSimDataYearSummary_bau19982051.csv"),
                       index_col="Time (Year)")
    month = pd.read_csv(os.path.join(DATA, "VICSimDataMonthlySummary_bau19982051.csv"),
                        index_col="Time (Month)")
    month.index = pd.to_datetime(month.index)
    ghge = pd.read_csv(os.path.join(DATA, "19902018_historic_emissions_Vic.csv"),
                       index_col="Time (Year)")
    opennem = pd.read_csv(os.path.join(DATA, "2005_2020_OpenNemDataV1.csv"),
                          index_col="Time (Month)")
    opennem.index = pd.to_datetime(opennem.index)
    tariffs = pd.read_csv(os.path.join(DATA, "2001to2019_historicTariffs.csv"),
                          index_col="Time (Year)")
    return year, month, ghge, opennem, tariffs


def renewable_share(opennem):
    """Historical renewable share of local generation, annual, in per cent.

    Imports and exports are excluded, so this is a share of Victorian generation
    rather than of supply.
    """
    annual = {column: opennem[column].resample("YS").mean() for column in GENERATION}
    return sum(annual[column] for column in RENEWABLE) / sum(annual.values()) * 100.0


def series(year, month, ghge, opennem, tariffs):
    """Build the (simulated, observed, period, unit) tuple for each indicator."""
    ghge_total = (year["GHG Emissions (tCO2-e) per household"]
                  * year["Number of Domestic Consumers (households)"] / 1e6 / 0.3)
    ghge_observed = ghge[ghge.index > 1997]["Hist_GHGE_MtCO2e"].values
    monthly = pd.merge(opennem["Volume Weighted Price (Historic) - $/MWh"],
                       month["Primary Wholesale ($/MWh)"],
                       left_index=True, right_index=True)
    return {
        "Wholesale price (monthly)": (
            monthly["Primary Wholesale ($/MWh)"].values,
            monthly["Volume Weighted Price (Historic) - $/MWh"].values,
            "2005-04 to 2020-06", "$/MWh"),
        "Wholesale price (annual)": (
            year["Primary Wholesale ($/MWh)"].iloc[7:23].values,
            opennem["Volume Weighted Price (Historic) - $/MWh"].resample("YE").mean().values,
            "2005-2020", "$/MWh"),
        "RE share (annual)": (
            year["Percentage Renewable Production"].iloc[7:23].values * 100.0,
            renewable_share(opennem).values,
            "2005-2020", "pp"),
        "Tariffs (annual)": (
            year["Avg Tariff (c/KWh) per household"].iloc[3:22].values,
            tariffs.mean(axis=1).values,
            "2001-2002, 2007-2019", "c/kWh"),
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
    }


def print_table(indicators):
    """Print the Section 5 summary table."""
    header = (f"{'Indicator':26s}{'Period':22s}{'n':>5}{'Units':>8}"
              f"{'MAE':>9}{'RMSE':>9}{'Bias':>9}{'NRMSE':>9}{'NSE':>9}")
    print(header)
    print("-" * len(header))
    for name, (simulated, observed, period, unit) in indicators.items():
        result = statistics(simulated, observed)
        print(f"{name:26s}{period:22s}{result['n']:>5}{unit:>8}"
              f"{result['MAE']:>9.2f}{result['RMSE']:>9.2f}{result['Bias']:>+9.2f}"
              f"{result['NRMSE']:>8.1f}%{result['NSE']:>9.2f}")


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


def make_figures(indicators, opennem, month, tariffs, outdir):
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
                                    label="Historic (ACCC; St Vincent de Paul)")[0])
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

    simulated, observed, _, _ = indicators["Tariffs (annual)"]
    panel(tariffs.index.values, simulated, observed, "$c/kWh$",
          "tariffsRMSEValidation.png", legend_loc="upper left", legend_size=14,
          extra=[tariffs.iloc[:, 0].values, tariffs.iloc[:, 1].values])

    simulated, observed, _, _ = indicators["Wholesale price (monthly)"]
    dates = pd.merge(opennem["Volume Weighted Price (Historic) - $/MWh"],
                     month["Primary Wholesale ($/MWh)"],
                     left_index=True, right_index=True).index
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
    args = parser.parse_args()

    year, month, ghge, opennem, tariffs = load_inputs()
    indicators = series(year, month, ghge, opennem, tariffs)
    print_table(indicators)

    if args.ensemble:
        ensemble_diagnostics(args.ensemble, indicators)
    if args.figures:
        make_figures(indicators, opennem, month, tariffs, args.figures)


if __name__ == "__main__":
    main()
