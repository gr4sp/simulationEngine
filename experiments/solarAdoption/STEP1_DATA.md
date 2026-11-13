# Solar adoption, step 1: data (13 Nov 2026)

All sources were published by 2020 (the information set of the ERSS article), except the census of 2021, which the
EVA already uses for its projection after 2016.

| Output | What | Built by | Source |
|---|---|---|---|
| `data/tenure_VIC.csv` | Households by census (2006, 2011, 2016, 2021) x dwelling (the EVA's four + OTHER) x HIED bracket x household type, split into renting / mortgage / outright, and the bracket's income quintile | `build_tenure_census.py DATA_DIR` | ABS TableBuilder rent and mortgage wafers already used by the EVA (`build_ahc_census.py`) |
| `data/hecs2012_targets.csv` | Share of households with grid PV by tenure, economic resources, Melbourne / rest of Victoria; mean weekly electricity use by fuel combination, area, family composition; feed-in kWh | `build_hecs2012_targets.py XLS` | ABS 4670.0 Household Energy Consumption Survey 2012, additional tables |
| `simulationSettings/residential_electricity_VIC.csv` | Residential electricity, residential solar, final electricity use and the residential share, FY1974-FY2019 | `scripts/data/residential_electricity_vic.py` | Australian Energy Statistics 2020, Table F3 |
| `simulationSettings/residential_dnsp_VIC.csv` | Residential energy delivered, customers, network revenue and solar exports, by distribution network, 2006-2019 (calendar years) | `scripts/data/fetch_aer_dnsp_rin.py DIR OUT` | AER economic benchmarking RIN responses of the five Victorian DNSPs |
| `simulationSettings/rooftop_pv_economics_VIC.csv` | System prices 1997-2019, federal rebate 2000-2009 with the 2008 means test, certificates (Solar Credits multiplier, deeming, price), Victorian feed-in tariffs 2009-2019, Victorian Solar Homes rebate 2018-19 | by hand, one row per change with its source and status | IEA PVPS national reports, IEA policies database, ESC decisions |

The recorded APVI installations (`solar_installation_monthly`, 2007-2019, already in the database) are the
calibration target, and the CER postcode data (`scripts/data/fetch_cer_solar.py`, 2011 onwards) can serve the
geographic validation later.

## What the data say

**Tenure (census).** Renting rose from 24.9 % of Victorian households (2006) to 29.1 % (2016); mortgage 35-36 %;
outright (and other tenures) 39.4 % -> 35.6 %. Renting is 16-20 % in detached houses, 41-45 % in semi-detached and
60-70 % in flats.

**Who had PV in 2012 (HECS).** Australia: 13.0 % of outright owners, 10.9 % of mortgagors, 0.95 % of renters
(8.4 % of all households); 4.8 % of low-income, 1.6 % of low-wealth households. Victoria: 5.4 % in Melbourne,
7.1 % in the rest of the state (5.9 % overall, about 128,000 households). APVI's cumulative count by mid-2012 is
about 140,000 installations, households and small businesses together, so the two agree.

**Prices and support.** System prices fell from 12 AUD/W (2008) to 3.9 (2011) and 1.6 (2019), before incentives.
The federal rebate was 5 AUD/W to 7,500 AUD (2000-2003), 4 AUD/W to 4,000 AUD (2003-2007), 8 AUD/W for the first
1 kW (from July 2007), means-tested at 100,000 AUD household income from May 2008, and closed in June 2009. Solar
Credits then multiplied the certificates of the first 1.5 kW by 5 (June 2009), 3 (July 2011), 2 (July 2012), 1
(January 2013). Victorian feed-in tariffs: premium 60 c/kWh (Nov 2009 - 2011, paid to 2024), transitional 25 c
(2012), minimum 8 c (from Oct 2012), 6.2 c (2015), 5 c (2016), 11.3 c (July 2017), 9.9 c (July 2018), 12 c
(July 2019). The certificate value computed from the register (deeming x zone rating x price) is about 0.68 AUD/W
in 2016 and 0.54 in 2019; the national reports give 0.60-0.70 and 0.41-0.57, so the register is consistent.

## Finding: the model's household electricity use is 16-56 % too high

`LoadData` (lines 1218-1237) sets each household's monthly use to `domesticConsumptionPercentage` (0.3) x
`total_consumption_gwh` / households. `total_consumption_gwh` (`generation_consumption_historic`) is generation
minus exports plus imports, including rooftop PV and power stations' own use (`scripts/data/fetch_openelectricity.py`,
`build_rows`), 48-57 TWh a year, while the electricity used by final consumers in Victoria (AES Table F3, all sectors
except electricity supply) was 38-46 TWh. 30 % of the larger total is 12-17 TWh a year against a recorded
residential use of 10.2-12.0 TWh:

