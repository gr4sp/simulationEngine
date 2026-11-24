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

The 2018 emissions comparator (46.4 Mt) is the inventory value, not an estimate. Corrected 19 Nov 2026: this note
used to say it was the 2017 figure less 11.8 Mt for Hazelwood, which would give 44.3 Mt. The Victorian Greenhouse
Gas Emissions Report 2018 (DELWP 2020; data from the State and Territory Greenhouse Gas Inventories released in
May 2020) prints it in Table 1 (p. 18): electricity generation 63.5 Mt CO2-e in 2005, 46.4 Mt in 2018, change -17.1 Mt;
net emissions 102.2 Mt, of which electricity generation 45.4 %. Checked against the PDF on 24 Nov 2026. The same
report's other figures match the file too: the 12.8 Mt fall over 2016-2018 (p. 20) is 59.2 - 46.4, and its
Appendix A, Table 3, shows no revision to electricity generation for 2005 or 2017 since the 2017 report. The PDF is
kept in `Sandbox/papers/GR4SPArticle_ERSS/` (`2018 Victorian-Greenhouse-Gas-Emissions-Report-2018a.pdf`). The
later reports (2019, 2020) would extend the record to 2020 and show any revisions; they were not downloaded.

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
weighted; bias -7.72 and -3.67). **Superseded 24 Nov 2026:** the table now uses the time-weighted comparator (section "Price basis and the
recursive emission index" below).

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

## Seed-band figures (R6, 19 Nov 2026)

`seed_band_figures.py ensemble_2026-11-20` draws the methods article's Figures 2-5 from the 100-seed ensemble of
`seed_ensemble.py`: the record, the median over the seeds and the 5-95 % band of the seeds, year by year (month by
month for the monthly price), with the median and 5-95 % range of the NSE in the legend. Outputs (PNG and PDF):
`ensemble_2026-11-20/figures/` (`ghgeSeedValidation`, `reSeedValidation`, `tariffsSeedValidation`,
`wholesaleSeedValidation`, the last with a monthly and an annual panel). The seeds differ only in the model's random
draws (forced outages and other events), so the band is the model's stochastic spread, not parameter uncertainty. It
is wide only for price; for emissions, RE share and tariffs it is under about 1 unit in most years. The figures
replace the +/-1 and +/-2 RMSE bands of `--figures`, which drew a constant-width band around one run.
`seed_months.csv` (monthly simulated price per seed) was written once with `--runs` from the run folders.

**RE share row, like for like (19 Nov 2026).** The record's first and last years are partial (2005: April-December;
2020: January-June), but the row compared them with the full simulated year. Both sides now cover the record's months
(`renewable_share_simulated`, generation-weighted as the year column; complete years unchanged to the cent). 2020:
simulated 31.4 % full year against 26.9 % January-June (record 23.0 %). Effect: repaired ensemble NSE 0.88 -> 0.97
(5-95 %: 0.97-0.97), bias +0.40 -> +0.12 pp; pinned BAU 0.74 -> 0.72. No other row changed.

## Monthly generation by fuel, and what is happening with gas (R6, 19 Nov 2026)

`fuel_series()` compares the model's monthly output by fuel with OpenNEM, April 2005 to June 2020 (183 months), and
`seed_ensemble.py` reports it over the 100 seeds (`seed_fuel_months.csv`, figure `fuelSeedValidation`: monthly series
and mean by calendar month in GWh per day). Median NSE (5-95 %): coal 0.89 (0.88-0.90), gas -1.31 (-1.66 to -1.05),
wind 0.84, hydro 0.97 and rooftop PV 0.98 (both largely inputs: recorded monthly hydro energy, CER installations),
monthly renewable share 0.94. Utility solar is left out (the record covers only the five semi-scheduled farms, and the
monthly summary has no per-unit columns; the annual like-for-like comparison stays).

