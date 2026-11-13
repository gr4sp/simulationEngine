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
| Wholesale price (monthly, annual) | AEMO VIC1 demand-weighted monthly price, 1999-01 to 2020-06 (request R2) | `aemo_VIC1_monthly_vwp_199812_202006.csv` (built by `scripts/data/aemo_vic1_monthly_price.py` from AEMO's aggregated price and demand files) |
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
differ. The price comparator covers 1999-01 to 2020-06 (AEMO, since Nov 2026); the OpenNEM extract, still used
for the renewable share, begins 2005-04 and ends 2020-06, both partial years.
The emissions series is annual from 1990. The tariff comparator is not
contiguous: Oakley Greenwood alone in 2001-2002 and 2007-2009, both series
2010-2017, St Vincent de Paul alone 2018-2019, and neither for 2003-2006. See
[Provenance of the tariff data](#provenance-of-the-tariff-data) for why those
years are absent.

**Update, Nov 2026 (R2):** the price rows now use the AEMO series from January 1999. The annual row still
resamples both sides from the same months; `wholesale_annual_windows()` prints it next to the simulated annual
output column, which agrees to the cent for 1999-2019 and differs only in 2020 (full year 40.95 against the
January-June 64.26): pinned BAU, annual NSE 0.56 resampled vs 0.48 from the annual column. The history below
refers to the earlier OpenNEM comparator.

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
contain, and any comparison charges the model for it. Closing the gap would need a
within-month weighted price persisted alongside the mean.

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

**Update, Nov 2026:** the extension is now applied (request R2), with the demand-weighted price that the article
uses (Angela, option G8 a). `wholesale_annual_windows()` also prints the monthly statistics against AEMO's
time-weighted mean, so the cost of the weighting stays visible (pinned BAU: RMSE 32.62 demand weighted, 28.37 time
weighted; bias -7.72 and -3.67).

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

This is the check the tariff averaging-window question asks for, and it settles part
of it:
the register's later rows are constructed, so the question of whether its `year`
labels are calendar or financial years is moot for 2003-2005 and 2017-2020, and
live only for the genuine rows.

### Since 12 Nov 2026: the additive tariff (`tariffComponentsFile`)

The base case no longer divides the simulated wholesale price by R_w. The tariff is
`(simulated wholesale + network + environmental + retail) x (1 + GST)`, with the three non-wholesale components taken
from the same Oakley Greenwood tables (2001-2017) and from AEMC price trends (2018-2020, forecasts to 2022/23, then
held in real terms) - `simulationSettings/tariff_components_VIC.csv`, built by `scripts/data/tariff_components_vic.py`.
The AEMC years are moved to the Oakley Greenwood basis: in the 2017 overlap the two differ by 5.98 c/kWh (real 2019,
excl. GST) in the non-wholesale part, almost all in retail, because Oakley Greenwood averages undiscounted market offers
and AEMC prices a representative, cheaper offer. Without that adjustment the tariff steps down in 2018 for no
physical reason (`--basis published` writes the unadjusted variant). The R_w register above is kept, unused, for the
comparison.

Why: dividing by R_w multiplies any wholesale error by 1/R_w (3-8 here), so the tariff swung between 6 and 31 c/kWh
before 2020 and fell to 1.5-4.7 c/kWh after it. The additive rule passes wholesale errors through one for one.

Seed 42, same fleet (12 Nov 2026):

| Tariff row | R_w rule | Additive (base case) |
|---|---|---|
| vs Oakley Greenwood, 2001-02 and 2007-17 (n=13): MAE / bias / NSE / r | 16.51 / -16.51 / -9.19 / 0.58 | 3.61 / -3.61 / 0.46 / 0.95 (2007 retail as published: 4.14 / -4.14 / 0.29 / 0.93) |
| vs St Vincent de Paul, 2010-19 (n=10): MAE / bias / NSE / r | 6.17 / -3.53 / -20.53 / 0.26 | 10.66 / +10.66 / -48.13 / 0.24 |

**What the Oakley Greenwood row now tests.** The non-wholesale components come from the comparator itself, so that
row is a test of the simulated wholesale component only: simulated minus recorded tariff is (simulated minus recorded
wholesale component) x 1.1. Its bias (-4.14 c/kWh) is the cost-based wholesale price running below the recorded
wholesale cost (the price row's bias), not a tariff error. It should be reported with that caveat, or replaced by a
direct comparison of the simulated wholesale component with the recorded one (`wholesale_recorded` in the register).
The St Vincent de Paul series measures a different quantity (about 15 c/kWh below Oakley Greenwood, see above), so the
additive tariff, on the Oakley Greenwood basis, sits above it; the R_w rule's smaller bias there came with r = 0.26.
2007 is the one year the components misfit: Oakley Greenwood's 2007 split (wholesale $402, retail $54) gives a
retail component of 1.43 c/kWh (the residual, squeezed by the drought year's recorded wholesale), and the additive
tariff was 15.1 against 23.4 recorded. Since 12 Nov (Angela) the 2007 retail component is interpolated between 2006 and
2008 (7.67 c/kWh, status `constructed` in the register); the 2007 tariff is 22.0 c/kWh and nothing else in the run
changes (prices and generation identical, seeds 42/7/99). For 2007 the Oakley Greenwood row is then no longer a pure
wholesale test, since its retail component is no longer the comparator's own.

## Audit repairs (13 Nov 2026)

The audit of 13 Nov 2026 (notes repo `docs/AUDIT_2026-11-13.md`) found defects that changed 1999-2019 results. They were
repaired in four commits, each with a regenerated baseline:
- `9641280` (F2): one clean `solar_ghi` value per half-hour. Midnight had used the day's peak irradiance.
- `73f0bd3` (F1): off-market units keep their capacity factor. Small hydro and gas had run at nameplate all year.
- `27f2402` (N21, F7): the solar fleet is added back to historic demand at its shape, and the forward period grows the
  calibrated base year.
- `063649e` (F4, F5): the forward tariff deflator, and the reporting of market output.

Mean of seeds 42/7/99 unless stated. Generation is against `generation_consumption_historic` 2005-2019; price NSE is
the time-weighted annual mean vs AEMO VIC1, 1999-2019.

| | Before (`847b549`) | F2 | + F1 | + N21/F7 | + F4/F5 (current) |
|---|---|---|---|---|---|
| Coal bias | -0.1 % | 0.0 % | +1.0 % | +1.0 % | +1.0 % |
| Gas bias | +26.1 % | +26.6 % | +4.4 % | +3.7 % | +3.7 % |
| Hydro bias | +0.4 % | +0.4 % | -1.7 % | -1.7 % | -1.7 % |
| Rooftop PV bias | -1.7 % | -9.7 % | -9.7 % | -9.7 % | -9.7 % |
| Utility solar bias 2018-19 | +14.9 % | +5.3 % | +5.8 % | +7.2 % | +7.2 % |
| Price NSE, time-weighted | 0.28 | 0.29 | 0.33 | 0.29 | 0.29 |
| Price 2019, $/MWh (record 124.4) | 74.2 | 75.6 | 74.1 | 69.3 | 69.3 |
| Unmet energy 1999-2019, GWh | 45.6 | 45.6 | 38.9 | 39.8 | 39.8 |
| Price 2030, $/MWh | 5.7 | 5.8 | 7.0 | 15.1 | 15.1 |
| Tariff 2030, c/kWh | 23.5 | 23.5 | 23.5 | 24.0 | 24.3 |
| Unmet energy 2050, TWh | 5.9 | 6.0 | 6.4 | 10.8 | 10.8 |

Current base case, with this script (`--run`), annual price vs AEMO demand-weighted 1999-2020:
- NSE 0.02 / 0.15 / 0.08 for seeds 42 / 7 / 99 (before: 0.07 / 0.14 / 0.13);
- bias -20.5 / -18.2 / -20.5 $/MWh;
- RE share NSE 0.91 (0.85 before);
- tariff vs Oakley Greenwood NSE 0.44-0.47.

Sanity suite, seed 42 (`checks.py`): all pass. Gas +3.7 % (was +25.8 %), gas burned vs AER +7.6 % (was +35 %), GHG NSE
0.64 (was 0.68).

What moved, and why:
- **Gas** is now close to the record. The off-market gas no longer runs flat, and the dispatchable hydro has its water
  back for the peaks.
- **Rooftop PV** is 10 % below the record once the phantom midnight output is gone. The fleet size or the 0.7168
  efficiency should be checked when step 3 (endogenous adoption) recalibrates the fleet.
- **The price fit** falls with N21. The double count had over-stated evening demand, which raised evening prices toward
  the record. The gap is part of the market-power mark-up the base case leaves out (cost-based competitive benchmark).
- **Minimum demand** in 2019 is now 3,210 MW against AEMO's 3,058 (2,255 before).

## N23: rooftop PV output (14 Nov 2026)

After the audit repairs rooftop PV was 10 % below the record (`generation_consumption_historic`, an AEMO/APVI
estimate). Three causes, measured one at a time on seed 42 (rooftop GWh against the record, 2011-2019 mean bias):

| Step | What | 2019 GWh (record 2,185) | Mean bias 2011-2019 |
|---|---|---|---|
| Before | | 1,893 | -9.6 % |
| a | Missing irradiance half-hours filled (445 in 2019, gave no output) | 1,976 | -8.5 % |
| b | + the CER installation record for 2011-2019: the APVI table stops in September 2019 and misses 191 MW of 2019's 461 MW; 2011-2018 agree within 0.5 % | 2,028 | -8.1 % |
| c | + irradiance on the tilted array for rooftop PV: the Clean Energy Council formula whose de-rating factors the model uses applies them to the irradiation on the array (CEC design guidelines, Issue 6.0, 2012, s. 8.1), not to horizontal irradiance; arrays face north at 16 degrees, the Australian mean (Killinger et al. 2018) | 2,208 | -0.3 % |
| d | + forward fleet from the ISP energy path at the model's own yield (no effect before 2020) | 2,208 | -0.3 % |

Year by year after step c: -6.2 % (2011), +0.7 % (2013), +0.8 % (2015), -3.9 % (2016), +6.2 % (2017), +2.7 % (2018),
+1.0 % (2019). The remaining scatter follows the single irradiance station (Melbourne Airport): the record's yield per
kW of the CER fleet is steady at 1,140-1,210 kWh a year, while the station's filled annual irradiance ranges from 1,458 to
1,590 kWh/m2 (2011-2019, -5 % to +4 % around its mean).
Sensitivity of step c: a fleet with a quarter of its arrays facing east and a quarter west at 20 degrees would receive
3.1 % more than a horizontal surface, instead of 8.6 %.

Step d changes the forward period only. The ISP gives rooftop PV as energy (raised by 25 % in `LoadData`, an unsourced
adjustment listed in `ASSUMPTIONS.md` A1); the old code turned each year's increment into capacity at a 20 % capacity
factor from 2021, while the model's yield is about 14 %, and added nothing between September 2019 and 2021.

Mean of seeds 42/7/99:

| | After the audit repairs (`063649e`) | After N23 |
|---|---|---|
| Rooftop PV bias 2007-2019 | -9.7 % | +0.9 % |
| Gas bias | +3.7 % | +2.0 % |
| Utility solar bias 2018-19 | +7.2 % | +11.6 % |
| Price NSE, time-weighted | 0.29 | 0.24 |
| Price 1999-2019, $/MWh | 30.7 | 30.3 |
| Price 2019, $/MWh (record 124.4) | 69.3 | 64.6 |
| Renewable share 2019 | 19.9 % | 20.6 % |
| Price 2030, $/MWh | 15.1 | 14.1 |
| Tariff 2019 / 2030, c/kWh | 31.3 / 24.3 | 31.1 / 24.1 |
| Unmet energy 2050, TWh | 10.8 | 9.5 |
| Rooftop PV 2030, TWh (seed 42) | 3.1 | 4.7 |

Sanity suite, seed 42 (`checks.py`): all pass; gas burned vs AER +5.1 %, RE share NSE 0.87, GHG NSE 0.64. Minimum demand
2019: 3,174 MW (AEMO 3,058). More rooftop output lowers midday demand, so prices fall further below the record and the
price fit drops. Utility solar gains from the filled half-hours; it keeps the horizontal irradiance, and the farms'
mounting (fixed or tracking) was not checked, so its bias is left open.

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

## N25: station emission factors, small-unit capacity factors, battery efficiency (14 Nov 2026)

Sources replace three unsourced settings (`ASSUMPTIONS.md`):
- **Emission factors**: each station's scope 1 emissions / production in the NGER designated generation facility data
  (Clean Energy Regulator), 2013-14 to 2018-19: Loy Yang A 1.159, Loy Yang B 1.134, Yallourn 1.306, Hazelwood 1.399,
  Anglesea 1.086 t/MWh; gas 0.533 (Newport) to 0.786 (Valley Power); Morwell 1.425 (heat rate x 95 kg/GJ, Acil Allen
  2016). Before, every brown coal unit had 1.23 and every gas unit 0.56 (Newport, a steam plant, is typed OCGT).
- **Off-market capacity factors**: each small unit's mean NGER output over its capacity; units without history take
  their Victorian class (landfill gas 0.648, small gas engines 0.193); embedded units (Longford, Qenos, the hospital
  backup diesel) 0 - AEMO's method for other non-scheduled generation (Demand Forecasting Methodology 2020, s. 4.1.2).
  Before, the landfill units took `Default` (0.9: the `Landfill` block never matched the database name `Landfill Gas`),
  Longford 0.30 and Qenos 0.65. Off-market non-rooftop output in 2019 falls from 1,510 to 1,110 GWh.
- **Battery round-trip efficiency** 0.85 is now sourced: Gannawarra measured 85.0 % (ARENA final report 2021); Ballarat
  contracted 86.8-87.2 %; IASR 2020 81 %. No change.

Mean of seeds 42/7/99:

| | After N23 (`e482238`) | After N25 |
|---|---|---|
| Coal / gas bias | +0.9 % / +2.0 % | +1.3 % / -2.2 % |
| Price NSE, time-weighted | 0.24 | 0.33 |
| Price 1999-2019 / 2019, $/MWh | 30.3 / 64.6 | 31.8 / 69.9 |
| Unmet energy 1999-2019, GWh | 39.2 | 42.7 |
| Price 2030, $/MWh | 14.1 | 14.6 |
| Unmet energy 2050, TWh | 9.5 | 9.5 |

The price rises because less small-unit output is taken out of market demand. That demand never contained it: the
monthly total it is calibrated to (`total_consumption_gwh`, OpenElectricity) sums only the fuel types of
`fetch_openelectricity.py`, without biogas, gas engines or distillate, while `Arena` still subtracts the units' output
(open item N28, the same kind of double count as N21).

Sanity suite, seed 42: all pass except the GHG NSE, 0.47 (0.64 before; tolerance 0.5). That indicator is emissions per
household x households / 0.3, so it carries the unsourced residential share. Direct scope 1 emissions (each unit's
generation x its factor) against the Victorian inventory, 2005-2018: NSE 0.66, bias -1.0 Mt with station factors;
0.69, -1.4 Mt with the old type factors. The indicator is to be revisited when solar step 2 replaces the 0.3.

## N29 and N28: the rooftop forecast and small units outside the demand record (14 Nov 2026)

**N29 (your decision).** The x 1.25 on AEMO's 2019 rooftop forecast is dropped (`rooftopForecastUplift: 1.0`): AEMO's
FY2020 value (2,485 GWh) is within 2 % of the recorded FY2020 output (2,439 GWh). The ISP/ESOO annual registers are
financial years (`forecastFinancialYears: true`): the demand growth from calendar year Y to Y+1 now uses the calendar
values (FY Y + FY Y+1) / 2 instead of FY Y+1 / FY Y, and the rooftop fleet on 1 January Y follows FY Y's energy. Check:
rooftop output in calendar 2021 is 3,008 GWh, the mean of FY2021 (2,876) and FY2022 (3,142). Nothing before 2020 changes.

**N28.** The monthly total the demand is calibrated to (`total_consumption_gwh`) sums the OpenElectricity fuel techs of
`fetch_openelectricity.py`, which leave out biogas, gas engines and distillate; `Arena` still subtracted those units'
output, so the market met less demand than the record. With `demandRecordFuels` (the recorded fuel types), the output
of the other off-market units is added to each month's target: calibrated demand minus the recorded total equals that
output exactly (15.3 GWh in July 2005, 23.5 in January 2012, 24.2 in June 2019; 0.18-0.29 TWh a year).

| Mean of seeds 42/7/99 | After N25 (`0f9bde3`) | After N29 and N28 |
|---|---|---|
| Coal / gas bias | +1.3 % / -2.2 % | +1.7 % / +3.8 % |
| Price NSE, time-weighted | 0.33 | 0.38 |
| Price 1999-2019 / 2019, $/MWh | 31.8 / 69.9 | 32.8 / 73.4 |
| Price 2030, $/MWh | 14.6 | 16.4 |
| Unmet energy 2050, TWh | 9.5 | 10.2 |
| Rooftop PV 2030 / 2050, TWh (seed 42) | 4.7 / 12.6 | 3.8 / 10.2 |

Sanity suite, seed 42: all pass but the GHG indicator (0.48, the 0.3 residential share, see N25); energy balance worst
year 4.4 % (tolerance 5 %). Minimum demand 2019: 3,254 MW (AEMO 3,058).

## Solar step 2: residential demand from the record; GHG from system emissions (14 Nov 2026)

Household use now follows the recorded residential series (`experiments/solarAdoption/STEP2_RESIDENTIAL.md`); the market
is unchanged (every market column identical). The GHG indicator now uses the year summary's `System GHG Emissions
(tCO2-e)` (each generator's output x its emission factor) instead of emissions per household x households / 0.3, which
held only while the residential share was 0.3: GHG NSE 0.68 (seed 42, 2005-2018; 0.48 on the old route). All sanity
checks pass.
