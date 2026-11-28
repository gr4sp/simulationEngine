# R10: off-market units and demand (base case, seed 1, v0.3.3 code and settings)

Written by `experiments/validation/r10_offmarket.py experiments\validation\r10_offmarket_2026-11-28`. Sources in the script's docstring.

## 1. Demand basis of the non-solar off-market output

The half-hourly demand is AEMO's, scaled each month to `total_consumption_gwh` (OpenElectricity generation by fuel, solar and imports included; `LoadData` calibration, l.1088-1135) plus, since N28, the output of off-market units whose fuel is not in `demandRecordFuels` (Brown Coal; Water; Wind; Battery; Gas Pipeline Turbine - OCGT; Gas Pipeline Turbine - Steam Sub Critical). `Arena` then subtracts all off-market output. So off-market output is added back when its fuel is outside the record, and assumed to be in the record otherwise. The question is whether the record holds the off-market output of the recorded fuels (wind and water here).

| year | off-market non-solar (GWh) | added back by N28 (GWh) | fuel in the record, not added back (GWh) |   of which Gas Pipeline (GWh) |   of which Water (GWh) |   of which Wind (GWh) |
|---|---|---|---|---|---|---|
| 1999 | 199.4 | 17.3 | 182.1 | 0.0 | 182.1 | 0.0 |
| 2000 | 199.9 | 17.4 | 182.6 | 0.0 | 182.6 | 0.0 |
| 2001 | 307.9 | 90.1 | 217.8 | 0.0 | 189.7 | 28.0 |
| 2002 | 420.6 | 135.2 | 285.4 | 0.0 | 205.2 | 80.2 |
| 2003 | 460.1 | 151.9 | 308.2 | 0.0 | 205.2 | 103.0 |
| 2004 | 476.0 | 166.9 | 309.1 | 0.0 | 205.8 | 103.3 |
| 2005 | 493.6 | 180.9 | 312.7 | 0.0 | 207.0 | 105.7 |
| 2006 | 545.5 | 184.4 | 361.1 | 0.0 | 226.6 | 134.6 |
| 2007 | 595.0 | 233.8 | 361.1 | 0.0 | 226.6 | 134.6 |
| 2008 | 623.5 | 261.2 | 362.3 | 0.0 | 227.4 | 134.9 |
| 2009 | 681.3 | 275.2 | 406.1 | 0.0 | 227.0 | 179.1 |
| 2010 | 698.3 | 277.7 | 420.7 | 0.0 | 227.0 | 193.7 |
| 2011 | 703.8 | 277.7 | 426.1 | 0.0 | 227.0 | 199.1 |
| 2012 | 719.9 | 278.7 | 441.2 | 0.0 | 227.6 | 213.6 |
| 2013 | 761.1 | 278.4 | 482.7 | 0.0 | 227.0 | 255.7 |
| 2014 | 761.1 | 278.4 | 482.7 | 0.0 | 227.0 | 255.7 |
| 2015 | 774.7 | 278.4 | 496.3 | 0.0 | 227.0 | 269.3 |
| 2016 | 823.0 | 279.2 | 543.9 | 0.0 | 227.6 | 316.2 |
| 2017 | 835.7 | 278.4 | 557.3 | 0.0 | 227.0 | 330.3 |
| 2018 | 936.2 | 286.8 | 649.4 | 0.0 | 228.0 | 421.4 |
| 2019 | 979.3 | 295.0 | 684.3 | 0.0 | 228.2 | 456.1 |

Wind: the record against the model's market and off-market rows. The record (OpenElectricity) has no wind before 2008 (17 GWh in 2008), while the model has wind rows from 2001. In those years the monthly target lacks all wind, and the model's whole wind output (market rows dispatched, off-market rows subtracted) takes the place of recorded coal and gas.

