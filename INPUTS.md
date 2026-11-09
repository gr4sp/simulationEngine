# Model inputs: what the model reads, where it lives, where it comes from

Register of every input the Victorian base case reads (`simulationSettings/VIC.yaml` before the base year,
`VICfuture.yaml` after it), as of 9 Nov 2026 (branch `repair/r3-solar-offers`, commit `e8ffe61`).

Inputs live in three places:

- **PostgreSQL database `gr4spdb`**: snapshot DB-2021-8-21, frozen. It holds the original data and must stay
  unchanged so the pre-repair model (`v0.1-article`) still reproduces. Sources below come from the table comments
  stored in the database (`obj_description`) unless marked otherwise.
- **CSV files in `simulationSettings/`**: data added during the 2026 repairs (R3). Versioned in git with the code
  that reads them; each row has a `source` column and each file a header comment. Each is read only when its setting
  is set in `VIC.yaml`.
- **YAML settings** (`VIC.yaml`, `VICfuture.yaml`): scalar parameters (capacity factors, base prices, market rules),
  with sources in the comments beside them.

Plan: at the dataset update before the Sobol rerun, build a new versioned database from scripts that includes the
CSV inputs, and keep DB-2021-8-21 as the frozen reference.

## 1. Database tables (DB-2021-8-21)

| Table | What the model takes | Period | Used for | Source |
|---|---|---|---|---|
| `generationassets` | Every generator in Victoria: name, fuel, technology, capacity, units, dispatch type, start and end dates, owner | 1900-2100 | Supply side, merit order | AEMO Generation Information (NEM); large-scale PV from APVI (pv-map.apvi.org.au/power-stations) to Nov 2019. Corrected at load time by `generator_corrections.csv` |
| `total_demand_halfhour` | VIC1 half-hourly total demand (MW) and price | 1999-01 to 2021-06 | Market demand each half-hour | AEMO aggregated price and demand data (aemo.com.au, NEM data) |
| `generation_consumption_historic` | Monthly generation by fuel (incl. `hydro_gwh`), total consumption, prices | 1900-01 to 2020-09 | Monthly calibration of half-hourly demand to total generation; recorded monthly hydro energy (`monthlyEnergyFromRecord`); pre-1999 history | Merge of OpenNEM generation, VIC demand data and SECV records |
| `solar_ghi` | Half-hourly global horizontal irradiance, Melbourne Airport (BoM station 086282) | 1999-05 to 2020-07 | Solar output | BoM one-minute solar data via nationalmap.gov.au, 30-min means |
| `solar_installation_monthly` | Rooftop PV installations and capacity per month | 2007-01 to 2019-09 | Rooftop PV fleet | APVI (pv-map.apvi.org.au/postcode) |
| `domestic_consumers` | Number of domestic consumers per year | 1902-2021 | Households | Comment: "latest version 16/03/2020"; original source not stated (to document) |
| `household_forecast_victoria` | Households by type | 2016-2051 | Households after the base year | DELWP Victoria in Future 2016; extended past 2036 by the author |
| `isp_annual_consumption`, `isp_maximum_demand`, `isp_minimum_demand` | Annual consumption (actual from 2010) and max/min demand forecasts by scenario | 2010-2050 | Demand after the base year | AEMO ISP 2019 and ESOO 2019 (effective 29 Oct 2019) |
| `tariffshistoric` | Historical retail tariffs | 1895-2019 | Tariffs in mapping mode | SECV applications, proceedings and annual reports (various) |
| `historic_tariff_contribution` | Share of each supply-chain component in the tariff (wholesale share R_w) | 1999-2020 | Tariffs in simulation mode | Constructed from various sources (R3 step 3: document or flag 2001-02, 2004-06, 2018-19) |
| `cpi_conversion` | CPI conversion to 2019 dollars | 1901-2019 | Tariffs in constant dollars | Comment: historical CPI; RBA Aug 2019 forecasts for future years |
| `historic_inflation` | Annual inflation | 1990-2019 | Inflation of base prices | rateinflation.com (secondary source; to replace with ABS 6401.0) |
| `actors`, `arenas`, `spm`, `connectionpoint`, `networkassets`, `storage` | Model structure: owners, markets, SPM nesting, network assets and loss factors | - | Structure | Author-built; `actors` from the actor-actor relationship database; `networkassets` updated Nov 2019 |

## 2. CSV files (`simulationSettings/`)

