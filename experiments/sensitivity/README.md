# Sobol sensitivity figure

Regenerates `sobolS1STFourOoi.png`, the $S_1$-vs-$S_T$ figure for the GR4SP EMS
article. It replaces the Elementary Effects screening scatter
(`figscatter_mu_star_sigmna_median_OoiYear_filtered.png`): screening only
justifies which parameters were carried forward, whereas the article's actual
sensitivity argument in §5.4 and §6 is the gap between first-order and
total-order indices.

```
python sobol_s1_st_figure.py                              # figure into this folder
python sobol_s1_st_figure.py --outdir ../notebookGr4sp/outputs/figs
python sobol_s1_st_figure.py --check                      # verify only, no figure
```

`pandas` and `openpyxl` are required; `matplotlib` as well unless `--check` is
given.

## Source

The **sensitivity** ensemble — Saltelli, $N = 2100$, $k = 29$ parameters,
$C = N(2k+2) = 126{,}000$ runs, archived as `gr4sp_SOBOL2021-Feb-03.tar.gz`
(the `afterBaseYear` set). Not the 105,000-run hypothetical-past ensemble behind
the validation bands; `../validation/README.md` explains why the two are not
interchangeable.

`../notebookGr4sp/ema_gr4sp_SOBOL_ABY.ipynb` loads that archive, runs
`SALib.analyze.sobol` per analysis year, and writes the median across years from
the 2019 base year onward to
`../notebookGr4sp/outputs/data/SOBOL__median_sensitivity_Indices.xlsx`. That
workbook is committed, so this script reproduces the figure without the 589 MB
archive.

## What it plots

A 2×2 grid — GHGE, renewable energy share, wholesale price, retail tariffs, the
same four outputs in the same order as the EET figure it replaces. Each panel
shows horizontal grouped bars for $S_1$ and $S_T$ with their confidence
intervals, for that panel's own top 10 parameters by $S_T$, largest at the top.
Median only: the "Maximum" panel of the thesis version
(`figs/SOBOL/afterBaseYear/fig_barplot_s1_st_*.png`) and its year annotations are
dropped, because the article never discusses the year of maximum sensitivity.

Axis labels use the article's prose names, not the code identifiers — the
mapping is `LABELS` in the script. Note that `includePublicallyAnnouncedGen` is
misspelled in the model definition; the figure label spells "announced"
correctly. `priceChangePercentageWater` is labelled "Hydro price change" by the
same convention the article's own list uses for `nameplateCapacityChangeWater`
("Hydro capacity change") and `priceChangePercentageWind` ("Wind price change").

## Acceptance checks

Both run on every invocation, and the script refuses to write the figure if
either fails:

1. The top five by $S_T$ for wholesale price, GHGE and renewable share reproduce
   Table 5 of the article to three decimals.
2. No plotted parameter has $S_T \le 0$.

## Parameter counts

Read from the model definitions, which are the SALib problem definition:

| Stage | Count | Source |
|---|---|---|
| Screened by Elementary Effects | 34 | `gr4spModelEET.getModelAFterBaseYear` |
| Entering the Sobol stage | 29 | `gr4spModelSOBOL.getModelAFterBaseYear` |

The 29 are a strict subset of the 34. The five dropped are
`nameplateCapacityChangeCcgt`, `onsiteGeneration`, `priceChangePercentageCcgt`,
`priceChangePercentageSolar` and `technologicalImprovement`. $k = 29$ is what
makes $C = N(2k+2) = 126{,}000$, so §5.4's "34 screened, 29 retained" and
Table 5's caption agree with the code. The EET figure's $X_1$–$X_{25}$ labelling
does not; the thesis's $X_1$–$X_{34}$ numbering is the correct one.

The screening rule itself is in `ema_gr4sp_EET_ABY.ipynb`: $\mu^*$ and $\sigma$
are min-max normalised per output and a parameter is kept if either exceeds
`significance_bound = 0.2`, unioned across outputs.
