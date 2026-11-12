# Parameter audit: the thesis's sensitivity parameters in the repaired model (R3 step 4, 11 Nov 2026)

Angela asked (11 Nov) how to tell, before rerunning the full Saltelli designs, whether the repairs change the sensitivity
and uncertainty results. Step 1 of four: trace each uncertain parameter of the thesis designs
(`gr4spModelEET.py`, 34 parameters; the Sobol designs use 24 or 29 of them) through the repaired code, as
`connector.runGr4sp` applies it, with the repaired base case (`simulationSettings/VIC.yaml` at `0e8f500`).

**Why parameters went dead.** In the repaired base case every fuel has its own spot offer rule
(`Generator.offerPriceMWh`): coal and gas offer at fuel x heat rate + operating cost + carbon (`heatRateGJperMWh`), wind
and solar at 0 (`srmcMWh: 0`), hydro at a water value on its recorded monthly energy (`storageOfferFactor`,
`monthlyEnergyFromRecord`), the battery by daily arbitrage. The old LCOE rule (base price / capacity factor) remains only
for generators without such a rule and for off-market prices. Unmet demand is priced by the interconnector rule and the
market price cap (`interconnectorFile`, `shortfallPriceFile`), and the tariff's wholesale share comes from the sourced
register for 1999-2020 (`tariffContributionFile`).

| Parameter | Status in the repaired model | Where |
|---|---|---|
| priceChangePercentageBrownCoal, ...Ocgt, ...Ccgt | **inactive in the spot market** (offers at running cost; base price used only off-market) | `Generator.offerPriceMWh`, `connector.py` sets base prices |
| priceChangePercentageWind, ...Solar | **inactive** (offer at 0) | same |
| priceChangePercentageWater | **inactive** (water-value offer) | same |
| priceChangePercentageBattery | **inactive** (arbitrage storage offers 0) | same |
| learningCurve | **inactive** (only lowers wind/solar base prices) | `SimPolicies` 116-187 |
| importPriceFactor | **inactive** (reached only when no shortfall price is set; the base case sets one) | `SpotMarket` 209-217 |
| wholesaleTariffContribution | **inactive 1999-2020** (sourced register); active after 2020 (YAML fallback) | `EndUserUnit` 150-160 |
| technologicalImprovement | **changed**: the base case keeps it at 0 before the base year through `VIC.yaml`; the connector sets both periods, so sampling it brings back historical improvement (wind availability, solar efficiency from 1999). A new design must vary only the after-base-year value | `SimPolicies` 112-187, `connector.py` |
| nameplateCapacityChangeWater | **changed**: before 2021 hydro gets each month's recorded energy, so capacity moves peak power only, not energy | `monthlyEnergyFromRecord` |
| semiScheduleGenSpotMarket, semiScheduleMinCapMarketGen | **changed in meaning**: participation now brings wind and solar into the spot market at 0 $/MWh | `Settings.isMarketPaticipant` |
| annualInflation | **weakened**: escalates base prices, which now reach only generators on the LCOE rule | `Arena` 343-354 |
| scheduleMinCapMarketGen, nonScheduleGenSpotMarket, nonScheduleMinCapMarketGen | active | `Settings.isMarketPaticipant` |
| nameplateCapacityChangeBrownCoal, ...Ocgt, ...Ccgt, ...Wind, ...Solar, ...Battery | active | `LoadData` 1606 |
| domesticConsumptionPercentage | active | `LoadData` 1229-1235 |
| consumption, energyEfficiency, onsiteGeneration, solarUptake, rooftopPV, annualCpi | active after the base year only (forecast scenarios), unchanged by the repairs | `LoadData` 71-227, 645 |
| includePublicallyAnnouncedGen, generationRolloutPeriod, generatorRetirement | active after the base year only, unchanged | `LoadData` 1551-1613 |

**Which design.** Two thesis Sobol designs exist, and their rankings differ because they answer different questions.
Article Table 5 (`tab:sa`) is the after-base-year design: 29 parameters sampled at 2019, indices from 2019 on, 126,000
runs (`notebookGr4sp/ema_gr4sp_SOBOL_ABY.ipynb` -> `outputs/data/SOBOL__median_sensitivity_Indices.xlsx`; checked
12 Nov 2026: every S1 and ST in Table 5 matches the workbook to three decimals). The hypothetical-past design
(`gr4sp_SOBOL_hypopast2021-Mar-03_includ_wholesale_month.tar.gz`, 24 parameters, indices from 1999) is the one used in
`saltelli_convergence.md`; its price top three include `priceChangePercentageOcgt` and `importPriceFactor`, which are
not in Table 5. Both are right for their own design.

