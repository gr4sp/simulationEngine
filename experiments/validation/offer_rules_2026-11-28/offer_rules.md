# Offer rules by generator, base case, seed 1 (R6 question of 28 Nov 2026)

Written by `experiments/validation/offer_rules.py analyse experiments\validation\offer_rules_2026-11-28 1`.

## Output-only check

| run | file | identical to `experiments\validation\ensemble_2026-11-24\run_s1\csv\VIC` |
|---|---|---|
| off_s1 | VICSimDataMonthlyPrices_seed_1.csv | True |
| off_s1 | VICSimDataMonthlySummary_seed_1.csv | True |
| off_s1 | VICSimDataYearSummary_seed_1.csv | True |
| log_s1 | VICSimDataMonthlyPrices_seed_1.csv | True |
| log_s1 | VICSimDataMonthlySummary_seed_1.csv | True |
| log_s1 | VICSimDataYearSummary_seed_1.csv | True |

## Off-market generators (market = off), by fuel and technology

Count and capacity: units active in December of the year. Energy: share of the year's generation of every active generator in the log (market and off-market, rooftop PV included).

| 1999: fuel / tech | units (count) | capacity (MW) | energy (% of all generation) |
|---|---|---|---|
| Gas Pipeline / Turbine - OCGT | 1.00 | 46.40 | 0.00 |
| Landfill Methane / Landfill Gas / Reciprocating Engine - Spark ignition | 1.00 | 6.18 | 0.04 |
| Water / Hydro - Dam | 6.00 | 86.50 | 0.34 |
| Water / Hydro - Gravity | 4.00 | 12.92 | 0.05 |
| Water / Hydro - Run of River | 1.00 | 4.50 | 0.02 |
| total | 13.00 | 156.50 | 0.45 |

| 2009: fuel / tech | units (count) | capacity (MW) | energy (% of all generation) |
|---|---|---|---|
| Biogas / Reciprocating Engine - Spark ignition | 2.00 | 2.20 | 0.02 |
| Diesel / Reciprocating Engine - Compression ignition | 2.00 | 4.28 | 0.00 |
| Gas Pipeline / Reciprocating Engine - Compression ignition | 2.00 | 2.39 | 0.00 |
| Gas Pipeline / Reciprocating Engine - Spark ignition | 2.00 | 1.50 | 0.01 |
| Gas Pipeline / Turbine - OCGT | 2.00 | 46.40 | 0.00 |
| Landfill Gas / Reciprocating Engine - Spark ignition | 6.00 | 20.03 | 0.21 |
| Landfill Methane / Landfill Gas / Reciprocating Engine - Spark ignition | 9.00 | 23.71 | 0.23 |
| Landfill Methane / Landfill Gas / Reciprocrating engine - Spark ignition | 1.00 | 1.06 | 0.01 |
| Municipal Solid Waste / Reciprocating Engine - Spark ignition | 1.00 | 0.78 | 0.01 |
| Solar / Solar - Rooftop | 35.00 | 12.55 | 0.02 |
| Solar / Solar PV - Fixed | 2.00 | 0.54 | 0.00 |
| Water / Hydro - Dam | 11.00 | 110.35 | 0.34 |
| Water / Hydro - Gravity | 4.00 | 12.92 | 0.04 |
| Water / Hydro - Run of River | 2.00 | 6.30 | 0.02 |
| Wind / Wind Turbine - Onshore | 4.00 | 73.70 | 0.31 |
| total | 85.00 | 318.70 | 1.22 |