Record gaps: the extract has no wind before 2009, while five farms of 134 MW ran (Codrington, Toora, Challicum Hills,
Wonthaggi, Yambuk); the likely reason is that they had no AEMO dispatch data before the semi-scheduled category (to
check). The model's wind runs at a constant capacity factor (0.30, `VIC.yaml` Wind `maxCapacityFactor` and
`maxCapacityFactorSummer`), so it has no seasonal or weather shape; the record peaks in winter (July 7.0 against
April 4.7 GWh/day). New wind farms produce at that factor from their start date (2020 well above the record), as utility
solar did before N27.

**Gas.** Gas is the residual fuel in the merit order: it supplies what coal, hydro, wind, solar and imports leave.
Its errors trace to two mechanisms, both visible in the data (100 seeds, energy balance by month):

1. **Imports are a last resort.** The demand is calibrated to local generation plus recorded imports
   (`generation_consumption_historic.total_consumption_gwh`; 2019: 46,393 + 1,937 = 48,330 GWh). The model imports
   only when Victorian capacity runs out (about 26 GWh/yr, step 4 of 9 Nov), while the record imported 0.05-1.9 TWh/yr
   whenever the neighbours were cheaper, most in winter (May-August 2.5-3.2 GWh/day against 0.9-1.3 in October-December, 2005-2016). Each
   year 2006-2019 the model's extra local generation equals the recorded imports (r = 0.999, within 68 GWh), and gas
   makes most of it: 2006 +1.4 TWh, 2018 +0.8.
2. **Coal availability is flat through the year.** Recorded brown coal output dips in April-May and September-November
   (122-124 GWh/day against 128-131 in summer and winter, April 2005 to June 2020), consistent with planned maintenance in the
   low-demand seasons; the model's coal availability is one constant (0.90) with random breakdowns, so its coal dips
   less. Model gas therefore follows demand peaks (summer and winter, 7 GWh/day) and almost stops in autumn and spring
   (2.5), while recorded gas is nearly flat (3.6-5.7 GWh/day).

