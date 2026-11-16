# Solar adoption, step 3: cohort agents and the adoption rule (14 Nov 2026)

Design sections 2-3, decisions D1 (720 cohorts) and D4 (self-consumption fixed from the literature). Switch
`rooftopAdoption: recorded | endogenous` (`VIC.yaml`); `recorded` stays the base case until the step 4 calibration is
accepted, and with it every output is byte-identical to the baseline (46 tests, regression 1998-2030).

## What was built

| Piece | File | What |
|---|---|---|
| Cohort table | `simulationSettings/household_cohorts_VIC.csv`, built by `build_cohorts.py` | Share of Victorian households in each year 1999-2021 for each of the 720 cohorts (household type 6 x dwelling 4 x main heating fuel 2 x tenure 3 x income quintile 5), and the household income bounds of the cohort's quintile |
| Self-consumption | `simulationSettings/rooftop_self_consumption_VIC.csv` | Self-consumed share of a system's output by household type and date (one Victorian value for now, see Open items) |
| Adoption model | `src/core/Social/RooftopAdoption.java` | Cohort counts, monthly payback and hazard, monthly installations and kW for `SimPolicies`, yearly report |
| Wiring | `SimPolicies`, `Gr4spSim`, `SaveData`, `Settings` | Endogenous mode: `SimPolicies` steps the adoption model, then builds the month's rooftop generators from its kW instead of the register |
| Output | `<yaml>RooftopAdoption<seed>.csv` (endogenous runs only) | Per year and cohort: households, adopters, adopters' kW, installations in the year, December payback |

## Definitions (new terms)

- **Cohort:** the households of one combination of household type, dwelling, main heating fuel, tenure and income
  quintile. The model keeps a count of adopters per cohort, not individual households.
