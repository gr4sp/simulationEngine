# Validation statistics

Reproduces the simulation-mode validation reported in Section 5 of the GR4SP EMS
article: the summary table and the four figures.

```
python validation_statistics.py                     # summary table
python validation_statistics.py --figures ./out      # table + figures
python validation_statistics.py --ensemble ../gr4sp_SOBOLhypopast2021-Mar-03_includ_wholesale_month.tar.gz
```

Only `numpy` and `pandas` are required for the table; `matplotlib` is needed for
`--figures`. The EMA Workbench is **not** required — the ensemble archive holds
plain CSVs.

## What it compares

| Indicator | Comparator | Source file |
|---|---|---|
| Wholesale price (monthly, annual) | OpenNEM volume-weighted price | `2005_2020_OpenNemDataV1.csv` |
| Renewable energy share | OpenNEM per-fuel generation | `2005_2020_OpenNemDataV1.csv` |
| Retail tariffs | ACCC + St Vincent de Paul, row-wise mean | `2001to2019_historicTariffs.csv` |
| GHGE | Victorian Government inventory | `19902018_historic_emissions_Vic.csv` |

The simulated side is the pinned BAU run, `VICSimDataYearSummary_bau19982051.csv`
and `VICSimDataMonthlySummary_bau19982051.csv`.

## Metrics

`MAE` and `RMSE` in the indicator's own units; `Bias` is the mean error, signed,
so a negative value means the model runs below the record. `NRMSE` is RMSE over
the observed mean, which is the only figure comparable across indicators. `NSE`
is the Nash–Sutcliffe efficiency: 1 is perfect, 0 is no better than predicting
the observed mean, and below 0 is worse than that baseline.

Earlier drafts reported a "MAD", computed as `median(|simulated - observed|)`.
That is a median absolute *error*, not the median absolute deviation of
statistics, and it is not reported here.

## Windows

Each indicator is compared over the span its comparator covers, and the spans
differ. The OpenNEM extract begins 2005-04 and ends 2020-06, both partial years.
The emissions series is annual from 1990. The tariff comparator is not
contiguous: ACCC alone 2001-2009, both series 2010-2017, St Vincent de Paul
alone 2018-2019, and nothing for 2003-2006.

No earlier comparator exists for price or renewable share. The database table
`generation_consumption_historic` has a null price before 2005-04, and its
pre-2005 per-fuel figures are a fixed-share reconstruction (wind is zero through
2004) that the model itself consumes as input.

GHGE is reported over two windows. 1998-2018 is the published span; 1998 is the
run's first year and still in mapping mode, which is why the bias is larger
there. 2005-2018 is the span that overlaps the other indicators.

The 2018 emissions comparator is not measured. It is the 2017 figure less
11.8 Mt for the retirement of Hazelwood.

## Ensembles

Two exist and are not interchangeable.

| Archive | Runs | Uncertainties | Used for |
|---|---|---|---|
| `gr4sp_SOBOLhypopast2021-Mar-03_includ_wholesale_month.tar.gz` | 105,000 | 24 | validation bands |
| `gr4sp_SOBOL2021-Feb-03.tar.gz` | 126,000 | 29 | Sobol sensitivity indices |

Both are Saltelli samples with N=2100. The validation ensemble is a
hypothetical-past design: it varies parameters that could plausibly have differed
historically and omits six forward-looking ones the sensitivity analysis
includes. Neither is in the repository (`*.tar.gz` is gitignored, and the
validation archive is 972 MB). The sensitivity archive is on Zenodo at
<https://doi.org/10.5281/zenodo.8320754>; **the validation archive is not yet
deposited anywhere.**

Rows 41357 and 41379 of the validation ensemble are empty and are dropped, which
is what the source notebook does.

## Why the ensemble spread is not reported as a validation statistic

Earlier drafts drew bands at one and two ensemble standard deviations and
reported the share of observations falling inside them. That measures how far
the output moves when the inputs vary, not how well the model matches the
record. For emissions the median sigma is 18.9 Mt against observations whose own
standard deviation is 4.4 Mt, so the band is more than four times wider than the
variation it is asked to contain and full coverage is guaranteed by
construction. Run with `--ensemble` to reproduce that comparison.

The figures therefore use bands of one and two RMSE, which are built from the
model's own error.
