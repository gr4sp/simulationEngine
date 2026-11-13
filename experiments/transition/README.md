# Transition timing: commissioning delay and coal closure (R3 forward period, 13 Nov 2026)

The thesis argued that the transition has to be orderly: coal phase-out coordinated with the arrival of new
generation, and project delays (planning, environmental approval, connection, commissioning) can open a gap between
them. In the thesis the knob was `generationRolloutPeriod`. In the repaired base case the ISP 2020 capacity path tops
wind, solar, battery and gas up to its target every financial year, so a delayed project is replaced at once by
generic capacity and the knob does nothing (ABY Morris pilot, `experiments/sensitivity/PARAMETER_AUDIT_R3.md`: mu*
0.01-0.03). The path is a plan, and a plan assumes projects arrive on time; a delay should act on it, not be filled in
by it. This folder holds the calibration of a commissioning delay `d` and the scenarios built on it.

**Scope (Angela, 13 Nov 2026).** This material is for the later future transition scenarios and policy-mixes
article, not the ERSS article. The ERSS article covers the past (1999-2019) on the information available in 2020
(ISP 2020, the 2019 closure dates in the database). The post-2020 facts below (Yallourn and Loy Yang A agreements,
Eraring extensions, slippage measured with AEMO's 2025 file) are recorded as considerations and limitations of that
analysis. `d` stays 0 in the base case; the closure rule of section 3 (tracker N19) is deferred to that article.

## 1. How late did Victoria's 2019 pipeline arrive?

`scripts/data/commissioning_slippage_vic.py` follows every Victorian project in the model's pipeline
(`generationassets`, DB-2021-8-21) into AEMO's Generation Information of July 2025
(`commissioning_slippage_VIC.csv`). The database pipeline is AEMO's list of mid to late 2019: it has the site renames
of 18 Jul 2019 and not those of 12 Nov 2019. Slippage = actual full commercial use date minus the date expected in
2019 (where the July 2025 file has no date, the change-log entry that moved the project to In Service, which can lag
by up to a quarter; four projects).

**Committed, Emerging and In Commissioning in 2019** (21 projects, 3,141 MW):

| Outcome | Projects | MW |
|---|---|---|
| built | 18 | 2,860 (91 %) |
| in commissioning (Cohuna) | 1 | 31 |
| still announced (Carwarp I) | 1 | 100 |
| dropped (Kiamal stage 2) | 1 | 150 |

Of the 17 built with a date: median **15.5 months late** (1.3 years), capacity-weighted mean 19.4 months (1.6 years),
interquartile range 10-23 months, from -1 (Mt Gellibrand) to 33 months (Mortlake South). Only Cherry Tree and
Mt Gellibrand were on time. The largest wind farms (Stockyard Hill 532 MW, Dundonnell 336, Moorabool 320) were
23-25 months late; Kiamal Solar Farm was 22 months late and then constrained by the West Murray system-strength
limits.

**Publicly Announced in 2019** (49 projects, 8,942 MW, used in the model only when `includePublicallyAnnouncedGen`):

| Outcome by July 2025 | Projects | MW |
|---|---|---|
| built | 7 | 698 (8 %) |
| committed or in commissioning | 7 | 1,693 (19 %); expected 39-71 months after the 2019 date |
| still announced | 16 | 5,158 (58 %), incl. Star of the South (2,000 MW, now 2032) |
| dropped or no longer listed | 18 | 1,312 (15 %) |
| duplicate (Moira = Wunghnu) | 1 | 80 |

The seven built were a median 21 months late (3 to 37). The 2019 dates of announced projects are placeholders
(mostly 1 January), so the useful number here is the completion rate, not the slippage.

**Against the ISP 2020 path** (`installed_vs_isp2020_VIC.csv`, installed on 1 July): the path's FY2021 level
(existing plus committed: wind 3,977 MW, solar 964 MW) was reached in FY2024 for wind (4,413 MW) and FY2023 for solar,
so about two to three years late. Victoria then caught up because the 2020 ISP path is flat until FY2025; the steep
part of the path (wind +480 MW a year, FY2026-FY2030) is still ahead.

Two database findings, left for a decision: Moira Solar Farm (80 MW) is Wunghnu Solar Farm under its old name, so the
announced pipeline counts it twice; and the brown coal closure dates are the 2019 ones (Yallourn units 2029-2032,
Loy Yang A 2048), not the announced ones (Yallourn mid-2028, announced March 2021; Loy Yang A 2035, AGL, 2022). The
ISP 2020 path was built with the 2019 closures too.

## 2. The commissioning delay `d`

`forecast.commissioningDelay` (years, fractions applied in whole months; VIC.yaml and VICfuture.yaml, default 0).
Read from the after-base-year settings, like the other forecast parameters. It moves:

- every pipeline project of `generationassets` (Committed, Committed*, Emerging, In Commissioning and, when included,
  Publicly Announced) whose planned date is after the base year (1 Jan 2019), by `d`;
- every step of the capacity path (`capacityPathFile`), by `d`: the FY step planned for 1 July arrives `d` later.

Negative `d` (faster approvals, connection and commissioning) brings them forward, but never before the base year or,
for the path, before its first step (1 July 2020). `generationRolloutPeriod` still spreads each pipeline project over
N years, now starting from the delayed date. Existing plant and closures do not move; coal closures have their own
shift, `generatorRetirement`. Code: `LoadData.commissioningDate`, `selectGenTech`, `addCapacityPath`; test:
`LoadDataIT.commissioningDelayShiftsPipelineAndCapacityPath`.

**Calibrated values.** Base case `d = 0` (the plan on time, as before; the baseline does not change). A realistic
delay, from the committed projects above, is `d = 1.3` years (median), with a range of about 0.8 to 2 years
(interquartile range) and up to 3 years (the slowest committed project; the ISP 2020 path's FY2021 level). Literature
for faster delivery (`d < 0`): Australian wind lead times fell from more than 88 months to about 53 for projects
started after 2016, solar to 41 (Clapin and Longden 2024, Energy Economics 107337); federal environmental decisions
went from 1.3 to 2.2 years between 2019 and 2021 referrals (CEIG and Herbert Smith Freehills 2024). A scenario range
of `d` from -1 to +3 years covers streamlined delivery to the slowest committed project.

## 3. Coal closure rules: fixed dates or replacement-conditional

Two ways governments have handled closures:

- **Fixed date with long notice (Victoria).** Yallourn: closure in mid-2028 set in a structured transition agreement
  with the Victorian Government (March 2021), with a 350 MW, four-hour battery at Jeeralang by 2026; the government
  requires seven years' notice. Loy Yang A: a similar agreement for 2035 with a 12-year notice period and minimum
  availability until closure. The date holds whatever the replacement does.
- **Replacement-conditional extension (NSW, Eraring).** Origin announced closure in August 2025; in May 2024 the NSW
  Government agreed an extension to August 2027 as a bridge while renewables, storage and transmission are built,
  citing AEMO's reliability gap. Origin opts in year by year; the state covers operating losses up to $225 m a year
  and takes a share of profits, and Origin must aim to generate at least 6 TWh a year. In January 2026 Origin moved
  closure to April 2029 because replacement capacity was still late (no state support after June 2027).

In the model, the fixed-date rule is what the base case does now (closure at `expected_closure_date` +
`generatorRetirement`). The Eraring rule would be: a coal unit due to close stays open while the capacity planned to
replace it has not arrived, up to a maximum extension `E`. With the commissioning delay this reduces to closure at the
scheduled date plus `min(d, E)`, when the planned replacement is the capacity path, so it can be built as a switch
and run with and without (with `E` = 2 years as in Eraring's first extension, or 4 as in its second).

Considerations (for the decision on whether to include it, and how to read the results):

- What it protects: reliability. In the model the signal is unmet demand (hours, MWh) and the shortfall price paid
  for imports; the rule should remove most of the unmet demand that a delay creates at a closure.
- What it costs: emissions (brown coal stays longer), and in the market it holds prices down while the coal runs,
  which in reality weakens the revenue case for the replacement projects; gr4sp builds capacity exogenously, so this
  feedback is not in the model and should be stated as a limit.
- Not in the model: the underwriting cost to the state, the investor-confidence effect (developers and the CEIG said
  the extension dents confidence), and whether an ageing unit can run longer (AEMO notes some cannot; the model's
  breakdown rates are not age-dependent).
- Victoria's fixed dates were set with the replacement in mind (battery obligation, long notice); with `d > 0` and
  fixed dates the model shows the disorderly case the thesis warned about, and the rule shows the orderly
  alternative. Comparing the two over a grid of `d` and `generatorRetirement` is the experiment.

## 4. First runs (13 Nov 2026, seed 42, base case at `314161f` plus `d`)

| `d` (years) | wind 2022 (TWh) | wind 2030 | renewable share 2030 | coal 2020-35 (TWh) | wholesale 2021 ($/MWh) | wholesale mean 2020-35 | GHG per household 2020-35 (t/yr) | unmet hours 2020-45 |
|---|---|---|---|---|---|---|---|---|
| -1 | 11.2 | 17.0 | 55 % | 346 | 9.7 | 11.5 | 2.86 | 428 |
| 0 (base) | 11.3 | 17.1 | 56 % | 350 | 9.6 | 11.4 | 2.89 | 593 |
| 1.5 | 11.2 | 16.5 | 54 % | 362 | 23.1 | 13.2 | 3.01 | 924 |
| 3 | 4.7 | 14.9 | 50 % | 375 | 25.8 | 15.3 | 3.13 | 1,029 |

Years to 2018 are identical for every `d`. A delay shows up as later wind (the 2020-22 committed wind farms, then the
path's FY2026-FY2030 steps), more brown coal generation (+26 TWh, 7 %, over 2020-35 at `d` = 3), higher emissions
and higher prices while the wind is late (2020-23). It does not create unmet demand before 2040: with the 2019
closure dates (Yallourn 2029-32, Loy Yang 2047-48) coal is still there to fill the gap. Unmet demand grows after 2040
(the path's late steps arrive later); the break of 2048-50, when the last coal closes beyond the ISP 2020 horizon
(FY2042), is the same at every `d` (known, 12 Nov forward-period diagnosis) and is left out of the table.

So with the 2019 closure dates there is little for a replacement-conditional rule to do; the coordination question
bites when closures come earlier: the announced dates (Yallourn mid-2028, Loy Yang A 2035) or `generatorRetirement`
< 0. The forward-period wholesale prices (under 10 $/MWh in 2021-30 at `d` = 0) are the base case's own and are
discussed in the forward-period notes; this table compares delays, not levels.

The first implementation sized each capacity-path step on its delayed date, when more existing wind farms had
retired, so a delay built more capacity late in the run (2040-45). Steps are now sized on their planned dates (every
unit carries a planned start, `Generator.getPlannedStart`) and only their arrival moves; the test checks that a
step's size does not change with `d`.
