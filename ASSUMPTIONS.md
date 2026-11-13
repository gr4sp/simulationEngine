# Settings and assumptions register (audit N24, 14 Nov 2026)

Two parts: where every setting's value comes from (audit B1), and one table of the model's assumptions (audit E). Data
inputs and their sources are in `INPUTS.md`; this file covers the values set by hand.

Words used here:
- **sourced**: the value comes from a named document or data set;
- **decision**: chosen on purpose and recorded (change log or README), with no external source needed;
- **constructed**: derived from sourced data by a stated rule (an interpolation, an extension);
- **assumed**: chosen with no source; it should be varied in the sensitivity runs;
- **unsourced**: no source or record found;
- **inactive**: read by the code but without effect in the base case (after the R3 repairs; see
  `experiments/sensitivity/PARAMETER_AUDIT_R3.md`).

## 1. The settings register

`scripts/settings_register.py` writes `settings_register.csv`: one row per leaf key of `simulationSettings/VIC.yaml`
and `VICfuture.yaml` (295 rows), with its value, line, the commit and date that last set it (git blame), the comment
that explains it, and a screen status: `cited` (the comment names a source), `explained` (it explains without a
source), `block` (the comment above the block applies) or `none`.

`VICfuture.yaml` holds only the keys the code reads from the after-base-year settings (`settingsAfterBaseYear`): the
forecast scenarios, market participation, offer rules of new generators, technological improvement and learning curve.
Every other key (generator capacity factors, emission factors, costs, input files) is read from `VIC.yaml` for the
whole run (`Settings.java`, comment at `availabilityFactor`). The two files differ in one value:
`forecast.technologicalImprovement`, 0 in `VIC.yaml` and 0.01 a year after the base year. Some code still writes
wind capacity factors into the after-base-year settings (`SimPolicies` 149-160); no generator reads them, so they have
no effect.

## 2. Settings reviewed by hand, by family