| year | record wind (GWh) | model wind, market (GWh) | model wind, off-market (GWh) | model wind, total (GWh) | model wind - record wind (GWh) | model wind - record wind, share of record total (%) |
|---|---|---|---|---|---|---|
| 1999 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 |
| 2000 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 |
| 2001 | 0.00 | 0.00 | 28.04 | 28.04 | 28.04 | 0.06 |
| 2002 | 0.00 | 0.00 | 80.18 | 80.18 | 80.18 | 0.17 |
| 2003 | 0.00 | 137.97 | 103.02 | 240.99 | 240.99 | 0.48 |
| 2004 | 0.00 | 138.35 | 103.30 | 241.65 | 241.65 | 0.47 |
| 2005 | 0.00 | 137.97 | 105.70 | 243.67 | 243.67 | 0.45 |
| 2006 | 0.00 | 137.97 | 134.55 | 272.52 | 272.52 | 0.48 |
| 2007 | 0.00 | 216.81 | 134.55 | 351.36 | 351.36 | 0.62 |
| 2008 | 16.77 | 217.40 | 134.92 | 352.33 | 335.56 | 0.59 |
| 2009 | 764.90 | 552.42 | 179.10 | 731.52 | -33.38 | -0.06 |
| 2010 | 1097.24 | 662.26 | 193.68 | 855.94 | -241.30 | -0.42 |
| 2011 | 1215.09 | 892.60 | 199.11 | 1091.71 | -123.38 | -0.22 |
| 2012 | 1535.05 | 1087.64 | 213.58 | 1301.22 | -233.83 | -0.43 |
| 2013 | 2588.27 | 2123.63 | 255.70 | 2379.34 | -208.93 | -0.39 |
| 2014 | 2539.58 | 2361.87 | 255.70 | 2617.57 | 77.99 | 0.14 |
| 2015 | 3143.21 | 2900.91 | 269.25 | 3170.16 | 26.95 | 0.05 |
| 2016 | 3476.84 | 3185.09 | 316.23 | 3501.33 | 24.49 | 0.04 |
| 2017 | 3513.04 | 3610.58 | 330.31 | 3940.89 | 427.85 | 0.87 |
| 2018 | 4342.75 | 3948.09 | 421.41 | 4369.50 | 26.75 | 0.06 |
| 2019 | 5060.47 | 4400.06 | 456.09 | 4856.15 | -204.32 | -0.42 |

2001-2008: the model's wind with no counterpart in the record is 1794 GWh (28-351 GWh a year, 0.06-0.62 % of the record total). From 2009 the record holds wind, and whether it holds the off-market rows cannot be told from the monthly totals by fuel; the difference then mixes that with the model's wind error.

## 2. Scheduled and semi-scheduled rows off-market, 1999-2019

`Settings.isMarketPaticipant` compares each register row with `minCapMarketGen` (30 MW for scheduled and semi-scheduled). Station total: every Victorian row of the same DUID (or site name where the DUID is empty) in `generationassets`, all statuses.

| off-market S/SS row | DUID | fuel | dispatch | row capacity (MW) | first month | energy 1999-2019 (GWh) | rows of the station (count) | station capacity, all rows (MW) | station rows in the market, last month (MW) |
|---|---|---|---|---|---|---|---|---|---|
| Hume Dam VIC | HUMEV | Water | S | 29.0 | 1999-01 | 1067.7 | 1 | 29.0 | 0.0 |
| Waubra | WAUBRAWF | Wind | SS | 22.5 | 2009-04 | 636.2 | 3 | 192.0 | 169.5 |

Rows of this kind that start in 2020 or later (forecast only), from `generationassets`:

| row | duid | fuel_type | nameplate_capacity_mw | dispatch_type | unit_status | full_commercial_use_date |
|---|---|---|---|---|---|---|
| Inverleigh Solar Farm | nan | Solar | 22.0 | SS | Publically Announced | 2021-09-15 |
| Bulgana Green Power Hub - BESS | nan | Battery | 20.0 | S | Committed | 2020-02-08 |
| Gannawarra Energy Storage System | GANNBG1 | Battery | 25.3 | S | In Commissioning | 2020-01-01 |
| Loy Yang A Power Station | LYA2 | Brown Coal | 15.0 | S | Upgrade | 2020-05-12 |
| AusNet Services Deakin University Victoria Geelong PV Plant | nan | Solar | 7.2 | SS | Publically Announced | 2020-07-01 |
| Mildura Power Station | nan | Solar | 9.6 | SS | Publically Announced | 2021-04-01 |
| Inverliegh Wind Farm Storage | nan | Battery | 12.0 | SS | Publically Announced | 2024-01-01 |

