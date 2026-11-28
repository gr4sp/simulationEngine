# Half-hourly output switch (R9), base case, seed 1

Written by `experiments/validation/half_hour_output.py analyse experiments\validation\half_hour_output_2026-11-28 1`.

## 1. Output only

| run | file | identical to `experiments\validation\ensemble_2026-11-24\run_s1` |
|---|---|---|
| off_s1 | VICSimDataMonthlyPrices_seed_1.csv | True |
| off_s1 | VICSimDataMonthlySummary_seed_1.csv | True |
| off_s1 | VICSimDataYearSummary_seed_1.csv | True |
| on_s1 | VICSimDataMonthlyPrices_seed_1.csv | True |
| on_s1 | VICSimDataMonthlySummary_seed_1.csv | True |
| on_s1 | VICSimDataYearSummary_seed_1.csv | True |

## 2. Monthly sums of the half-hourly file against the monthly summary (624 months, 1999-01-01 00:00 to 2050-12-31 23:30)

The monthly CSV prints about seven significant figures, so differences are rounding when the relative difference is below about 1e-6. Price: the half-hourly prices of the month summed and divided by N + 1, as the model does (R6 finding 2). Rooftop PV is counted in both Rooftop PV and the off-market total.

| monthly output | unit | months | largest abs. difference | largest relative difference |
|---|---|---|---|---|
| Primary Wholesale ($/MWh) | $/MWh | 624 | 20.7 | 0.0021 |
| System Production Primary Spot | MWh | 624 | 9.69e-08 | 2.28e-14 |
| System Production Off Spot | MWh | 624 | 3.96e-09 | 7.32e-15 |
| Primary Total Unmet Demand (MWh) | MWh | 624 | 1.86e-09 | 2.2e-15 |
| System GHG Emissions (tCO2-e) | t | 624 | 1.16e-07 | 3.46e-14 |
| System Production Rooftop PV | MWh | 624 | 2.56e-09 | 3.92e-15 |
| System Production Coal | MWh | 624 | 9.55e-08 | 4.37e-14 |
| System Production Water | MWh | 624 | 5.24e-10 | 4.68e-15 |
| System Production Wind | MWh | 624 | 1.58e-08 | 1.37e-14 |
| System Production Gas | MWh | 624 | 5.82e-10 | 2.19e-14 |
| System Production Solar | MWh | 624 | 2.33e-10 | 2.94e-15 |
| System Production Battery | MWh | 624 | 1.75e-10 | 1.49e-15 |
| Primary Wholesale ($/MWh), months without a zero-demand half-hour | $/MWh | 616 | 0.000482 | 5.65e-08 |
| Primary Wholesale ($/MWh), 1999-2019 | $/MWh | 252 | 3.51e-06 | 5.65e-08 |

Months with a half-hour of no market demand (left out of the model's monthly mean price): 8 (2045-10, 2046-10, 2047-10, 2048-10, 2049-10, 2049-11, 2050-10, 2050-11).

Energy balance of the primary rows (dispatch - (market demand - unserved - imports)), largest monthly |value|: 1.41 MWh. The clearing counts a gap as unserved only when ceil(offered) < floor(demand) (SpotMarket.computeMarketPrice), so up to about 1 MW in a half-hour is neither dispatched nor unserved.

## 3. Size and run time

| | switch off | switch on |
|---|---|---|
| run time, wall clock (s) | 268.4 | 253.9 |
| half-hourly file (CSV, MB) | - | 346.2 |
| rows | - | 1823328 |

The two runs ran one after the other on the same machine. Period written: the whole run (halfHourOutputFrom/To unset), 1999-2050.