| Family | Keys (base case value) | Active in the base case | Source | Status |
|---|---|---|---|---|
| Run control | `folderOutput`, `reportGeneration`, `logLevel`, `simulationDates.*` (1998-2051, spot market from 1999) | yes | 1999: first AEMO half-hourly demand | decision |
| Input files | `carbonPriceFile`, `generatorCorrections`, `generatorParametersFile`, `cpiFile`, `interconnectorFile`, `shortfallPriceFile`, `tariffComponentsFile`, `capacityPathFile`, `netExportsShapeFile`, `solarInstallationsFile` | yes | `INPUTS.md`, one row each | sourced |
| Repair switches | `shapeDemandUplift`, `outagesPerUnit`, `solarShapeInDemand`, `forecastFromCalibratedDemand`, `rooftopForecastFromModelYield` (all true) | yes | change log C26-C29, C31, N21, C58, N23 | decision |
| Cost basis | `costPriceYear` 2020 | yes | AEMO 2020 IASR costs are in real 2020 dollars | sourced |
| Coal and gas offers | `heatRateGJperMWh`, `variableOMMWh`, `fuelPriceGJ`/`fuelPriceFile`, `forcedOutageRate`, `outageHours`, `availabilityFactor` by type | yes (stations without a row in `generator_parameters_VIC.csv` use the type value) | AEMO 2020 IASR; ACIL Tasman 2009 for stations closed before 2020 (file header of `VIC.yaml`, `generator_parameters_VIC.csv`) | sourced. The CCGT type defaults (heat rate 7.5, O&M 4, availability 0.9) have no effect in Victoria: no CCGT runs in the market (the database's only Victorian CCGT is Qenos, 21 MW, embedded; the capacity path adds OCGT) |
| Coal must-run | `mustRunShare` 0.6, `mustRunPriceMWh` 0 | yes | none; 0.4 and 0.8 tested (C20, N07) | **assumed** |
| Hydro | `monthlyEnergyFromRecord`, `storageOfferFactor` 1.0, `peakOnly`, `dailyEnergyLimit` | yes | monthly energy recorded (`generation_consumption_historic`); the water value (last month's mean price) is a decision of R3 | sourced energy, **assumed** water value |
| Wind | `srmcMWh` 0; `maxCapacityFactor` 0.30 | yes | offer at 0: R3; availability 0.30 for market wind; off-market wind runs flat at a CF of 0.30 (audit C3) | decision; flat shape **unsourced** |
| Solar efficiency | `fman` 0.97, `fdirt` 0.95, `ftemp` 0.9, `npv_inv` 0.97, `ninv` 0.9, `ninv_sb` 0.99 (product 0.717) | yes | the yield formula of the Clean Energy Council design guidelines (Issue 6.0, 2012, s. 8.1); dirt 0.95 is the guideline's example, the temperature factor assumes a 20 C day, a 25 C cell rise and -0.5 %/C (comment), the rest are typical values | sourced formula, assumed values |
| Rooftop array | `rooftopArrayTilt` 16, `rooftopArrayAzimuth` 0 | yes | Killinger, Lingfors and Saint-Drenan (2018), Australian mean tilt, equator-facing | sourced (see A2) |
| Battery | `roundTripEfficiency` 0.85; `storageOfferFactor` 1.2 | yes (Ballarat from 2018; the capacity path) | measured 85.0 % at the connection point over Gannawarra's first two years (Tesla Powerpack, 25 MW / 50 MWh; ARENA final knowledge sharing report, 2021, Fig. 3); Ballarat's contracted 87.17 % (year 1) and 86.80 % (year 2) (Fluence lithium-ion, 30 MW / 30 MWh; ARENA final report, 2021, Table 1); AEMO 2020 IASR 81 % for utility batteries (lower bound for sensitivity); offer factor R3 | sourced, decision |
| Off-market capacity factors | station `capacityFactor` in `generator_parameters_VIC.csv`: 25 units; type values (`Default` 0.9 and others) only for units without one | yes since F1 (units outside the market run at their capacity factor) | each unit's mean output in the NGER designated generation facility data 2013-14 to 2018-19 over its capacity; units without history take their Victorian class (landfill gas 0.648, small gas engines 0.193); embedded units (Longford, Qenos, hospital backup diesel) 0: the method of AEMO's demand forecasts for other non-scheduled generation (Electricity Demand Forecasting Methodology Information Paper 2020, s. 4.1.2). The `Landfill` and `Dry wood` blocks never applied: the database names are `Landfill Gas` and `Dry Wood`, so those units took `Default` | sourced (N25) |
| Emission factors | station `emissionFactorTonnesMWh` for 13 stations (brown coal 1.086-1.425, gas 0.533-0.786 t/MWh); type `minEF` for other units | yes (GHG outputs) | NGER scope 1 / production, 2013-14 to 2018-19 (the Acil Allen 2016 report in `docs/` is the review behind AEMO's factors; its station values are charts, the IASR 2020 Emissions sheet tabulates AEMO's values: for brown coal they agree with NGER within 0.01 t/MWh; for the gas peakers NGER differs by up to 0.2 t/MWh, as their intensity depends on starts); Morwell: its heat rate x 95 kg/GJ (Acil Allen 2016) | sourced (N25); held constant before 2013-14 |
| LCOE rule | `basePriceMWh`, `marketPriceCap` 8000, `minCapacityFactor` by type; `policy.annualInflation` 0.033; `forecast.learningCurve` 0.05 | **inactive** in the spot market (every type has its own offer rule); used for off-market prices only | thesis values (CO2CRC 2019, CSIRO 2018 for wind; Lazard for storage) | inactive |
| Market participation | `arena.*`: scheduled and semi-scheduled from 30 MW in the primary market; non-scheduled in none | yes | AEMO registration: units under 30 MW are normally non-scheduled | sourced (not cited in the file) |
| Household use | `residentialConsumptionFile` (base case); `population.domesticConsumptionPercentage` 0.3 only when unset | yes (household consumption and GHG per household, not the market) | AES 2020 Table F3 residential electricity FY1999-FY2019, then AEMO's residential grid demand forecast (ISP/ESOO 2019); share of the total 0.19-0.24 in 1999-2019 | sourced (solar step 2) |
| Forecast scenarios | `forecast.scenario.*` Central; `rooftopPV` both | after 2020 | AEMO 2020 ISP | sourced |
| Forecast timing | `baseYear` 2019, `startDemandForecast` 2020-08-01 | yes | last full year; last month of AEMO data | decision |
| Forecast rules | `includePublicallyAnnouncedGen` false, `generationRolloutPeriod` 1, `generatorRetirement` 0, `commissioningDelay` 0 | after 2020 | C53 (delay; median 1.3 years late recorded, sensitivity) | decision |
| Price indices | `forecast.annualCpi` 0.0233 | after 2020 | median CPI change 1990-2019 (comment cites the series) | sourced |
| Technological improvement | 0 in `VIC.yaml`; 0.01 a year in `VICfuture.yaml` | after 2020 | none for 0.01 | **assumed** (in the thesis designs) |
| New rooftop systems | `forecast.solarInstallCapacity` 6.3 kW | after 2020 (installation counts only) | the mean system size of January 2019 in the installation register | constructed |
| Imports | `forecast.importPriceFactor` 0.29 | **inactive** (shortfall prices are set) | thesis | inactive |
| Tariff share | `tariffs.wholesaleContribution.*`; `tariffContributionLag` 0 | **inactive** since the additive tariff (12 Nov) | R3 step 3 | inactive |
| Nameplate scaling | `nameplateCapacityChange` 1.0 | yes (neutral) | sensitivity knob | decision |

Nothing open from this review: the residential share comes from the record since solar step 2, and the x 1.25 on the rooftop forecast was dropped (N29). The emission factors, off-market capacity factors and battery efficiency were sourced on 14 Nov (N25).

## 3. Assumptions register

Varied: whether the sensitivity designs vary the assumption. "Thesis" means the thesis's Morris and Sobol designs,
whose parameter list predates the repairs (`PARAMETER_AUDIT_R3.md`); "tested" means a one-at-a-time test is recorded;
"no" means it has not been varied yet (the new parameter list, N18, should take these up).

| # | Area | Assumption | Value | Basis | Varied | Where |
|---|---|---|---|---|---|---|
| A1 | Rooftop PV | AEMO's 2019 rooftop forecast (ESOO/ISP inputs, October 2019) used as published, read as financial years | uplift 1.0 (thesis 1.25) | decision (N29, 14 Nov): AEMO's FY2020 value is within 2 % of the recorded FY2020 output; the thesis uplift reflected installations seen after 2020 | no | `rooftopForecastUplift`, `forecastFinancialYears` |
| A2 | Rooftop PV | Arrays face north at 16 degrees; diffuse irradiance by Erbs, transposition by Hay-Davies, ground reflectance 0.2 | +8.6 % irradiation (2007-2019) | Killinger et al. (2018) mean tilt; standard models. A fleet with a quarter facing east and a quarter west would get +3.1 % at 20 degrees | no | `RooftopIrradiance`, `VIC.yaml` |
| A3 | Rooftop PV | One irradiance station (Melbourne Airport) for all of Victoria | | decision; with it, rooftop output is within -6.2 to +6.2 % of the record each year (2011-2019) | no | `solar_ghi` |
| A4 | Rooftop PV | Systems last 30 years and are then replaced (forward period) | 30 years | thesis value | no | `SimPolicies`, `LoadData.createSolarInstallationForecastFromYield` |
| A5 | Solar | Missing irradiance half-hours take the mean of the same half-hour on the three days either side | | constructed (N23) | no | `LoadData.fillSolarExposureGaps` |
| A6 | Solar, demand | Every forward year repeats the base year's irradiance (2019) | | thesis | no | `LoadData.createHalfHourSolarExposureForecast` |
| A7 | Generation | Fuel split of the record before 2005 | | constructed (audit F3) | no | `generation_consumption_historic` |
| A8 | Gas price | 1999-2003 read from the AER chart | | secondary (C16) | no | `fuel_price_gas_VIC.csv` |
| A9 | Coal | Must-run share and its price | 0.6 at 0 $/MWh | assumed | tested (0.4, 0.8) | `VIC.yaml` |
| A10 | Hydro | Water value: last month's mean spot price | factor 1.0 | assumed | tested (5 Nov) | `VIC.yaml` |
| A11 | Gas | CCGT type defaults: availability 0.9, heat rate 7.5 GJ/MWh, O&M 4 $/MWh | | no effect in Victoria (no market CCGT, 1999-2050) | no | `VIC.yaml` |
| A12 | Small units | Off-market capacity factors: station values, else the Victorian class, embedded units 0 | 0-0.958 | sourced: NGER 2013-14 to 2018-19, AEMO 2020 method (N25) | no | `generator_parameters_VIC.csv` |
| A13 | Wind | Off-market wind runs flat at a capacity factor of 0.30 | | unsourced shape | no | `VIC.yaml` |
| A14 | Emissions | Station emission intensities held constant over each station's life (NGER covers 2013-14 to 2018-19) | brown coal 1.086-1.425 t/MWh | sourced values, assumed constancy | no | `generator_parameters_VIC.csv` |
| A15 | Interconnectors | Limits before 2015 and the Murraylink start date; SA gas at the Victorian price | | assumed (`INPUTS.md`) | no | `interconnectors_VIC.csv` |
| A16 | Tariff | Network, environmental and retail components held in real terms after the last AEMC forecast year (2022/23) | | decision | no | `tariff_components_VIC.csv` |
| A17 | Tariff | 2007 retail component interpolated between 2006 and 2008 | | constructed (C50) | no | `tariff_components_VIC.csv` |
| A18 | Capacity | ISP 2020 Central path to FY2042, then held; closures from the database | | sourced, decision | thesis (rollout, retirement) | `capacity_path_VIC.csv` |
| A19 | Forward period | Projects arrive on time | delay 0 | decision; recorded median 1.3 years late | tested (C53) | `forecast.commissioningDelay` |
| A20 | Forward period | Technological improvement after the base year | 0.01 a year | assumed | thesis | `VICfuture.yaml` |
| A21 | Forward period | CPI after the base year | 2.33 % a year | sourced | thesis | `forecast.annualCpi` |
| A22 | Households | Residential grid use from AES (to FY2019) and AEMO's forecast (after); each month follows the shape of the month's total | 0.17-0.24 of the total | sourced series, assumed monthly shape | no | `residentialConsumptionFile` |
| A23 | EVA | Share of homes on gas: ABS surveys, linear between surveys, held at the ends; 2009 income gradient applied every year | 71.5 % (1999) to 64.3 % (2014) | sourced, constructed | tested (`--fuel-mix`) | R4 step 6 |
| A24 | EVA | A dwelling's age is independent of its occupants' type, fuel and income | | assumed | no | R4 step 8 |
| A25 | EVA | Stock shares by construction period from completions, without demolitions or vacant dwellings (newer stock an upper bound) | | constructed | no | R4 step 8 |
| A26 | EVA | Gas heater efficiency behind the star ratings | 0.8 | assumed | tested (0.7, 0.9) | R4 step 8 |
| A27 | EVA | A lone person uses 0.7 of a couple's energy; group households use an other family's | | thesis | no | notebook cell 66 |
| A28 | EVA | IMAP's 57 m2 low flats are pre-1992 dwellings | | decision | no | R4 step 8 |
| A29 | EVA | Incomes spread evenly within a census bracket; payers of housing costs in six equal groups per cell | | assumed | no | R4 steps 4, 5b |
| A30 | EVA | 1996 and 2001 housing costs: the 2006 census moved by the ratio of the type's medians | | constructed | no | R4 step 5b |
| A31 | EVA | Gas tariff before 2006 falls back 3 % a year | | constructed | no | notebook (`gas_tariff` column) |
| A32 | Solar adoption | Tenure before 2006 held at the 2006 shares | | assumed | no | `STEP1_DATA.md` |
| A33 | Solar adoption | Exports before November 2009 credited at the retail rate | | assumed | no | `STEP1_DATA.md` |
| A34 | Solar adoption | Economics values marked `secondary` (10) or `assumed` (4): 2015-16 minimum feed-in tariffs, Solar Credits dates, Solar Homes cap and income limit, zone 4 rating, certificate price before 2011 | | secondary, assumed | planned | `rooftop_pv_economics_VIC.csv` |
| A35 | Solar adoption | Self-consumption shares by household type | | to set in step 3 | planned | `DESIGN.md` D4 |

| A36 | Small units | Units embedded in an industrial or hospital load supply that load only: Longford (Esso gas plant), Qenos (Altona), the hospital backup diesel | output 0 | AEMO 2020 s. 4.1.2 (removes units captured by net metering); not in NGER or the demand record | no | `generator_parameters_VIC.csv` |
| A37 | Battery | One round-trip efficiency for every battery, existing and generic | 0.85 | measured (Gannawarra 85.0 %); IASR 81 % as the lower bound | no | `VIC.yaml` |
| A38 | Demand | Off-market units of fuel types outside the recorded total (landfill gas, biogas, gas engines, diesel) supply demand the record leaves out; non-scheduled wind and hydro are in the record | | decision (N28): OpenElectricity counts registered non-scheduled units; its biogas and gas-engine techs were not fetched | no | `demandRecordFuels` |