| File | Setting | What | Source | Rebuild |
|---|---|---|---|---|
| `generator_corrections.csv` | `generatorCorrections` | Start/close dates and exclusions that fix `generationassets` rows (Murray 1-2 in VIC1 from July 2008; Somerton 2002; Mortlake 2012; old Dartmouth, McKay, Eildon rows closed) | OpenElectricity station records, per row | by hand |
| `generator_parameters_VIC.csv` | `generatorParametersFile` | Station heat rates, variable O&M, coal price, outage rates and durations | AEMO 2020 IASR workbook v2.1 (sheets Heat rates, Variable OPEX, Coal and Biomass price, Generator Reliability Settings; based on GHD 2018 for AEMO); ACIL Tasman 2009 Table 16 for stations closed before 2020 (Hazelwood, Anglesea, Morwell) | by hand |
| `fuel_price_gas_VIC.csv` | `fuelPriceFile` (gas) | Gas price, $/GJ nominal, by year | 1999-2003 AER State of the Energy Market 2007 Fig 8.17 (read from the chart); 2004-2020 AEMO DWGM; 2021 on, mean of 2017-2020 | notes repo `dwgm_series.py` |
| `carbon_price_AU.csv` | `carbonPriceFile` | Carbon price by month, Jul 2012 - Jun 2014 | Clean Energy Act 2011 fixed prices | by hand |
| `cpi_melbourne.csv` | `cpiFile` with `costPriceYear: 2020` | Melbourne CPI, annual, to turn 2020-dollar costs into each year's dollars | ABS 6401.0 Table 7 | by hand |
| `net_exports_shape_VIC.csv` | `netExportsShapeFile` with `shapeDemandUplift` | Half-hourly shape of VIC net exports by calendar month | AEMO MMSDM TRADINGINTERCONNECT 2015-2020 | `scripts/data/net_exports_shape_vic.py` |
| `net_exports_VIC.csv` | `netExportsFile` (not set in the base case: exports are already in the calibrated demand) | Monthly net exports | OpenNEM; before Apr 2005 the 2006-08 calendar-month means | `scripts/data/net_exports_opennem.py` |
| `portfolios_VIC.csv` | `portfolioFile` (withholding scenario only; off in the base case) | Stations grouped into owner portfolios | Ownership histories, per row | by hand |
| `interconnectors_VIC.csv` | `interconnectorFile` | Import capability per link at VIC peak demand, and the neighbour's offer (short-run cost or water value) | AEMO MMSDM DISPATCHINTERCONNECTORRES 2015-2020 (median limit in the top 5% demand half-hours); AER $5000 report 26 Jan 2006 (VIC-Snowy before July 2008); IASR 2020 (neighbour costs) | `scripts/data/interconnector_limits_vic.py` |
| `market_price_cap_NEM.csv` | `shortfallPriceFile` | Market price cap by effective date | AEMO MMS MARKET_PRICE_THRESHOLDS (MMSDM 2020-12) | by hand from the table |
| `tariff_contribution_VIC.csv` | `tariffContributionFile` (opt-in, not yet in the base case) | Wholesale share of the residential bill (R_w), 1999-2020, each row marked source / constructed / unknown; replaces `historic_tariff_contribution` | Oakley Greenwood 2017 Table 3 (2001-2002, 2007-2017); AEMC Residential electricity price trends 2018-2020 (2018-2020); 1999-2000 and 2003-2006 without source | by hand |

## 3. YAML parameters

Only part of `VIC.yaml` cites its sources in comments today (the IASR cost and outage block at the top, and
`annualInflation`). The main groups:

- **Per fuel and technology** (`generators:`): base price, min/max capacity factor (availability), emissions factors,
  SRMC options, forced outage rate, storage settings. Cost and outage values: AEMO 2020 IASR and ACIL Tasman 2009
  (cited in the file header). Base prices and emissions factors: the original model's values (main article Table
  A.1), source per value not yet recorded. Values changed in R3 step 1 (solar/wind SRMC offers, wind availability
  0.30, hydro 0.20, coal 0.90, battery offer factor): derivation in the notes repo,
  `docs/r3_offer_tests_30oct/README.md`.
- **Market** (`arena:`): which generators bid in which market, participation thresholds.
- **Demand and forecast** (`forecast:`): base year, scenario choices, `importPriceFactor` (still read by the
  secondary market; the primary market now uses section 2's imports and price cap).
- **Tariffs** (`tariffs:`): `wholesaleContribution` (R_w) for years `historic_tariff_contribution` does not cover.

## 4. Records used only to check results (not read by the model)

| Record | Used for | Where |
|---|---|---|
| AEMO VIC1 monthly demand-weighted price, Dec 1998 - Jun 2020 | Price validation (R2 comparator) | `Sandbox/papers/GR4SPArticle_ERSS/data/aemo_VIC1_monthly_vwp_199812_202006.csv` |
| OpenNEM VIC1 monthly generation by fuel, Apr 2005 - Jun 2020 | Generation and renewable share validation | `experiments/simulationData/2005_2020_OpenNemDataV1.csv` |
| Victorian Government greenhouse gas inventory, 1990-2018 | Emissions validation | `experiments/simulationData/19902018_historic_emissions_Vic.csv` |
| Oakley Greenwood; St Vincent de Paul tariff series | Tariff validation | `experiments/simulationData/2001to2019_historicTariffs.csv` |
| AER gas used for power generation, VIC, quarterly from 2008 | Sanity check | notes repo `checks.py` |
| AEMO half-hourly price (in `total_demand_halfhour`) | Spike frequency, price profiles | database |

## 5. Gaps to close

- `VIC.yaml`: add a source comment to each per-fuel value (base prices, emissions factors, capacity factors).
- `domestic_consumers`: name the original source.
- `historic_inflation`: replace the secondary source with ABS 6401.0 (as `cpi_melbourne.csv` already does).
- `historic_tariff_contribution`: sources found for 2001-2002, 2007-2020 (`tariff_contribution_VIC.csv`); 1999-2000
  and 2003-2006 still without a source (the 2014 Oakley Greenwood edition, an APH submission, may cover 2003-2006).
- `interconnectors_VIC.csv`: limits before 2015 and the Murraylink start date are assumptions; SA gas uses the
  Victorian gas price.
- IASR emissions sheet values for SA gas stations are implausible (about 1 t/MWh) and are not used.