| FY | AES residential (TWh) | 0.3 x model total (TWh) | ratio | AES per household (MWh) | model per household (MWh) |
|---|---|---|---|---|---|
| 1999 | 10.64 | 12.36 | 1.16 | 6.44 | 7.48 |
| 2005 | 11.16 | 15.59 | 1.40 | 6.11 | 8.54 |
| 2010 | 11.78 | 17.06 | 1.45 | 5.94 | 8.60 |
| 2012 | 11.33 | 17.12 | 1.51 | 5.52 | 8.34 |
| 2014 | 10.17 | 15.84 | 1.56 | 4.76 | 7.41 |
| 2017 | 10.66 | 16.09 | 1.51 | 4.66 | 7.03 |
| 2019 | 10.86 | 14.43 | 1.33 | 4.41 | 5.86 |

The HECS 2012 confirms the recorded level independently: 104.2 kWh a week in Melbourne and 115.7 in the rest of
Victoria (5.4 and 6.0 MWh a year), against 5.5 MWh from AES and 8.3 MWh in the model for 2011-12. The residential
share of final use moved between 24.6 % (2014) and 28.2 % (2003).

What it affects: the household consumption and emissions outputs (`EndUserUnit.currentConsumption`,
`currentEmissions`, so the "GHG per household" indicator), not the wholesale market (the market demand is the
half-hourly series) and not the EVA (which uses its own profile consumption). Step 2 replaces the constant with
per-household use by profile scaled to the AES residential series, and reports the change in the outputs.

Caveat: AES "Residential, Electricity" is electricity bought from the grid; rooftop output used in the home is in
"Residential, Solar energy" together with solar hot water (1.0 TWh in 2018-19). Adding all of it does not close the
gap (ratio 1.22 in 2019).

**Third source: the distribution networks (AER RIN, added 13 Nov).** Residential energy delivered by the five
Victorian networks is 11.5-12.7 TWh a year (2006-2019). That is 0-13 % above AES (calendar year against the mean of
the two financial years). Per residential customer it is 5.66 MWh in 2006, 5.16 in 2012 and 4.61 in 2019. Network
customers (2.35 M in 2012) outnumber census households (about 2.0 M in 2011), because the networks also count
unoccupied dwellings and holiday homes. So the network figure per *household* is higher: about 6.0 MWh in 2012. In
2012 the three sources give 5.2-6.0 MWh per household, and the model gives 8.3 MWh. The finding stands. The network
data also give the step 2 split by area: Jemena 4.2-4.3, CitiPower 4.4-4.6, United Energy 4.5-4.7, Powercor 5.0-5.1
and AusNet 5.0-5.2 MWh per customer (2014-19).

## Finding 2: rooftop PV is taken out of the wholesale demand twice at midday

The script is `midday_double_count.py`, and its output is `data/midday_double_count.csv`. The wholesale demand is
built in three steps:
1. The input is AEMO's VIC1 total demand. Rooftop PV sits behind the meter, so that series is already net of rooftop
   output, midday dip included.
2. The base case adds a flat monthly uplift (`shapeDemandUplift: true`) so each month sums to
   `total_consumption_gwh`. That total includes the recorded rooftop generation, so the rooftop energy comes back in,
   spread evenly over every half-hour.
3. `Arena` subtracts the model's rooftop output in each half-hour (lines 559-599).

The error at each half-hour is the month's mean rooftop output minus the output at that half-hour. Wholesale demand
is too low at midday and too high at night. The monthly energy is right: the model's rooftop energy matches the
recorded series within 5 %.

| Year | Rooftop MW (end) | Midday 10-15 h, mean error (MW) | Same, summer | Worst half-hour (MW) | Evening 17-21 h (MW) | Minimum demand, AEMO vs model (MW) | Half-hours with a dip > 5 % |
|---|---|---|---|---|---|---|---|
| 2012 | 430 | -71 (-1.2 %) | -90 | -258 | +38 | 3,612 vs 3,682 | 0.1 % |
| 2015 | 880 | -192 (-3.5 %) | -263 | -712 | +94 | 3,369 vs 3,032 | 9.6 % |
| 2017 | 1,237 | -277 (-5.4 %) | -349 | -996 | +124 | 2,967 vs 2,484 | 15.7 % |
| 2019 | 1,831 | -403 (-8.2 %) | -557 | -1,472 | +215 | 3,058 vs 2,116 | 19.9 % |