| 2019: fuel / tech | units (count) | capacity (MW) | energy (% of all generation) |
|---|---|---|---|
| Bagasse / Turbine - Steam Sub Critical | 1.00 | 3.10 | 0.03 |
| Biogas / Reciprocating Engine - Spark ignition | 2.00 | 2.20 | 0.03 |
| Diesel / Reciprocating Engine - Compression ignition | 2.00 | 4.28 | 0.00 |
| Gas Pipeline / Reciprocating Engine - Compression ignition | 3.00 | 2.77 | 0.01 |
| Gas Pipeline / Reciprocating Engine - Spark ignition | 2.00 | 1.50 | 0.01 |
| Gas Pipeline / Turbine - CCGT | 1.00 | 21.00 | 0.00 |
| Gas Pipeline / Turbine - OCGT | 2.00 | 46.40 | 0.00 |
| Landfill Gas / Reciprocating Engine - Spark ignition | 6.00 | 20.03 | 0.24 |
| Landfill Methane / Landfill Gas / Reciprocating Engine - Spark ignition | 9.00 | 23.71 | 0.27 |
| Landfill Methane / Landfill Gas / Reciprocrating engine - Spark ignition | 1.00 | 1.06 | 0.01 |
| Municipal Solid Waste / Reciprocating Engine - Spark ignition | 1.00 | 0.78 | 0.01 |
| Solar / Solar - Rooftop | 156.00 | 2024.74 | 4.56 |
| Solar / Solar PV - Fixed | 216.00 | 106.78 | 0.22 |
| Solar / Solar PV - Single axis tracking | 3.00 | 20.38 | 0.04 |
| Water / Hydro - Dam | 15.00 | 111.04 | 0.40 |
| Water / Hydro - Gravity | 4.00 | 12.92 | 0.05 |
| Water / Hydro - Run of River | 2.00 | 6.30 | 0.02 |
| Wind / Wind Turbine - Onshore | 12.00 | 173.55 | 0.94 |
| total | 438.00 | 2582.54 | 6.83 |

Market units (primary or secondary) whose fuel contains Biogas, Diesel, Dry wood, Landfill, 1999-2019: 0 (none).

Market units by fuel, technology and dispatch type, 1999-2019 (distinct names):

| fuel / tech / dispatch | units (count) | max capacity (MW) |
|---|---|---|
| Battery / Storage - Battery / S | 1.0 | 30.0 |
| Brown Coal / Turbine - Steam Sub Critical / S | 12.0 | 560.0 |
| Gas Pipeline / Turbine - OCGT / S | 8.0 | 500.0 |
| Solar / Solar PV - Fixed / SS | 1.0 | 87.5 |
| Solar / Solar PV - Single axis tracking / SS | 5.0 | 107.5 |
| Water / Hydro - Dam / S | 4.0 | 185.0 |
| Water / Hydro - Gravity / S | 4.0 | 950.0 |
| Wind / Wind Turbine - Onshore / SS | 13.0 | 420.0 |


## Offer rules of market units, before 1999 (spin-up), seed 1

| fuel (half-hours offered) | arbitrage | srmc | shortRunCost | waterValue | storageMarkup | lcoe |
|---|---|---|---|---|---|---|

No market unit offered under the LCOE rule in this period.


## Offer rules of market units, 1999-2019, seed 1

| fuel (half-hours offered) | arbitrage | srmc | shortRunCost | waterValue | storageMarkup | lcoe |
|---|---|---|---|---|---|---|
| Battery | 819 | 0 | 0 | 0 | 0 | 0 |
| Brown Coal | 0 | 0 | 6120469 | 0 | 0 | 0 |
| Gas Pipeline | 0 | 0 | 4839209 | 0 | 0 | 0 |
| Solar | 0 | 51911 | 0 | 0 | 0 | 0 |
| Water | 0 | 0 | 0 | 2699675 | 0 | 8840 |
| Wind | 0 | 1908470 | 0 | 0 | 0 | 0 |

| market units on the LCOE rule | months (count) | first | last | half-hours |
|---|---|---|---|---|
| Dartmouth (Water) | 1 | 1999-01 | 1999-01 | 1473 |
| Eildon (Water) | 1 | 1999-01 | 1999-01 | 2949 |
| McKay Creek Power Station (Water) | 1 | 1999-01 | 1999-01 | 1442 |
| West Kiewa (Water) | 1 | 1999-01 | 1999-01 | 2976 |

Months with any LCOE offer: 1 (1999-01).


## Offer rules of market units, 2020-2050 (VICfuture.yaml), seed 1