## 3. Hume Dam VIC and Clover

| row | energy 1999-2019 (GWh) | energy 2010 (GWh) |
|---|---|---|
| Clover (13.0 MW, NS, off) | 478.608 | 22.776 |
| Clover (16.0 MW, NS, off) | 589.056 | 28.032 |
| Hume Dam VIC (29.0 MW, S, off) | 1067.664 | 50.808 |

An off-market unit runs at nameplate x its type's capacity factor in every half-hour, with no outages (`Generator.computeAvailableCapacity`, l.407-413; `Arena` off-market loop). Both stations are type Water: maxCapacityFactor 0.2, summer 0.2, and neither has a row in `generator_parameters_VIC.csv`. Hume is one 29 MW row; Clover is two rows of 16 and 13 MW, 29 MW together. Same capacity and same factor give the same energy: 29 MW x 0.20 x 8,760 h = 50.808 GWh a year (8,784 h in leap years).

## 4a. January 1999: hydro on the LCOE rule

Seed 1, price-setter log of 1999 (`pricesetter_s1/`, same run as the base case: its three CSVs are compared below). January 1999 has 1488 half-hours; the four hydro units on the LCOE rule set the primary price in 17.

| January 1999, primary market | half-hours | mean price ($/MWh) |
|---|---|---|
| all half-hours | 1488.00 | 9.55 |
| without those half-hours | 1471.00 | 8.86 |
| only those half-hours | 17.00 | 69.68 |

| price-setting fuel, January 1999 | half-hours setting the price |
|---|---|
| Brown Coal | 1367 |
| Gas Pipeline | 104 |
| Water | 17 |

The model's monthly price for January 1999 is 9.5461 $/MWh (sum / (N + 1), R6 finding 2). The record (`aemo_VIC1_monthly_vwp_199812_202006.csv`) is not part of this check.

## 4b. BasePrice of the market units (for R8)

BasePrice and the capacity factors come from the type blocks of `VIC.yaml` (`generators`), not from the database: `Generator` reads `basePriceMWh` from settings (Generator.java:186), `Arena.applyInflation` raises it by CPI every year. The LCOE-rule price is BasePrice / historic capacity factor, with the factor kept between minCapacityFactor and maxCapacityFactor (BasePrice / maxCapacityFactor for a unit that has never bid). The logged price is `Generator.priceMWhLCOE` in the base case, December of each year: under L1 or L2 the units' dispatch, and so their historic factor, would differ.

| fuel / tech | basePriceMWh (VIC.yaml) | minCF | maxCF | base / maxCF ($/MWh, before CPI) | LCOE price 1999, min-max ($/MWh) | LCOE price 2009, min-max ($/MWh) | LCOE price 2019, min-max ($/MWh) |
|---|---|---|---|---|---|---|---|
| Battery / Storage - Battery | 23.57 | 0.01 | 0.90 | 26.19 | - | - | 4023.7-4023.7 |
| Brown Coal / Turbine - Steam Sub Critical | 4.75 | 0.47 | 0.90 | 5.28 | 10.3-10.3 | 14.0-14.0 | 17.3-17.3 |
| Gas Pipeline / Turbine - OCGT | 7.90 | 0.06 | 0.30 | 26.33 | 111.4-133.6 | 36.5-131.6 | 45.0-224.8 |
| Solar / Solar PV - Fixed | 7.05 | 0.12 | 0.26 | 27.12 | - | - | 37.8-37.8 |
| Solar / Solar PV - Single axis tracking | 7.05 | 0.12 | 0.26 | 27.12 | - | - | 36.0-37.8 |
| Water / Hydro - Dam | 2.06 | 0.03 | 0.20 | 10.30 | 10.5-10.5 | 14.3-14.3 | 17.6-17.6 |
| Water / Hydro - Gravity | 2.06 | 0.03 | 0.20 | 10.30 | 10.5-10.5 | 14.3-14.3 | 17.6-17.6 |
| Wind / Wind Turbine - Onshore | 10.45 | 0.28 | 0.30 | 34.83 | - | 30.9-44.3 | 22.8-54.6 |