Before 2012 the error is negligible, and from 2015 it is material. The model under-states midday demand, which
pushes down midday prices and the coal output that midday demand would need. It over-states evening demand. The
summary indicators most exposed are minimum demand, the price spread within the day, the renewable share and the
value of rooftop exports. The value of exports matters directly for the adoption payback in step 3.

Two checks were made:
- `solar_ghi` holds identical copies of most half-hours. The model keeps one value per timestamp, and so does the
  script. Without that de-duplication, the first pass over-stated rooftop energy 2.7 times.
- The reference series is AEMO's demand with the rooftop energy left out of the uplift.

**Applied 13 Nov 2026** (switches `solarShapeInDemand` and `forecastFromCalibratedDemand`, both on in `VIC.yaml`).
Before the monthly calibration, each historic half-hour gets back the output of the off-market solar fleet as `Arena`
computes it. Rooftop PV is rebuilt from the installation register, the way `SimPolicies` creates it, and small solar
farms come from the SPMs. Check, seed 42, 2019: model market demand minus AEMO demand minus the net-export shape now
varies by 13 MW across the day (782 MW before; midday −231 vs night +450 MW before, +219 both now). Minimum demand is
3,210 MW (AEMO 3,058; 2,255 before). The `solar_ghi` midnight rows are cleaned first (audit F2). The forward period
had the same problem and more: it grew AEMO's operational demand, which lacks rooftop, non-scheduled output and
exports, so 2021 demand was 10 % low. It now grows the calibrated base year.

Original proposal: build underlying demand by adding back the *recorded* rooftop fleet's output at its own
half-hourly shape. That fleet is the APVI capacity x GHI x efficiency. Calibrate only the remaining gap flat, and
leave the Arena subtracting the *simulated* fleet. With recorded adoption the two cancel exactly. With endogenous
adoption (step 3) the difference is the effect of the simulated adoption, which is what the experiment needs. The
repair changes base-case results from about 2013, so it comes with a baseline regeneration.

## Definitions

From the literature (ABS 4670.0, HECS 2012, Table 7A footnotes; the survey ran on the Survey of Income and
Housing 2011-12 sample, so it uses that survey's concepts):

- **Low income:** households in the lowest quintile of *equivalised disposable household income, including
  imputed rent*. Equivalised means adjusted for household size and composition (ABS: modified OECD scale). Imputed
  rent is the rent owner-occupiers would pay for their own home, net of their housing costs.
- **Low wealth:** households in the lowest quintile of *equivalised household net worth* (assets minus
  liabilities, including the home).
- **Low economic resources:** households in the lowest two quintiles of *both* income and net worth (ABS concept
  for households with little income and little to fall back on). PV share in 2012: 2.7 %, against 4.8 % for low
  income and 1.6 % for low wealth.

From this work:

- **Tenure** (`tenure_VIC.csv`): renting = households in the census rent table with a stated, nil or unstated rent;
  mortgage = the same in the mortgage table; outright = the rest (owned outright, other tenures, not stated).
- **Income quintile** (D1 cohorts): the quintile of *gross* household income (census HIED brackets) that holds the
  bracket's middle household. It is not the ABS low-income measure (not equivalised, no imputed rent), so the
  model's quintile 1 cannot be compared with the HECS "low income" share as is. Before validation (step 4) either
  equivalise the census cells with the household type (it gives the household size) or state the comparison as
  approximate. The model has no wealth; tenure stands in for it (renters and low wealth overlap most).
- **Self-consumption share:** the share of a system's annual output used in the home rather than exported.

## Open items (none blocks step 2)

- Tenure before 2006: the 2006 shares are held back to 1999 (adoption before 2007 is negligible: rebate-funded,
  under 1,000 Victorian systems a year).
- Values marked `secondary` or `assumed` in the economics register: the 2015 and 2016 minimum feed-in tariffs, the
  Solar Credits dates (government announcements via pv magazine), the Solar Homes cap and income limit of 2018, the
  zone 4 rating and the certificate price before 2011. They are varied in the sensitivity runs.
- Exports before November 2009: credited at the retail rate (net metering where retailers offered it), an assumption.
- Self-consumption shares by household type (D4): to set from the literature in step 3.