**Against the thesis's results (article Table 5, after-base-year design, top five by ST).** Wholesale price: all five still act, but
`semiScheduleGenSpotMarket` has changed meaning. GHGE and renewable share: `priceChangePercentageBrownCoal` is in both top
fives (ST 0.098 and 0.157) and is now inactive; `semiScheduleGenSpotMarket`, first for both, has changed meaning. So the
thesis's Sobol rankings for GHGE and renewable share **cannot be carried over** to the repaired model, whatever the Morris
pilot shows.

**Not in the old designs at all: the repaired model's own uncertain inputs.** Station heat rates, operating costs and
coal cost (`generator_parameters_VIC.csv`), the gas price series and the carbon price, the coal must-run share and price,
breakdown rates and durations, the hydro water-value factor (`storageOfferFactor`), the battery round-trip efficiency,
availabilities (coal 0.9, wind 0.30), interconnector limits, the R_w register, and the export shape. Several were the
largest levers in the 5 Nov sensitivity runs (heat rates and coal cost, must-run share, water value).

**Implication for the rerun.** A full Saltelli rerun with the old parameter list would spend about a third of its
parameters on knobs that do nothing (9 of 34 inactive, 5 changed) and leave out the repaired model's main uncertainties.
If a rerun is needed, its parameter list should be redesigned first: drop the inactive ones, restrict
`technologicalImprovement` to after the base year, and add the repaired inputs with ranges from their sources. The Morris
pilot (step 2) checks this audit (inactive parameters should show mu* ~ 0) and the rankings of the parameters that still
act; the convergence check (step 3) gives the sample size such a rerun needs.

## After the base year (12 Nov 2026)

Angela (12 Nov): the after-base-year design is the one that matters (future scenarios and transition pathways; article
Table 5); the hypothetical past was a thesis check of what would have happened had x or y changed. The Morris pilot now
screens that design (`morris_pilot.py run aby`, `connector.runGr4spAfterBaseYear`, added 12 Nov: before it,
`gr4spModelEET.getModelAFterBaseYear` pointed to a function that did not exist in `connector.py`). The 533 runs of the
hypothetical-past pilot are kept in `csv/VIC/morris_pilot_hypopast_2026-11-11/` with their sample
(`results/morris_past_sample.object`).

How the sampled values reach the run in this design (traced in the code): at the base year `SimPolicies` (60-100,
220-240) resets every generator's base price, nameplate capacity and market participation from `settingsAfterBaseYear`;
the import factor (`SpotMarket` 215-228), inflation (`Arena` 343) and the wholesale share (`EndUserUnit` 150-160) switch
on the date; the forecast inputs (consumption, efficiency, on-site generation, solar uptake, rooftop PV, CPI, domestic
share, announced generation, rollout, retirement) are read from `settingsAfterBaseYear` in `LoadData`.

What changes against the table above, for this design:
- `wholesaleTariffContribution`: the sourced register covers 1999-2020, so the sampled value acts from 2021.
  **Superseded 12 Nov 2026 (`b6df85e`):** the base case now builds the tariff additively (`tariffComponentsFile`,
  `EndUserUnit` 190-196), which never reads the wholesale share, so this parameter is inactive in every year.
- **Capacity path (`b6df85e`, `capacityPathFile`, ISP 2020 Central):** from FY2021 wind, solar, battery and gas are
  topped up to the ISP path (`LoadData.addCapacityPath`, computed once when generators load). Where the path binds,
  `includePublicallyAnnouncedGen`, `generationRolloutPeriod` and the wind/solar/battery/gas `nameplateCapacityChange*`
  can only add capacity above it; expect their effects to shrink. The ABY pilot on this base case checks it.
- `nameplateCapacityChangeWater`: once the recorded monthly hydro series ends, each hydro generator gets no budget (-1)
  and falls back to the old rule (`Arena` 491-507), so capacity can move hydro energy and price again.
- The forecast inputs and `includePublicallyAnnouncedGen`, `generationRolloutPeriod`, `generatorRetirement` act only in
  this period, so they are the parameters this design is about.
- Not traced yet, left to the pilot (mu* ~ 0 or not): whether the price-change parameters reach any generator after the
  base year (new generators from the announced list may fall on the LCOE rule), and whether `importPriceFactor` still
  never acts (the import rule and shortfall price are read from VIC.yaml in all years).