- **Income quintile (corrected):** the quintile of *equivalised* gross household income. The census variable HIED is
  equivalised (modified OECD scale), so `STEP1_DATA.md`'s statement "not equivalised" was wrong and is corrected there.
  Households are spread evenly within each census bracket and a bracket that straddles a quintile boundary is split,
  so each quintile holds 20 % of households (step 1's `quintile` column put whole brackets in one quintile, 11-35 %).
  The quintile is still not the ABS low-income measure, which uses disposable income and imputed rent.
- **Household income bounds:** the quintile's equivalised bounds times the cohort's mean equivalence factor (eA, the
  EVA's `ahc_equivalence_VIC.csv`): the household income the rebates' means tests apply to.
- **Simple payback (PB):** net cost of a system of the month's mean size divided by its first-year saving, in current
  dollars.
- **Hazard:** the share of a cohort's non-adopters that installs in the month,
  h_c = e_c [a + b A_c] / (1 + exp(k (PB_c - PB0))), with A_c the cohort's share already adopted (imitation, as in the
  Bass model), a the innovation rate, k and PB0 the slope and midpoint of the willingness to install, and e_c the
  product of the barrier factors for renters, flats and income quintile 1.

## How the payback is computed (each month, per cohort)

- Size: the register's mean system size of the month (`system_capacity`; earlier months take the nearest earlier one).
- Gross cost: system price (AUD/W, IEA PVPS) x size. The register's prices are taken as the household's price (the
  2008-2019 series is quoted without GST; a constant factor is absorbed by PB0 in the calibration).
- Federal rebate: rate x size up to the cap; from July 2007 the first kW only; means test from May 2008: times the
  cohort's share below 100,000 AUD (household income, uniform within its bounds).
- Certificates: floor(deeming years x zone rating x (multiplier x first 1.5 kW + the rest)) x certificate price;
  RECs from April 2001, Solar Credits multiplier from June 2009. A household before June 2009 gets both the rebate and
  the certificates.
- Victorian Solar Homes rebate (from August 2018): owner-occupiers (not renters) under the income limit; 50 % of cost
  up to the cap.
- Saving: output x self-consumed share x tariff + output x (1 - share) x feed-in tariff of the installation date
  ("retail" before November 2009: the tariff, A33). Output = size x the model's yield: mean annual irradiation on the
  rooftop plane over the complete years of the irradiance record x the rooftop efficiency, so it matches the rooftop
  generators. The tariff is the model's household tariff of the previous month, converted from real January-2019 to
  current c/kWh with the CPI factor.

Hand check (seed 42, December 2012): 3.52 kW x 3 AUD/W = 10,560 AUD; 89 certificates x 40 AUD = 3,560 AUD; net 7,000
AUD. Tariff 30.67 c real / 1.14 = 26.9 c current; share 0.67; feed-in tariff 8 c. The model's payback of 8.03 years
implies a yield of 1,198 kWh/kW, close to the recorded fleet's 1,167 kWh/kW in FY2011-12.

## The cohort table

Tenure shares match step 1 (renting 24.8 % in 2006 to 28.7 % in 2019); gas heating 71.5 % (1999) to 64.3 % (2014,
held after). The census rows (stated incomes, four dwelling types) hold 1.59 M households in 2006 and 2.23 M in 2021,
85-86 % of the model's count (`domestic_consumers`: 1.87 M and 2.58 M); the shares assume the rest are spread like
them. Household income quintile bounds, 2011: 0-30, 33-54, 57-84, 84-123, 115-345 k AUD (cohort-weighted means; they
overlap because household sizes differ).

## First endogenous run (seed 42, to 2021, initial parameters, not calibrated)

| Year | Mean payback (yr) | Installations, model | Installations, register | Adopters, % of households |
|---|---|---|---|---|
| 2003 | 27.2 | 0 | | 0.0 |
| 2009 | 14.8 | 2,052 | 8,433 | 0.1 |
| 2011 | 5.3 | 11,301 | 60,256 | 0.9 |
| 2012 | 8.0 | 12,600 | 66,239 | 1.5 |
| 2015 | 6.9 | 20,428 | 31,382 | 3.1 |
| 2018 | 3.8 | 52,114 | 47,244 | 6.9 |
| 2019 | 3.5 | 68,578 | 61,784 | 9.4 |

The payback follows the policy history: 27-56 years in 2000-2006, 15 years in December 2009 (Solar Credits and the
premium feed-in tariff, at 9 AUD/W), 5 years in 2011 (premium feed-in tariff, 3.9 AUD/W), 8 years once it closed
(2012), under 4 years from 2018 as prices fell and the minimum feed-in tariff rose. With the initial parameters the 2010-12 boom is under-produced and
2018-19 over-produced; step 4 calibrates a, b, k, PB0 and the barrier factors. Who adopts (2012): 2.1 % of mortgage
holders, 2.0 % of outright owners, 0.1 % of renters; quintile 1 0.6 %, quintiles 2-5 1.5-1.9 %; flats under 0.1 %.

Feedback: with less rooftop output than recorded, wholesale prices are higher (2018: 88.0 against 69.8 $/MWh), and
the runs are identical to the recorded run until January 2007, when the recorded fleet starts. The N21 add-back keeps
the recorded fleet, so the difference is the effect of the simulated adoption.

## Open items

1. **D4, self-consumption by household type.** No primary source with shares by household type was found. The file
   holds one Victorian value that falls as systems grow, from two Victorian anchors: 0.67 in 2011-12 (HECS 2012 exports
   of 14.8 kWh/week per PV household against 45.4 kWh/week of output from a 2.03 kW mean system at 1,167 kWh/kW) and
   about 0.45 in 2017-19 (AER RIN residential exports, 48-50 % of all rooftop output; the household share depends on
   the unknown non-household part, 0.37-0.52). Status `constructed`. Options: keep one value (falling with size), or
   look further for type differences (Griffith 2021: hardship customers self-consume 36 % against 26 % for others,
   a later study, retailer data). For Angela.
2. **Non-household systems.** The register counts small businesses too; `rooftopNonResidentialShare` (0 for now) adds
   that share of the recorded kW. HECS 2012 counts about 128,000 Victorian households with PV against about 143,000
   systems in the register by mid-2012 (a non-household share near 10 % by count). To set before the calibration.
3. **N30 (found here): the household count jumps 7 % in 2015.** From 2016 the model's household register takes
   `household_forecast_victoria` (2.394 M in 2016) instead of the recorded `domestic_consumers` (2.242 M), and the
   monthly interpolation spreads the jump over 2015. It changes step 2's use per household in 2015-2019 and, in
   endogenous mode, the cohort sizes. Not repaired here.
4. **Hazard parameters** are initial guesses (VIC.yaml), not literature values; step 4 calibrates them.
5. **Not yet:** adopters' generators in their own DER SPM (2), per-cohort use and bills (step 6), forward adoption
   after 2019 (policy-mixes article; the economics register ends in 2019 and its last values are held).

## 15 Nov 2026: follow-up on the open items

**D4, Victorian consumption benchmarks** (saved in `Sandbox/papers/GR4SPArticle_ERSS/data/consumption_benchmarks/`):
- AER, *Residential energy consumption benchmarks* (Frontier Economics, 9 Dec 2020; Energy Made Easy): annual grid use
  by household size (1-5+) and climate zone, Victoria included (1,380 households). Grid use only: "does not account
  for self-consumption of electricity generated by their solar PV".
- ACIL Allen for the ESC, *Victorian energy usage profiles* (1 Mar 2019; Victorian Energy Fact Sheet and Victorian
  Energy Compare): the Victorian part of the AER 2017 dataset (3,079 households, mostly 2016 meter data). Half-hourly
  profiles by household size (Figure 4.2) and with / without PV (Figure 4.8); daily use one person 7.5, 2-3 people
  12.1, 4+ people 16.1 kWh (Table 4.2); annual use with PV 4,671 and without 5,241 kWh (Table 3.5). The half-hourly
  values are in a spreadsheet tool delivered to the ESC, not found online.
- Leslie, Pourkhanali and Roger (Monash, 2021 working paper): Victorian zone-substation half-hourly data mapped to
  census areas; household load by age, household size, tenure and solar penetration (figures), with the cross-subsidy
  under flat tariffs. Relevant to step 6.
None reports self-consumption by household type. They give what is needed to compute it: a half-hourly load shape by
household size, against the model's own half-hourly rooftop output, self-consumed share = sum of min(load, PV) / sum
of PV, per household size and system size. The half-hourly values are needed (ESC spreadsheet, on request).

**Non-household share (A43), effect measured** (seed 42, endogenous, initial parameters; share 0 vs 0.1): 10 % of the
recorded rooftop output is added (2019: +219 GWh), the market energy falls by the same amount (45,809 vs 46,026 GWh),
prices fall (2018: 80.3 vs 88.0; 2019: 75.8 vs 78.9 $/MWh); household adoption falls 0.1 % (lower tariff, longer
payback). Recorded mode is unaffected. For the calibration the households' target becomes 90 % of the register.

**N30, household counts** (2016): census occupied private dwellings 2,112,699 (2011: 1,944,691; ABS 2016 Census
media release); `domestic_consumers` 2,242,283 (source not stated); `household_forecast_victoria` (VIF 2016)
2,394,020; network residential customers 2,504,213 (AER RIN, includes vacant dwellings and holiday homes).

## 16 Nov 2026: D4 self-consumption by household type, from the ESC usage profiles

The figures of ACIL Allen (2019) for the ESC are vector charts, so the plotted values were read exactly
(`extract_esc_profiles.py` -> `data/esc2019_profiles.csv`, re-plots `data/esc2019_fig*.png` for a visual check). Check
against the report's own numbers: one person 7.50 kWh/day (Table 4.2: 7.5); 2 and 3 people 11.57 and 13.19 (2-3 people
12.1; the text gives 1.6 kWh/day between them: 1.62); 4 and 5+ people 15.51 and 16.87 (4+: 16.1).

