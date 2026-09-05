# Analysis notebooks

Scenario, sensitivity and energy-vulnerability analysis of the EMA-workbench runs.
`reproducibility/` records which notebook needs which result archive and which of
them reproduce from the published Zenodo deposit; `legacy/` holds superseded copies.

## Energy vulnerability (EV)

Two notebooks are active:

| Notebook | Result archive it loads | On Zenodo |
|---|---|---|
| `gr4sp_energy_vulnerability_IMAP.ipynb` | the scenario archive chosen in its config cell | yes |
| `gr4sp_energy_vulnerability_SOBOL_analysis.ipynb` | `gr4sp_SOBOL2021-Feb-03.tar.gz` (589 MB) | yes |

Every other input is committed to the repository — the IMAP typologies workbook,
the ABS census cross-tabulation, dwelling types, gas prices, the CPI conversion and
the household forecast — so the archive is the only download. Fetch it with
`python ../simulationData/fetch_results.py <name>`.

`gr4sp_energy_vulnerability_IMAP.ipynb` replaces the former per-scenario copies. Its
configuration cell selects the scenario and nothing below it is scenario-specific:

```python
SCENARIO = "ST"          # "JT" | "LCT" | "ST"
onlyOneScenario = False  # True -> load the single BAU run instead (fast)
ANALYSIS_ONLY = False    # True -> reuse the caches from a prior full run
```

Caches are written per scenario as `outputs/data/<SCENARIO>_*.pkl.bz2`, so scenarios
can be run back to back. They are large (of order 100 MB each) and `.gitignore`d;
`ANALYSIS_ONLY = True` reuses them and asserts, with the scenario named, if they are
absent. `set_scenario.py` and `set_flag.py` set the same switches for headless runs.

### What the EV indicator rests on, and the years for which that is measured

Energy vulnerability is driven by the retail tariff, and the wholesale share of that
tariff, `R_w`, comes from the database table `historic_tariff_contribution`. The
model reads it at **`year - 1`** and falls back to a YAML constant when the year is
absent (`EndUserUnit.java:146-158`). Three consequences bound what the indicator can
be said to measure, and none is visible in any output:

1. **From 2022 onward `R_w` is a constant.** The register ends at 2020, so with the
   one-year lag every simulated year from 2022 to the end of a run takes
   `wholesaleContribution.usage` from `VICfuture.yaml:167`, which is **0.2837** — the
   midpoint of the register's own minimum (0.1137, 2015) and maximum (0.4537, 2007).
   Across the whole projection horizon the wholesale share of the retail tariff
   therefore does not vary at all, in any scenario. Scenario differences in EV after
   2021 come from the wholesale *price* and from consumption, never from the share.
2. **Not every register row is a measurement.** Only 1999-2002 and 2006-2016 are
   genuine. 1999-2001 repeat the 1999 row verbatim, 2003-2005 are a constructed ramp,
   and 2017-2020 have no recoverable source — 2017 and 2018 do not even satisfy the
   identity that the seven shares sum to one, missing it by -5.10 and +9.21
   percentage points. The evidence is in
   [`../validation/README.md`](../validation/README.md), *Provenance of the tariff
   data*.
3. **The lag moves those rows onto the following simulated year.** Simulated tariffs
   for 2001 and 2002 are computed from the back-filled 1999 value, and those for
   2018-2021 from the constructed rows.

**So the wholesale share underlying EV is measured data for roughly 2007-2017,
constructed or repeated outside it, and constant from 2022.** That is a statement
about the historical record rather than about the model: the private-regime retail
cost stack is not published in a form that closes those gaps. It should be stated
wherever EV results are reported, particularly for projections.

A separate structural gap affects the same period: rooftop solar installations are
historical to 2019 and forecast from 2021, so **no rooftop capacity is added in
2020** (`LoadData.createSolarInstallationForecast` writes its first key at
`baseYear + 1`). For an affordability indicator a missing year of self-generation is
not neutral.

## Reproducing the figures

The archives are not in the repository. `reproducibility/archive_manifest.csv` maps
each notebook to what it needs and whether that is published; `notebook_outputs.csv`
lists what each one writes. Regenerate both with:

```
python reproducibility/build_manifest.py
```

Notebook outputs land in `outputs/figs/` (PNG, gitignored) and `outputs/data/`.
