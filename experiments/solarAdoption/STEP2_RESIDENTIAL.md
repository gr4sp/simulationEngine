# Solar adoption, step 2: residential demand from the record (14 Nov 2026)

Design section 5, decision D3. Setting `residentialConsumptionFile: residential_electricity_VIC.csv` (`VIC.yaml`); unset,
the thesis's constant `domesticConsumptionPercentage` (0.3) applies.

## What changed

- Household electricity use in each calendar year = the residential grid electricity of that year / the households.
  The series is Australian Energy Statistics 2020, Table F3, Victoria, residential electricity (FY1974-FY2019,
  `scripts/data/residential_electricity_vic.py`), then AEMO's residential grid demand forecast for the consumption
  scenario (`isp_annual_consumption`, category Residential, "Res grid demand", FY2020-FY2050). Financial years become
  calendar years as in N29 (calendar Y = mean of FY Y and FY Y+1).
- The residential share of the total is therefore a yearly series: residential / the year's total consumption
  (`total_consumption_gwh`, generation-based). It is written to the year summary (`Residential Share of Consumption`).
  Each month's household use keeps the shape of the month's total.
- The year summary also reports `System GHG Emissions (tCO2-e)`: every generator's output x its emission factor. The
  validation (`validation_statistics.py`, `checks.py`, `tracker_data.py`) now uses it instead of emissions per
  household x households / 0.3, which held only while the share was 0.3.

## Effect (seed 42)

Only the household outputs change; every market, price and tariff column is identical with the switch on or off.

| Year | Share (was 0.3) | Household use, kWh (was) | AES per household, MWh | GHG per household, t (was) |
|---|---|---|---|---|
| 1999 | 0.236 | 6,265 (7,973) | 6.44 (FY1999) | 8.10 (10.31) |
| 2005 | 0.212 | 6,119 (8,679) | 6.11 (FY2005) | 7.54 (10.69) |
| 2012 | 0.204 | 5,359 (7,862) | 5.52 (FY2012) | 6.40 (9.38) |
| 2019 | 0.227 | 4,228 (5,596) | 4.41 (FY2019) | 4.12 (5.45) |
| 2030 | 0.225 | 3,557 (4,734) | | 2.15 (2.86) |
| 2050 | 0.172 | 2,623 (4,566) | | 0.35 (0.61) |

The HECS 2012 (5.4-6.0 MWh a year) and the network data (about 6.0 MWh per household in 2012) bracket the new 2012
value. The model's households are slightly more numerous than AES's, so its use per household sits 2-5 % below AES's.
The forward share falls to 0.17 by 2050 because AEMO's residential grid demand falls (rooftop PV, efficiency) while
the total grows with electric vehicles and business load.

Validation, seed 42: GHG NSE 0.68 on system emissions (0.48 on the household route); all sanity checks pass.

## Not in this step

- Use by profile (household type x dwelling x fuel): the model still has one block of identical households. Step 3
  creates the cohorts; each will take its profile's use (the EVA's IMAP requirements) scaled so the cohorts add up to
  this residential series.
- Self-consumption: AES "Residential, electricity" is grid electricity, so the series is already net of rooftop output
  used at home. Step 3 adds the adopters' self-consumption explicitly.

## N27 (utility solar) and the next steps

Utility solar is 52 % above the record in 2018 (206 vs 135 GWh) and 3 % above in 2019 (654 vs 636). Three parts:
farms produce in full from their commissioning date (the record ramps up through 2018); the large farms are
single-axis tracking in northern Victoria but get Melbourne's horizontal irradiance (in 2019 they make 523 GWh); and
about 130 GWh in 2019 comes from 0.3-2 MW commercial systems that the record's utility series probably does not
contain. It does not touch step 2. For steps 3-4 (adoption calibrated on 2007-2019) it is small: it moves the 2018-19
wholesale price, hence a few per cent of the tariff in two years. It matters for the forward period, where the
capacity path adds tracking farms; to be repaired before the forward scenarios.