| fuel (half-hours offered) | arbitrage | srmc | shortRunCost | waterValue | storageMarkup | lcoe |
|---|---|---|---|---|---|---|
| Battery | 598568 | 0 | 0 | 0 | 0 | 0 |
| Brown Coal | 0 | 0 | 3945940 | 0 | 0 | 0 |
| Gas Pipeline | 0 | 0 | 8931152 | 0 | 0 | 0 |
| Solar | 0 | 4566249 | 0 | 0 | 0 | 0 |
| Water | 0 | 0 | 0 | 116464 | 985734 | 0 |
| Wind | 0 | 15877318 | 0 | 0 | 0 | 0 |

No market unit offered under the LCOE rule in this period.

storageMarkup (hydro without a monthly budget): 363 months, 2020-10 to 2050-12, units Bogong / Mackay, Dartmouth, Eildon, Murray 1, Murray 2, West Kiewa.


## Off-market prices (LCOE rule), 2019, seed 1

Off-market solar (rooftop PV, small solar) enters the off-market price with its surplus, which is never set (Arena, off-market loop), so it carries no weight. The other off-market units carry their available capacity.

| 2019 off-market fuel (solar excluded) | energy (GWh) | LCOE price, energy weighted ($/MWh) | LCOE price min ($/MWh) | LCOE price max ($/MWh) | share of off-market energy (%) |
|---|---|---|---|---|---|
| Bagasse | 16.59 | 2.79 | 2.79 | 2.79 | 1.69 |
| Biogas | 12.49 | 4.61 | 4.61 | 4.61 | 1.28 |
| Diesel | 0.00 | nan | 13656.87 | 13656.87 | 0.00 |
| Gas Pipeline | 7.21 | 8.86 | 7.00 | 13656.87 | 0.74 |
| Landfill Gas | 117.37 | 2.55 | 2.08 | 4.19 | 11.98 |
| Landfill Methane / Landfill Gas | 136.14 | 2.72 | 1.78 | 5.33 | 13.90 |
| Municipal Solid Waste | 5.21 | 2.24 | 2.24 | 2.24 | 0.53 |
| Water | 228.21 | 17.58 | 17.58 | 17.58 | 23.30 |
| Wind | 456.09 | 34.09 | 22.44 | 53.67 | 46.57 |


## The LCOE rule's share of the tariff's wholesale term, 2019

All-markets price = (primary price x primary demand + off-market price x off-market demand) / (sum) (Arena.getWholesalePrice). The weight is recovered per month as (all-markets - primary) / (off-market - primary).

| 2019, monthly means, 100 seeds of ensemble_2026-11-24 | 5 % | median | 95 % |
|---|---|---|---|
| all-markets ($/MWh) | 61.40 | 71.65 | 92.64 |
| primary ($/MWh) | 62.26 | 72.74 | 94.18 |
| off-market price ($/MWh) | 20.83 | 20.83 | 20.83 |
| off-market weight (%) | 2.12 | 2.12 | 2.12 |
| all-markets - primary ($/MWh) | -1.54 | -1.08 | -0.86 |

By fuel, seed 1: contribution = weight x share of off-market energy x (fuel's LCOE price - primary price). Compare the sum with seed 1's all-markets - primary, -0.86 $/MWh, and its off-market price, 20.83 $/MWh (the split uses energy shares; the model weights each half-hour by available capacity).

| seed 1, 2019 (weight 2.12 %, primary 62.04 $/MWh) | share of off-market energy (%) | LCOE price ($/MWh) | contribution to all-markets - primary ($/MWh) |
|---|---|---|---|
| Bagasse | 1.69 | 2.79 | -0.02 |
| Biogas | 1.28 | 4.61 | -0.02 |
| Diesel | 0.00 | nan | 0.00 |
| Gas Pipeline | 0.74 | 8.86 | -0.01 |
| Landfill Gas | 11.98 | 2.55 | -0.15 |
| Landfill Methane / Landfill Gas | 13.90 | 2.72 | -0.18 |
| Municipal Solid Waste | 0.53 | 2.24 | -0.01 |
| Water | 23.30 | 17.58 | -0.22 |
| Wind | 46.57 | 34.09 | -0.28 |
| sum | 100.00 | 20.84 | -0.87 |

