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
| Retail tariffs | Oakley Greenwood, and St Vincent de Paul, each on its own window | `2001to2019_historicTariffs.csv` |
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
contiguous: Oakley Greenwood alone in 2001-2002 and 2007-2009, both series
2010-2017, St Vincent de Paul alone 2018-2019, and neither for 2003-2006. See
[Provenance of the tariff data](#provenance-of-the-tariff-data) for why those
years are absent.

**The annual wholesale row was computed over mismatched windows and has been
corrected.** It read the simulated *annual* output column, which covers whole
calendar years, against an observed side covering only the months the OpenNEM
extract holds. Since the extract begins 2005-04 and ends 2020-06, two of the
sixteen years compared different spans — 2020 severely, a full simulated year
against six observed months, which put the simulated figure at 40.95 $/MWh where
the comparable January-June average is 64.26. Both sides are now resampled from
the same months. The other fourteen years are unchanged to the cent, because for
a complete year the annual output column already equals the mean of its months.

| Annual wholesale | MAE | RMSE | Bias | NSE |
|---|---|---|---|---|
| aligned to the comparator (tabled) | 14.08 | 18.33 | -6.83 | 0.50 |
| full calendar year (published, superseded) | 15.16 | 19.19 | -8.67 | 0.45 |

`wholesale_annual_windows()` prints both and the per-year shift, so the size of
the change stays on the record, as `tariff_comparators` does for the retired
tariff composite. **This is the one row in the table that deliberately departs
from the published Section 5 figure.** It is also why
`SimulationValidation.ipynb`, which resamples the monthly series, never
reproduced the published annual number.

No earlier comparator exists for price or renewable share. The database table
`generation_consumption_historic` has a null price before 2005-04, and its
pre-2005 per-fuel figures are a fixed-share reconstruction (wind is zero through
2004) that the model itself consumes as input.

GHGE is reported over two windows. 1998-2018 is the published span; 1998 is the
run's first year and still in mapping mode, which is why the bias is larger
there. 2005-2018 is the span that overlaps the other indicators.

The two tariff sources are not measuring the same quantity, and over the eight
years where both exist the Oakley Greenwood series runs 1.5-1.9x the St Vincent
de Paul one, a mean gap of 15.03 c/kWh. Each is therefore compared on its own
coverage rather than against their row-wise mean: Oakley Greenwood over
2001-2002 and 2007-2017 (n=13), St Vincent de Paul over 2010-2019 (n=10), 15
distinct years between them. The model sits inside the envelope the two sources span, and the sign of its error
depends on which is chosen - bias +2.14 c/kWh against St Vincent de Paul,
-12.14 c/kWh against Oakley Greenwood.

The row-wise mean is no longer tabled. It changes composition across the window
- one source to 2009, both to 2017, the other after - so with the sources 15
c/kWh apart it steps at 2010 and again at 2018, and that step entered the
composite's bias and NSE as if it were model error. `tariff_comparators` still
prints the composite's statistics and the per-source overlap figures so the size
of the artefact stays on the record. The tariff figure keeps the composite as
its band, because the panel spans every year either source covers.

The 2018 emissions comparator is not measured. It is the 2017 figure less
11.8 Mt for the retirement of Hazelwood.

## Resolution: what a weighted comparator can and cannot ask of the model

The wholesale comparator is **volume weighted within each month** — that is what
OpenNEM's "Volume Weighted Price (Historic)" is — while the simulated series is an
unweighted monthly mean. The two are therefore not the same kind of quantity, and
the size of that difference was measured on 5 September 2026 by rebuilding the
observed price from `total_demand_halfhour` (VIC1, half-hourly, from 1999) three
ways: unweighted, weighted by monthly demand, and weighted by half-hourly demand.

| Weighting applied | mean effect | max effect |
|---|---|---|
| **across** months | +0.23 $/MWh | 0.89 $/MWh |
| **within** months | **+4.06 $/MWh** | **15.70 $/MWh** (2019) |

Almost the entire effect is **intra-month covariance between price and demand** —
price spikes falling in high-demand intervals. Weighting *across* months barely
matters.

**The model cannot reproduce that, in principle.** It emits a monthly mean, which
has no intra-month structure left to weight: production-weighting the simulated
side moves it by at most 3.69 $/MWh against the observed side's 15.70. A weighted
comparator therefore contains information the simulated series structurally cannot
contain, and any comparison charges the model for it. Recorded as `ROADMAP.md`
item 14, whose first step — persisting a within-month weighted price alongside the
mean — is what would close the gap.

Measured on the monthly row over the same months, against three observed
conventions:

| Monthly observed series | MAE | RMSE | Bias | NSE |
|---|---|---|---|---|
| OpenNEM volume weighted (tabled) | 24.89 | 35.22 | -7.43 | 0.15 |
| demand weighted within month | 25.83 | 36.75 | -8.99 | 0.17 |
| unweighted mean of half-hourly prices | 22.93 | 31.71 | -4.03 | 0.00 |

> **NSE is not comparable across these rows.** It is normalised by the *comparator's*
> own variance, and removing the intra-month spikes removes much of that variance, so
> the unweighted row scores 0.00 despite the lowest RMSE and bias. Only MAE, RMSE and
> Bias may be read across the three.

### Consequence for extending the window

There is no price comparator before 2005-04, but `total_demand_halfhour` reaches
back to 1999-01-01 and its `price` column is **not read by the model** —
`LoadData.java:483` selects it and `:495-497` logs it, but only `total_demand` is
stored (`:504-505`) — so it is independent of the simulated price. Verified
reproducible: restoring `backupDB/DB-2021-8-21.sql` into a separate database gives
values identical to a working `gr4spdb` for every year 1999-2020, with identical
period counts, because the refresh scripts append to this table rather than
rewriting it.

Extending on that basis should use the **unweighted** annual mean, for the reason
above:

| Annual, 2000-2020 | n | RMSE | Bias | NSE |
|---|---|---|---|---|
| AER-style demand weighted | 21 | 18.07 | -8.59 | 0.49 |
| unweighted | 21 | **15.90** | **-4.15** | 0.51 |

against 16 years, RMSE 18.33 and bias -6.83 on the tabled window. **This extension
is not currently applied**; the table above is the case for it, not a description of
what the script does.

Comparing directly to the AER's figure 2.3 would stack three mismatches at once —
demand weighted, financial year, and weighted against *native* demand (initial
supply plus intermittent generation) rather than the operational demand this table
holds. The rooftop share of Victorian generation is 0% until 2010 and 4.9% by 2020,
so that last difference is the smallest of the three. The figure is better used as
context — whether the model reproduces the spikes at 2000-01, 2006-07, 2012-13 and
the 2016-18 climb — than as a source of statistics.

## Provenance of the tariff data

Two different things are easily conflated, so they are separated here. The
**comparator** is what the simulated tariff is measured against. The **register**
is an input the simulated tariff is computed *from*. Neither was documented
before; both were reconstructed and verified on 5 September 2026.

### The comparator: `2001to2019_historicTariffs.csv`, column 2

**This is not an ACCC publication.** It is a submission *to* the ACCC's inquiry:

> *Submission to the ACCC inquiry into retail electricity supply and pricing*,
> Victorian Electricity Distribution Businesses — AusNet Services, CitiPower,
> Powercor, Jemena and United Energy — 30 June 2017. The cost-stack analysis was
> commissioned from the consultants **Oakley Greenwood** (p. 5).
> <https://www.accc.gov.au/system/files/Victorian%20Electricity%20Distribution%20Networks.pdf>

The numbers come from its **Figure 1** (p. 6), captioned *"Composition (2016$) of
the annual residential electricity bill in Victoria (4,000 kWh; no electric
off-peak hot water), 1995, 2001 & 2002, and 2007 to 2017"*.

Each CSV value is the figure's annual bill divided by 4,000 kWh, expressed in
cents, and rescaled from 2016 to 2019 dollars by a single constant:

```
c/kWh (2019$) = bill(2016$) / 4000 * 100 * 1.0557
```

All thirteen values reproduce with a ratio between 1.0555 and 1.0559 — a spread
of 0.031%, i.e. one deflator applied uniformly. For example 2017: $1,425 / 4000
= 35.625 c/kWh, x 1.0557 = 37.61, which is the CSV value.

**The gaps in the comparator are the report's own gaps.** The report has no
2003-2006 and nothing after 2017, so those years are blank and
`validation_statistics.py` drops them (`dropna`, line 80). Nothing is
interpolated or carried forward. **1995 is the one year present in the report and
deliberately not used.** The Oakley Greenwood row's n=13 is therefore exactly the report's
coverage less 1995.

Two consequences worth stating in any write-up:

- The series is a **whole-of-bill average, not a usage tariff**. Figure 1's bars
  are the total annual bill — distribution, transmission, AMI metering, feed-in
  tariffs, VEET, RET, carbon price, wholesale, retail margin **and GST** — over an
  assumed 4,000 kWh. It amortises the fixed supply charge and includes GST, which
  plausibly accounts for much of the 15 c/kWh gap against St Vincent de Paul.
- The series is labelled **Oakley Greenwood** throughout - in `TARIFF_SOURCES`, in
  the summary table row and in the tariff figure's legend. It was previously
  labelled after the ACCC, which misattributed a consultant's analysis, prepared
  for a submission, to the regulator receiving it.

### The register: `historic_tariff_contribution`

Read by `LoadData.java:451` into `tariff_contribution_wholesale_register`, and
used by `EndUserUnit.java:149-150` as the wholesale share `R_w` of the retail
tariff. It has **22 rows, 1999-2020, with no gaps — but not every row is a
measurement.**

The seven component shares should sum to 1. That identity is the cleanest
available fingerprint of which rows are real:

| Years | Share sum | Reading |
|---|---|---|
| 1999-2002, 2006-2016 | 1.0000 (±0.01%) | genuine decompositions |
| 2003, 2004, 2005 | 0.9968 (-0.32%) | constructed |
| 2017 | **0.9490 (-5.10%)** | cannot be a decomposition |
| 2018 | **1.0921 (+9.21%)** | cannot be a decomposition |
| 2019, 2020 | 1.0028, 1.0016 | constructed |

- **1999-2001** repeat the 1999 row verbatim in all seven columns: one
  observation back-filled over three years.
- **2003-2005 are not an interpolation.** Interpolating linearly between the 2002
  and 2006 rows would give wholesale 0.2754, 0.3302, 0.3850; the actual values are
  0.25, 0.27, 0.29. What is there instead is a hand-built ramp — wholesale rising
  exactly +0.02/year, retail falling exactly -0.02/year to match, and every other
  component frozen at a round constant (transmission 0.035, distribution 0.300,
  GST and retail policies carried from 2002). It joins neither endpoint: 2002 to
  2003 steps +0.029 and 2005 to 2006 steps +0.150.
- **2017-2020 have no recoverable source.** They lie beyond Oakley Greenwood's
  2017 end (2018-2020 entirely), no file in this repository carries them, and the
  database preserves no provenance. The 2017 row is *within* the report's range
  but does not match it — the submission's own 2017 breakdown (p. 3) is wholesale
  23.6%, transmission 4.3%, distribution 25.4%, policy 12.1%, retail 25.5%, GST
  9.1%, summing to exactly 100.0; the register's 2017 is 20.0 / 4.1 / 25.9 / 11.8
  / 24.0 / 9.1, summing to 94.9.

**The register fills precisely the years the comparator omits, and none of it is
visible anywhere.** It is an input, not a plotted series, so no figure shows it.
Nor could a gap show: `EndUserUnit.java:152-157` silently falls back to the YAML
constant `wholesaleContribution.usage` (0.2837 in both `VIC.yaml:406` and
`VICfuture.yaml:167`) for any year absent from the map, and lines 160-161 then
clamp the result into [0.01, 1.0]. Neither substitution is logged.

**The lag compounds this at the earliest comparison points.** The model reads the
register at `year - 1` (`EndUserUnit.java:146`), so the simulated tariffs for 2001
and 2002 — the two earliest years in the Oakley Greenwood comparison — are both
computed from the back-filled 1999 value of 0.2089.

This is the check that `ROADMAP.md` item 10 asks for, and it settles part of it:
the register's later rows are constructed, so the question of whether its `year`
labels are calendar or financial years is moot for 2003-2005 and 2017-2020, and
live only for the genuine rows.

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
validation archive is 972 MB). Both are deposited on Zenodo under the concept
DOI <https://doi.org/10.5281/zenodo.4667996>, but in different versions: the
sensitivity archive in [8320754](https://zenodo.org/records/8320754), the
validation archive in [22172036](https://zenodo.org/records/22172036), published
2026-08-30. A Zenodo version does not inherit the previous one's files, so
neither version holds both. `fetch_results.py` merges the two file lists and
fetches either:

```
python ../simulationData/fetch_results.py hypopast   # the validation ensemble
```

You can cite all versions by using the DOI
[10.5281/zenodo.4667996](https://doi.org/10.5281/zenodo.4667996), which always
resolves to the latest one. Cite a version record only to pin the exact files.

Version 22172036 also carries `VICSimDataYearSummary_bau19982051.csv`, the pinned
BAU run this script reads. The copy in `../simulationData/` is the same file and
is committed, so the table reproduces without any download.

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
