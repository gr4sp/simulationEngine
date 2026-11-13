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
| Coal and gas offers | `heatRateGJperMWh`, `variableOMMWh`, `fuelPriceGJ`/`fuelPriceFile`, `forcedOutageRate`, `outageHours`, `availabilityFactor` by type | yes (stations without a row in `generator_parameters_VIC.csv` use the type value) | AEMO 2020 IASR; ACIL Tasman 2009 for stations closed before 2020 (file header of `VIC.yaml`, `generator_parameters_VIC.csv`) | sourced, except the CCGT type defaults (heat rate 7.5, O&M 4, availability 0.9: **unsourced**) |
| Coal must-run | `mustRunShare` 0.6, `mustRunPriceMWh` 0 | yes | none; 0.4 and 0.8 tested (C20, N07) | **assumed** |
| Hydro | `monthlyEnergyFromRecord`, `storageOfferFactor` 1.0, `peakOnly`, `dailyEnergyLimit` | yes | monthly energy recorded (`generation_consumption_historic`); the water value (last month's mean price) is a decision of R3 | sourced energy, **assumed** water value |
| Wind | `srmcMWh` 0; `maxCapacityFactor` 0.30 | yes | offer at 0: R3; availability 0.30 for market wind; off-market wind runs flat at a CF of 0.30 (audit C3) | decision; flat shape **unsourced** |
| Solar efficiency | `fman` 0.97, `fdirt` 0.95, `ftemp` 0.9, `npv_inv` 0.97, `ninv` 0.9, `ninv_sb` 0.99 (product 0.717) | yes | the yield formula of the Clean Energy Council design guidelines (Issue 6.0, 2012, s. 8.1); dirt 0.95 is the guideline's example, the temperature factor assumes a 20 C day, a 25 C cell rise and -0.5 %/C (comment), the rest are typical values | sourced formula, assumed values |
| Rooftop array | `rooftopArrayTilt` 16, `rooftopArrayAzimuth` 0 | yes | Killinger, Lingfors and Saint-Drenan (2018), Australian mean tilt, equator-facing | sourced (see A2) |
| Battery | `roundTripEfficiency` 0.85; `storageOfferFactor` 1.2 | yes (Ballarat from 2018; the capacity path) | the block comment cites Lazard LCOS v4.0 (2018) for storage costs, not the efficiency; offer factor R3 | efficiency **unsourced**, offer decision |
| Off-market capacity factors | `Default` 0.9, `Biogas` 0.9, `Diesel` 0.5, `Dry wood` 0.85, `Landfill` 0.9 (`maxCapacityFactor`; minimum 0) | yes since F1 (units outside the market run at their capacity factor) | none | **unsourced** |
| Emission factors | `minEF` by type: brown coal 1.23, OCGT 0.56, CCGT 0.49 t/MWh, others 0.0003-0.957; `linRateEF`, `expRateEF` 0 | yes (GHG outputs) | wind, hydro and solar cite Clean Energy Regulator scope 1 data; the fossil values have none in the file. The 2016 Acil Allen emission factors report in `docs/` gives station intensities only as charts, so the values could not be checked against it | **unsourced** for fossil fuels |
| LCOE rule | `basePriceMWh`, `marketPriceCap` 8000, `minCapacityFactor` by type; `policy.annualInflation` 0.033; `forecast.learningCurve` 0.05 | **inactive** in the spot market (every type has its own offer rule); used for off-market prices only | thesis values (CO2CRC 2019, CSIRO 2018 for wind; Lazard for storage) | inactive |
| Market participation | `arena.*`: scheduled and semi-scheduled from 30 MW in the primary market; non-scheduled in none | yes | AEMO registration: units under 30 MW are normally non-scheduled | sourced (not cited in the file) |
| Household use | `population.domesticConsumptionPercentage` 0.3 | yes (household consumption and GHG per household, not the market) | none; 16-56 % above the recorded residential use (`experiments/solarAdoption/STEP1_DATA.md`) | **unsourced**, to replace in solar step 2 |
| Forecast scenarios | `forecast.scenario.*` Central; `rooftopPV` both | after 2020 | AEMO 2020 ISP | sourced |
| Forecast timing | `baseYear` 2019, `startDemandForecast` 2020-08-01 | yes | last full year; last month of AEMO data | decision |
| Forecast rules | `includePublicallyAnnouncedGen` false, `generationRolloutPeriod` 1, `generatorRetirement` 0, `commissioningDelay` 0 | after 2020 | C53 (delay; median 1.3 years late recorded, sensitivity) | decision |
| Price indices | `forecast.annualCpi` 0.0233 | after 2020 | median CPI change 1990-2019 (comment cites the series) | sourced |
| Technological improvement | 0 in `VIC.yaml`; 0.01 a year in `VICfuture.yaml` | after 2020 | none for 0.01 | **assumed** (in the thesis designs) |
| New rooftop systems | `forecast.solarInstallCapacity` 6.3 kW | after 2020 (installation counts only) | the mean system size of January 2019 in the installation register | constructed |
| Imports | `forecast.importPriceFactor` 0.29 | **inactive** (shortfall prices are set) | thesis | inactive |
| Tariff share | `tariffs.wholesaleContribution.*`; `tariffContributionLag` 0 | **inactive** since the additive tariff (12 Nov) | R3 step 3 | inactive |
| Nameplate scaling | `nameplateCapacityChange` 1.0 | yes (neutral) | sensitivity knob | decision |

Open from this review: the fossil emission factors, the off-market capacity factors, the CCGT type defaults and
`domesticConsumptionPercentage` have no source. The first three affect GHG and the energy of small units; the last is
already scheduled (solar step 2).

## 3. Assumptions register

Varied: whether the sensitivity designs vary the assumption. "Thesis" means the thesis's Morris and Sobol designs,
whose parameter list predates the repairs (`PARAMETER_AUDIT_R3.md`); "tested" means a one-at-a-time test is recorded;
"no" means it has not been varied yet (the new parameter list, N18, should take these up).

| # | Area | Assumption | Value | Basis | Varied | Where |
|---|---|---|---|---|---|---|
| A1 | Rooftop PV | The ISP 2020 rooftop forecast is raised by 25 % "to reconcile the latest info in pv-map" | x 1.25 | **unsourced**; the reason given uses installations after the ISP was made. With it, 2020 additions (about 470 MW) are close to the 574 MW recorded by CER | no | `LoadData.selectForecastSolarUptake` |
| A2 | Rooftop PV | Arrays face north at 16 degrees; diffuse irradiance by Erbs, transposition by Hay-Davies, ground reflectance 0.2 | +8.6 % irradiation (2007-2019) | Killinger et al. (2018) mean tilt; standard models. A fleet with a quarter facing east and a quarter west would get +3.1 % at 20 degrees | no | `RooftopIrradiance`, `VIC.yaml` |
| A3 | Rooftop PV | One irradiance station (Melbourne Airport) for all of Victoria | | decision; with it, rooftop output is within -6.2 to +6.2 % of the record each year (2011-2019) | no | `solar_ghi` |
| A4 | Rooftop PV | Systems last 30 years and are then replaced (forward period) | 30 years | thesis value | no | `SimPolicies`, `LoadData.createSolarInstallationForecastFromYield` |
| A5 | Solar | Missing irradiance half-hours take the mean of the same half-hour on the three days either side | | constructed (N23) | no | `LoadData.fillSolarExposureGaps` |
| A6 | Solar, demand | Every forward year repeats the base year's irradiance (2019) | | thesis | no | `LoadData.createHalfHourSolarExposureForecast` |
| A7 | Generation | Fuel split of the record before 2005 | | constructed (audit F3) | no | `generation_consumption_historic` |
| A8 | Gas price | 1999-2003 read from the AER chart | | secondary (C16) | no | `fuel_price_gas_VIC.csv` |
| A9 | Coal | Must-run share and its price | 0.6 at 0 $/MWh | assumed | tested (0.4, 0.8) | `VIC.yaml` |
| A10 | Hydro | Water value: last month's mean spot price | factor 1.0 | assumed | tested (5 Nov) | `VIC.yaml` |
| A11 | Gas | CCGT type defaults: availability 0.9, heat rate 7.5 GJ/MWh, O&M 4 $/MWh | | unsourced | no | `VIC.yaml` |
| A12 | Small units | Off-market capacity factors (Default, Biogas, Diesel, Dry wood, Landfill) | 0.5-0.9 | unsourced | no | `VIC.yaml` |
| A13 | Wind | Off-market wind runs flat at a capacity factor of 0.30 | | unsourced shape | no | `VIC.yaml` |
| A14 | Emissions | Emission factors constant over time; fossil values unsourced | brown coal 1.23 t/MWh | unsourced | no | `VIC.yaml` |
| A15 | Interconnectors | Limits before 2015 and the Murraylink start date; SA gas at the Victorian price | | assumed (`INPUTS.md`) | no | `interconnectors_VIC.csv` |
| A16 | Tariff | Network, environmental and retail components held in real terms after the last AEMC forecast year (2022/23) | | decision | no | `tariff_components_VIC.csv` |
| A17 | Tariff | 2007 retail component interpolated between 2006 and 2008 | | constructed (C50) | no | `tariff_components_VIC.csv` |
| A18 | Capacity | ISP 2020 Central path to FY2042, then held; closures from the database | | sourced, decision | thesis (rollout, retirement) | `capacity_path_VIC.csv` |
| A19 | Forward period | Projects arrive on time | delay 0 | decision; recorded median 1.3 years late | tested (C53) | `forecast.commissioningDelay` |
| A20 | Forward period | Technological improvement after the base year | 0.01 a year | assumed | thesis | `VICfuture.yaml` |
| A21 | Forward period | CPI after the base year | 2.33 % a year | sourced | thesis | `forecast.annualCpi` |
| A22 | Households | Residential share of total consumption | 0.3 | unsourced, too high (STEP1_DATA) | thesis | `domesticConsumptionPercentage` |
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