`self_consumption.py`: each half-hour the home uses its PV output up to its load; share = sum of min(load, PV) / sum of
PV over the year. Load = the profile of the type's mean size (census 2016: lone person 1.0, couple 2.1, group 2.5, other
family 2.4, one parent 2.9, couple with children 4.0 persons) scaled to the year's use per household and x 1.2 for
homes that adopt PV. PV = the model's own half-hourly rooftop output per kW (`tools/DumpRooftop.java`) x the year's mean
new system.

Why x 1.2: in Figure 4.8 homes with PV import about as much as homes without (12.3 against 12.2 kWh/day), and at night,
when import is the whole load, they import 1.20-1.24 times as much at 21:00-23:00 and 1.4-1.7 times in the early
morning (probably off-peak water heating). Homes that adopt use more (the report notes the same, footnote 9). Without
the factor the anchors are missed (0.59 vs 0.67 in 2011-12).

| Check | Computed (1.0 / 1.2 / 1.42) | Victorian anchor |
|---|---|---|
| FY2011-12 fleet, 2.03 kW | 0.59 / 0.66 / 0.73 | 0.67 (HECS 2012) |
| 2017 fleet, 3.64 kW | 0.33 / 0.38 / 0.43 | 0.37-0.52 (AER RIN) |
| 2019 fleet, 4.54 kW | 0.26 / 0.30 / 0.34 | 0.37-0.52 |