Not explained yet: 2012-2014, when the model's coal is 1.4-1.9 TWh/yr above the record and its gas 1.3 TWh/yr below.
The carbon price (2012-14) is an input, and gas sets the model price in only 3 % of 2013's half-hours. Candidate events,
not checked: the Yallourn mine flood (June 2012) and how Mortlake was run. 2019 coal (+2.8 TWh) includes the long Loy
Yang A unit 2 outage (May-December 2019), which random breakdowns cannot reproduce (to check against AEMO's records).

Repair options (Angela's decision; each changes results): imports offered in the merit order at the neighbour's cost
up to each link's limit, not only at shortfall; a seasonal coal availability (planned maintenance) from AEMO's
availability data; or both stated as limitations, since gas and imports carry a small share of energy (gas 1-7 % of generation) but
set the price at the margin.

Sources for the practice followed: `VALIDATION_PRACTICE.md`.

## Price duration curves (R6, 19 Nov 2026)

`seed_price_duration.py` reruns the 100 seeds with the half-hourly diagnostic log (`-Dgr4sp.priceSetterLog`), using the
compiled classes and settings kept with the ensemble (`3165126`, the frozen model; seed 42 reproduces the ensemble's monthly summary byte
for byte), and keeps only summaries: `seed_price_duration.csv` (price exceeded in a given % of half-hours, by period),
`seed_halfhour_years.csv` (per seed and year: mean price, share of half-hours above 300 $/MWh, imported GWh, share of
half-hours each fuel sets the price). The record is AEMO's half-hourly VIC1 price in the database
(`total_demand_halfhour`, 368,146 of 368,160 half-hours 1999-2019; `record_price_duration.csv`). Figure
`priceDurationSeedValidation` (linear axis to 300 $/MWh; the share above it is printed in each panel).

| Period | Half-hours below 15 $/MWh, model / record | Median half-hour price, model / record ($/MWh) | Half-hours above 300 $/MWh, model / record |
|---|---|---|---|
| 1999-2008 | 71 % / 15 % | 10 / 25 | 0.03 % / 0.27 % |
| 2009-2016 | 42 % / 6 % | 25 / 33 | 0.09 % / 0.12 % |
| 2017-2019 | 36 % / 2 % | 83 / 88 | 0.21 % / 0.35 % |
| 1999-2019 | 55 % / 10 % | 13 / 31 | 0.09 % / 0.22 % |

Reading: the middle of the distribution is close in 2009-2016 and 2017-2019 (from about the 5th to the 40th percentile
from the top), and only around the 20th percentile in 1999-2008; the largest errors sit at both ends. (1) A floor: in a large share of half-hours the model's price is brown coal's
short-run cost (about 8-12 $/MWh), while recorded prices at low demand stay at 15-30 $/MWh; brown coal sets the model
price in 31-77 % of half-hours (yearly medians over the seeds). Over 1999-2019 the model's unweighted half-hour mean
is 12.2 $/MWh below the record's: the lowest 60 % of half-hours give 5.2 of it, the top 1 % 2.3, the rest 4.7. The floor
is what a cost-based
competitive benchmark gives (withholding is off by design); the record's low-demand prices carry offers above cost.
(2) Too few spikes, worst in 1999-2008 (0.03 against 0.27 %). The model also
has no negative prices (record 0.13 % of half-hours, 0.36 % in 2017-2019), as AMIRIS (Nitsch et al. 2021). Model imports
(`ensemble_2026-11-20`, corrected 23 Nov 2026): median over the seeds of each seed's 1999-2019 mean **20.4 GWh/yr**
(5-95 % 16.4-25.9); by period 24.8 (1999-2008), 12.7 (2009-2016) and 25.8 (2017-2019). "13-26" was the range of those
period medians, not stated as such. Recorded imports: 50-1,900 GWh/yr.

## Frozen model and the final 100-seed ensemble (20 Nov 2026)

The model is frozen: Java code and `simulationSettings/` as at `3165126`, release tag `v0.3-methodsx` (the commit
holding this ensemble). It is the base case of `07eedaf` (N27, Numurkah start date) with the N38 switches present but
off (Angela, 20 Nov: keep the base case; imports as last resort, flat coal availability and constant wind are stated as
limitations, tests in the notes `docs/n38_probes/`). The earlier ensemble (`b796f64`, before N27) was rerun on the
frozen model, seeds 1-100: `ensemble_2026-11-20/` replaces `ensemble_2026-11-19/` (git history keeps it). Every
median NSE moved by under 0.01 (annual price 0.24 -> 0.25, monthly price -0.07 -> -0.06; emissions, RE share, tariffs,
coal, gas and wind unchanged to two decimals). In the price duration reading only the 2017-2019 share above
300 $/MWh (0.20 -> 0.21 %), the mean gap (12.3 -> 12.2 $/MWh) and coal's price-setting share (32-77 -> 31-77 %) moved.

## Year basis, money basis and price events (23 Nov 2026, questions from the methods article draft)

Model results unchanged: the code adds only output (a monthly `System GHG Emissions (tCO2-e)` column) and an opt-in
generator parameter (`endYear`, used by the event runs only). The 100 seeds were rerun on it: every year, monthly and
price file of every seed is identical to `ensemble_2026-11-20` (the summaries differ by 1e-14, from summation order).
46 tests pass.

**Money basis.** The simulated tariff is in real January-2019 c/kWh: `EndUserUnit` deflates the six-month mean
wholesale price with the CPI table and adds the non-wholesale components, which `tariff_components_VIC.csv` gives in
real January-2019 c/kWh; both comparators are in 2019 dollars. The tariff figure's axis said "nominal" (a label error,
fixed). Wholesale prices are nominal on both sides: generator costs are set in `costPriceYear` dollars and moved to
each year's dollars with `cpiFile`, the carbon and gas prices are nominal, and so is the AEMO record. The price axes now
say so.

**Emissions year basis.** The inventory reports financial years ("2017" = 1 July 2016 to 30 June 2017; Victorian
Greenhouse Gas Emissions Reports 2022 and 2023 state the convention: DEECA, *Victorian Greenhouse Gas Emissions
Report 2022*, https://climatechange.vic.gov.au/__data/assets/pdf_file/0020/726203/victorian-greenhouse-gas-emissions-report-2022.pdf;
*Victorian Greenhouse Gas Emissions Report 2023*,
https://www.climatechange.vic.gov.au/__data/assets/pdf_file/0038/767855/Victorian-Greenhouse-Gas-Emissions-Report-2023.pdf;
the site blocks automated downloads, open them in a browser); the year summary is a calendar year. Hazelwood
closed on 31 March 2017, so inventory 2017 has it for nine months and model 2017 for three: that is most of the 2017
gap. `emissions_year_basis.py` sums the monthly emissions over July-June years (`ensemble_2026-11-20/
emissions_year_basis.md`; median and 5-95 % over 100 seeds):

| Basis | Window | NSE (-) | Bias (Mt CO2-e) | RMSE (Mt CO2-e) |
|---|---|---|---|---|
| Calendar (as reported so far) | 2005-2018 | 0.70 (0.68 to 0.71) | -0.64 | 2.85 |
| July-June (the inventory's) | 2005-2018 | 0.93 (0.92 to 0.94) | -0.03 | 1.36 |
| Calendar | 1999-2018 | 0.37 (0.35 to 0.38) | -1.73 | 3.50 |
| July-June | 2000-2018 | 0.71 (0.69 to 0.71) | -1.10 | 2.45 |

The 2017 gap falls from -7.9 to -2.0 Mt, 2012 from -4.7 to -1.4 (carbon price start, July 2012) and 2014 from +3.1 to
+1.0 (its end, June 2014). What remains is the 2000-2005 shortfall (-2.3 to -5.5 Mt), not yet explained. The July-June
1999 year is dropped (July-December 1998 is spin-up).

Angela's decision (23 Nov 2026): the main table uses July-June years; the calendar-year rows move to a supplement
table, kept for comparison with earlier versions. `seed_ensemble.py` now writes both (indicators `GHGE (annual,
July-June)` and `GHGE (annual, July-June, full)` beside the calendar ones); `seed_summary.md` regenerated from the
100-seed rerun, every other row identical. The emissions figure (`ghgeSeedValidation`) uses July-June years too. When
reporting, state that the 2000-2005 shortfall remains, so the higher NSE does not read as a full fit.

**Price gap.** The 12.2 $/MWh mean gap (record 45.69 $/MWh, unweighted half-hour mean 1999-2019) can now be recomputed
without the database: `ensemble_2026-11-20/record_halfhour_years.csv` (record by year, `seed_price_duration.py record`)
with `seed_halfhour_years.csv`: median 12.2 (5-95 % 10.1-13.9) $/MWh over the seeds. Seed 42 ranks 89th of 100 on
annual price NSE in this ensemble (87th was the 19 Nov ensemble).

**Price events** (`price_events.py`, `price_events_2026-11-23/`; seeds 1-20 and 42, each paired with the same base
seed, so the forced outages are identical; effect = base minus the run without the event, median over the seeds):

| Event removed | Years | Price effect ($/MWh, nominal) | Emissions effect (Mt CO2-e) |
|---|---|---|---|
| Carbon price | 2012 / 2013 / 2014 | +15.3 / +30.2 / +4.2 | -0.1 each year |
| Hazelwood closure (kept open to 2019) | 2017 / 2018 / 2019 | +61.5 / +66.6 / +61.0 | -3.3 / -3.4 / -3.0 |
| Gas price rise (held at 4.15 $/GJ from 2016) | 2017 / 2018 / 2019 | +30.9 / +33.8 / +30.0 | 0.0 |
| Closure and gas rise together | 2017 / 2018 / 2019 | +64.4 / +67.0 / +61.7 | -3.3 / -3.4 / -3.0 |

Reading:
- **Carbon price.** Full pass-through: about +30 $/MWh in every month it applied (23-24 $/t x brown coal at the
  margin), 2012 and 2014 are half years. January 2014 (heatwave) is the exception: the run without the carbon price
  has higher spikes (-96 $/MWh median that month), which pulls 2014 down; not yet explained (candidate: hydro offers
  follow last month's price, `Arena` sets the reference price from it). No emissions effect: brown coal stays cheaper
  than gas with the carbon price, so the model has no fuel switching. The recorded 2012-13 fall in emissions comes into
  the model through the demand input (below).
- **Hazelwood and gas interact.** With Hazelwood open, gas is almost never at the margin and the gas price adds about
  3 $/MWh (joint minus Hazelwood alone); with Hazelwood closed it adds about 31. The closure adds about 33 $/MWh at
  2015 gas prices (joint minus gas alone) and about 61 at recorded ones. The interaction (joint minus the sum of the
  two single effects) is -28 to -33 $/MWh. Neither event alone gives the 2017 step: the closure puts gas at the
  margin, the gas price sets how far prices rise.
- **The demand input carries trade.** Demand is calibrated to recorded local generation plus imports, so it includes
  exports. When Hazelwood closed, exports fell from 8.0 TWh (2016) to 3.9 (2017) and 2.0 (2018) and the demand input
  fell with them (55.2 -> 49.4 -> 48.4 TWh). Kept open in the event run, Hazelwood (7.8-8.6 TWh/yr, seed 1) displaces other
  Victorian coal (3.5-6 TWh) and gas instead of being exported, so the run measures "Hazelwood open at the demand
  recorded after it closed": its emissions effect is a lower bound and its price effect an upper bound. The same holds
  for the carbon price years: exports fell from 5.0 TWh (2011) to 3.5-3.7 (2012-13).


## Price basis and the recursive emission index (24 Nov 2026, questions from the methods article draft)

Model results unchanged: no Java code or setting changed. Only the price comparator of the validation table changed,
and two checks were added.

**Price basis (case B in the main table, case C as a supplement; Angela, 24 Nov 2026).** The simulated monthly price is
a plain (time-weighted) mean of the half-hourly primary-spot prices (`Arena`); the table compared it with AEMO's
demand-weighted price (option G8 a, above), so it charged the model with the price-demand covariance it does not
report. `validation_statistics.load_price_comparator()` now returns AEMO's time-weighted monthly mean (column `mean` of
`aemo_VIC1_monthly_vwp_199812_202006.csv`); `load_price_comparator("demand")` keeps the demand-weighted one, and
`wholesale_annual_windows()` prints it as the cross-check. Statistics recomputed on the 23 Nov runs
(`ensemble_2026-11-20/seed_summary.*`, `seed_statistics.csv`; only the two price rows change; figure
`wholesaleSeedValidation` redrawn, record labelled time-weighted):

| Wholesale price, median (5-95 %) | NSE | Bias ($/MWh) | RMSE ($/MWh) |
|---|---|---|---|
| Monthly, demand-weighted comparator (before) | -0.06 (-0.45 to 0.19) | -16.51 (-18.09 to -14.56) | 37.99 (33.15 to 44.41) |
| Monthly, time-weighted comparator (now) | -0.29 (-0.96 to 0.05) | -12.46 (-14.04 to -10.51) | 33.48 (28.71 to 41.27) |
| Annual, demand-weighted comparator (before) | 0.25 (0.11 to 0.44) | -17.00 (-18.71 to -14.81) | 22.24 (19.20 to 24.27) |
| Annual, time-weighted comparator (now) | 0.39 (0.26 to 0.54) | -12.66 (-14.38 to -10.47) | 17.91 (15.47 to 19.62) |

About 4 of the 17 $/MWh bias was the basis. The monthly NSE falls although bias and RMSE improve: the time-weighted
record varies less from month to month, so the same errors weigh more against its variance.

Case C (`seed_price_duration.py` now also writes `seed_halfhour_months.csv`, each seed's monthly price on both bases
from the half-hourly log, and `price_basis.md`; `python seed_price_duration.py basis OUT_DIR` rewrites the report):
both sides demand weighted, 1999-2019 (the log's years), 100 seeds: monthly NSE -0.45 (-1.37 to 0.05), bias -12.52,
RMSE 42.88; annual NSE 0.39 (0.25 to 0.55), RMSE 20.10. Case B on the same window: monthly NSE -0.23, annual 0.42.
Weighting raises the simulated price by 3.46 (2.63 to 4.55) $/MWh, the record's by 3.75: the model's price-demand
covariance is about right on average, but its spikes fall in different months (January 2009: the model's heatwave
spike, weighted, adds 18.7 $/MWh to seed 42's 2009 against 7.0 in the record), which is why the monthly NSE drops
further. The log's monthly mean equals the output column within 0.24 $/MWh (seed 42, 252 months).

**Recursive emission intensity index** (`emissions_index.py`, `ensemble_2026-11-20/emissions_index.*`). The validated
emissions are the system total (generator MWh x factor); the index (`Spm.computeIndicators`, tonnes per MWh consumed,
written as household emissions / household consumption) was not tested. Two tests:

1. *Consistency.* Index / system intensity (System GHG Emissions / system generation, month by month) is
   1.053-1.066 over 1999-2020, the same in every seed to 1.3e-4: the recursion neither loses nor adds emissions; it
   only scales them by the network losses.
2. *Independent comparators.* NGA Factors (DISER, August 2021, Table 46, Victoria, July-June years; the last edition
   with the time series; `simulationData/nga_factors_2021_table46_vic.csv`, from the kg/GJ columns because the
   kg/kWh column reads 0.102 for 2017/18, a typo for 1.02) and AEMO's CDEII for VIC1 (daily, from 2011-12):

| Model series vs comparator | Years | NSE (5-95 % over seeds) | Mean relative bias | r |
|---|---|---|---|---|
| Index vs NGA scope 2 (per kWh sent out) | 2000-2019 (16 years) | 0.88 (0.87 to 0.89) | +1.4 % | 0.95 |
| Index vs NGA scope 2 + losses (per kWh delivered) | 2000-2019 (16 years) | 0.03 (0.01 to 0.05) | -7.3 % | 0.93 |
| System intensity vs AEMO CDEII VIC1 (per MWh sent out) | 2012-2020 (9 years) | 0.23 (0.20 to 0.25) | -7.8 % | 0.95 |

"Scope 2 + losses" = scope 2 + end-user scope 3 - 0.01 (the network operators' scope 3, fuel supply only), the
quantity the index stands for. The trend is right (r 0.93-0.95); the level is 7 % low, for two reasons found here:

- *As-generated basis.* The generator factors are NGER scope 1 / NGER electricity production, which is as generated
  (Acil Allen 2016, report to AEMO, s. 2.5, 3.1). The model's generation (rooftop excluded) is 7-11 % above AEMO's
  sent-out energy in 2011-12 to 2019-20 while its emissions agree within about 5 % (2012-2018): the stations' own use
  is inside the model's MWh, so every intensity per MWh is low against a sent-out comparator.
- *Network losses (a code defect).* `Spm.computeNetworksLosses` sums the losses of the network assets active on the
  date but divides by the number of all the SPM's assets, past owners included (transmission: 2 active of 12 rows,
  (0.1 + 0.1) / 12 = 1.7 %; 400 V and distribution: 2-3 of 7 rows, 0.6-0.9 % each; customer 3 %). Chained, this gives
  1.0666 in 1999 and 1.0605 from 2016, which is the ratio in test 1. The intended mean over active assets would give
  about 1.19 (19 %), with an unsourced 10 % transmission loss; NGA's losses part is 7-12 % of scope 2 for Victoria (2000-2019).

The index feeds only the household emissions output (`EndUserUnit.currentEmissions`, `SaveData`): prices, tariffs and
the validated system emissions do not depend on it. Repair options (results change for that output only; not done):
mean over active assets, with loss values sourced (AEMO transmission and AER distribution loss factors); and a
sent-out denominator (auxiliary use per technology). NGA scope 2 matching the index (+1.4 %) is partly the two
differences offsetting each other, so it should not be reported as a validation on its own.