The RIN range's lower end assumes 25 % of rooftop output is not households'; a larger business share lowers it. Two
reasons the computed values are low rather than high: Figure 4.2 averages the whole sample, where PV homes are
over-represented (AER), so midday load is already net of some self-use; one daily shape all year misses winter (high
load, low PV). Real loads are spikier, which works the other way.

New systems by type (factor 1.2): 2012 (3.1 kW) lone person 0.36, couple 0.49, couple with children 0.60; 2019 (7.5 kW)
0.14, 0.20, 0.25. Main heating fuel moves the share by about 3 points (Figure 4.9, 4 kW, 2016: gas 0.33, all-electric
0.36), so it is left out. Endogenous run (seed 42, initial hazard parameters): paybacks now differ by type (2012: 8.6-9.6
years) and the lower self-consumption of large systems lengthens the 2015 payback to 10 years.

Other material in the report for later steps: Figure 4.9 and Table 3.5 (gas and all-electric homes: 4,432 vs 6,871
kWh/yr; profiles) for the EVA's fuel profiles and per-profile use (step 6); Table 4.3 gas use by household size (92,
159, 201 MJ/day) for the EVA's gas bills; Figure 4.1 (controlled load) for off-peak water heating. The VEMD workbook
(ESC Victorian Energy Market Dashboard, 2020-21 onwards: hardship, disconnections, best offers) is after the ERSS period;
it belongs to the policy-mixes article.

## 16 Nov 2026: N30 repaired (census household series)

`domestic_consumers` (source not stated) equals the census count of occupied private dwellings in 2006 (1,869,388 vs
1,869,384) and is within 0.6 % in 2001, then runs above it (2011 +4.4 %, 2016 +6.1 %, 2021 +8.1 %); from 2016 the model
took Victoria in Future (2.394 M, 13 % above the 2016 census), hence the 7 % jump in 2015. `householdsFile:
households_VIC.csv` (`scripts/data/households_census_vic.py`) puts the census counts (linear between census nights) on
1 January 2007-2021; earlier years keep `domestic_consumers`; later years keep Victoria in Future's growth, rebased on
the 2021 census (x 0.880). Prices, generation and every market column are identical; per-household outputs change
(seed 42):

| Year | Households (was) | Use per household, kWh (was) | GHG per household, t (was) |
|---|---|---|---|
| 2008 | 1.890 M (1.934) | 6,263 (6,097) | 7.73 (7.53) |
| 2012 | 1.958 M (2.073) | 5,683 (5,359) | 6.78 (6.40) |
| 2016 | 2.093 M (2.394) | 5,038 (4,397) | 5.54 (4.84) |
| 2019 | 2.246 M (2.565) | 4,823 (4,228) | 4.69 (4.12) |
| 2030 | 2.775 M (3.153) | 4,043 (3,557) | 2.44 (2.15) |

Checks: 2012, 5.68 MWh against HECS 2012 (5.4-6.0) and the networks (about 6.0 per household); 2016, 5.04 MWh against
the ESC sample's 4,984 kWh (ACIL Allen 2019, Table 3.5, mostly 2016 meter data). The census count is a lower bound on
households using electricity over a year (residents away on census night make a dwelling "unoccupied"); ERP-based
estimates such as Victoria in Future are higher for that reason and the census undercount.

## 16 Nov 2026: non-household systems from the size bands (A43)

APVI publishes monthly installations by size band (Australia, from the CER register, 2010 onwards; no Victorian split).
Systems of 14 kW and more, taken as non-household: 0.1-0.3 % of installations and 1.8-3.3 % of capacity in 2010-12,
3-5 % of installations and 15-24 % of capacity in 2014-19 (band middles; they match APVI's average size within 0-8 %).
So the constant 10 % (HECS 2012 households vs the register, by count) is too high before 2013 and too low by capacity
after 2014. The yearly share (`rooftop_nonresidential_share_VIC.csv`, `scripts/data/rooftop_nonresidential_share.py`)
replaces the constant in endogenous mode; in step 4 the households' calibration target is the register minus this
share. It is a lower bound for businesses (small businesses with small systems count as households). The HECS gap
(about 10 % by count in 2012) suggests more small business systems than the size rule finds, or survey error; to test
in step 4 as a sensitivity. Recorded mode unchanged (46 tests).
