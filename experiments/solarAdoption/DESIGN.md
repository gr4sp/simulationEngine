# Household rooftop solar adoption as a model decision (design, 13 Nov 2026)

Scope: the ERSS article (1999-2019, on the information available in 2020). Status: D1-D4 decided (Angela, 13 Nov
2026: the recommended option in each); build next.

## 1. Why

Today rooftop PV is an input. Each month `SimPolicies` (lines 195-262) reads the APVI installations and kW for that
month (`solar_installation_monthly`, 2007-2019) and spreads them equally over the sub-distribution SPMs. No household
decides anything, so the 2009-12 boom (premium feed-in tariff, rebates, falling system prices) and who took part in
it are put into the model, not produced by it. The aim is to make adoption a household decision that responds to
what the model itself computes (the tariff) and to policy (rebates, certificates, feed-in tariffs), so the APVI series
becomes a validation target and the distribution of adoption becomes an energy-justice result.

## 2. Households as agents

Now: `EndUserUnit` blocks of up to `maxHouseholdsPerConsumerUnit` identical households (2 persons, no gas, income 0;
the `family`, `income` and `hasGas` fields are marked "not used"). Each block has its own SPM chain: Conventional (1)
contains sub-distribution (6), distribution (5), transmission (7), large generation (9).

Proposed: cohort agents, one per combination of the attributes below, sized from the census (2001, 2006, 2011, 2016,
interpolated as the EVA already does). A cohort holds counts of adopters and non-adopters, not individual households,
so the run time stays close to today's. The attributes match the EVA's 48 profiles (household type x dwelling x fuel)
so that bills with and without solar can be computed per profile (ROADMAP item 20), plus the two the literature puts
first:

| Attribute | Levels | Role in adoption | Evidence |
|---|---|---|---|
| Tenure | owner outright, owner with mortgage, renter | Renters face the split incentive (owner pays, tenant saves) and short stays; owners adopt | Best, Burke and Nishitateno 2019 (ABS SIH 2015-16, HECS 2012: tenure, net wealth, mortgage holders); McCarthy and Liu 2022 |
| Dwelling | separate house, semi-detached, flat, other | Roof access and strata consent; flats rarely adopt | Best et al. 2019 (space constraints); ABS 4602.2 Victoria 2009 |
| Income (quintile, equivalised) | 5 | Up-front cost and credit; adoption rises with income and wealth but is strongest in middle-income outer suburbs, not the richest | Best et al. 2019; Macintosh and Wilkinson 2011 (rebate programme); Nelson, Simshauser and Kelley 2011 |
| Household type | the EVA's 6 | Day-time occupancy sets self-consumption (retirees, families) | Best et al. 2019 (age); EVA profiles |
| Fuel | gas, all-electric | Consumption level, so savings | EVA profiles |

Adopters move from the Conventional SPM (1) to the DER generation SPM (2), which the `spm` table already describes
("DER generation") and contains the same sub-distribution SPM. Their rooftop generator lives in their own SPM, not
spread over every sub-distribution SPM.

## 3. The decision rule

Each month, for the non-adopters of cohort c, the share that installs is

    h_c(t) = e_c * [ a + b * A_c(t) ] * f( PB_c(t) )

- `A_c(t)`: share of cohort c already adopted (imitation; a Bass-type hazard with innovation `a` and imitation `b`).
- `PB_c(t)`: simple payback in years of a system of the year's average size (APVI `system_capacity`, already in the
  register) = net cost / first-year saving.
  - Net cost: system price ($/W, IEA PVPS National Survey Reports for Australia, 1997 onwards) minus the federal
    rebate (PV Rebate Programme / Solar Homes and Communities Plan, 2000-2009, means-tested from mid-2008), minus the
    certificates (Solar Credits multiplier 2009-12, then STCs under the SRES).
  - Saving: self-consumed kWh x the tariff the model computes for that cohort + exported kWh x the feed-in tariff a
    system gets when it is installed (Victoria: premium 60 c/kWh, Nov 2009-2011, for 15 years; transitional 25 c 2012;
    then the standard and minimum feed-in tariffs). The self-consumed share depends on household type (day-time
    occupancy); it is an assumption from the literature, to be set in the build.
- `f(PB) = 1 / (1 + exp(k (PB - PB0)))`: willingness to install falls as the payback lengthens.
- `e_c`: eligibility and barriers: 1 for owner-occupied houses, a calibrated factor for renters and for flats (the
  split incentive and strata), and for the lowest income quintile (credit).

This is the structure of the empirical PV adoption models: an economic core with social influence (Rai and Robinson
2015, Environmental Modelling and Software; Robinson and Rai 2015, Applied Energy, who find that the economics alone
predict the rate and scale of adoption but attributes and social influence are needed for who adopts and where;
Palmer et al. 2015 for Italy). Simpson and Clifton 2017 (Western Australia) found 82 % of buyers installed for financial
reasons after the early adopters, which supports a payback core.

## 4. Feedbacks this closes

1. Tariff -> adoption: the payback uses the model's own tariff, so wholesale prices, the drought of 2007 and the
   carbon price of 2012-14 change adoption.
2. Adoption -> wholesale: rooftop output is already taken off the wholesale demand (`Arena`, lines 556-596); with
   endogenous adoption the amount is the model's own.
3. Adoption -> tariff (decision D2): today the network, environmental and retail components are recorded c/kWh, so a
   household that adopts does not move anyone else's bill. With the network component set as the recorded revenue
   (recorded c/kWh x recorded residential kWh) divided by the simulated residential grid kWh, and the premium FiT cost
   recovered in the environmental component, adoption shifts costs to non-adopters. That cost shift is the justice
   question Nelson, Simshauser and Kelley 2011 and Simpson and Clifton 2016 raise.

## 5. Residential demand: replacing the constant 30 %

Now: each household's monthly use = `domesticConsumptionPercentage` (0.3) x total consumption / households
(`LoadData`, lines 1218-1237). That is about 5.3 MWh a household in 2019 (0.3 x 44,436 GWh / 2.51 m). It is the same
for every household and the share does not change over time, while the business load did (large industrial loads
fell, e.g. the Point Henry smelter closed in 2014) and rooftop PV took a growing part of household use off the grid.
ISP 2020 (in the database) puts FY2020 residential grid use at 11.0 TWh of 42.3 TWh operational (26 %), or 29 % of the
total if the 2.0 TWh of residential rooftop output is added.

Proposed:
- Per-household use by profile (household type x dwelling x fuel), from residential consumption benchmarks (AER /
  Frontier Economics 2017, Victorian climate zones; the EVA's consumption profiles), so a lone person in a flat with
  gas uses less than a family in an all-electric house.
- Scaled each year so that the residential total matches a recorded residential series: Australian Energy Statistics
  Table F, Victoria, residential electricity (FY1974-FY2019, 2020 edition), checked against AEMO's residential
  estimates where they exist. Decision D3.
- Business use = total minus residential, so the residential share becomes a yearly series that the data sets, not a
  constant. The shares used are reported.
- Rooftop output is first used by the household (self-consumption) and the rest exported; household grid use =
  household use minus self-consumption.

This also sets the calibration: the savings in section 3 depend on how much a household uses and when.

## 6. Calibration and validation (pattern-oriented)

- Calibrate `a`, `b`, `k`, `PB0` and the renter, flat and low-income factors to APVI monthly installations and kW in
  Victoria, 2007-2015; test on 2016-2019 (out of sample).
- Validate the patterns the calibration did not use (Grimm et al. 2005): adoption by tenure, dwelling and income
  against ABS 4602.0.55.001 (2014) and the ABS Survey of Income and Housing 2015-16 (used by Best et al. 2019); the
  share of households with PV in 2019; and, if postcode geography is added later, APVI postcode penetration against
  SEIFA.
- The existing validation statistics (wholesale price, generation mix, emissions, tariffs) must stay within their
  current ranges; any change is reported.

## 7. Indicators at each SPM level

The SPM nesting is recursive (`Spm.computeIndicators(state, consumption, recursionLevel)`), but today the recursion
only builds one number, the emission intensity seen from the household, and every household block has its own copy of
the chain, so a "sub-distribution" SPM is not a place.

- Now (small, with this work): a recursive level report. Each month, walk the tree from every household SPM upward and
  sum, per SPM type (household, DER household, sub-distribution, distribution, transmission, generation): energy
  delivered, local generation, exports to the parent, losses, emissions and cost. This gives household-level and
  network-level indicators from the same run, and the household-level ones by cohort (so by income and tenure).
- Later (future iteration): geography. Make sub-distribution and distribution SPMs places, the five Victorian
  distribution networks (CitiPower, Powercor, Jemena, United Energy, AusNet), each with its own network tariff and its
  cohorts. Adoption differs strongly between networks (regional and outer suburbs higher), so this is where the
  recursive structure pays off: the same indicators at household, network-area and state level, and the regional
  justice question. It needs the census by network area and the network tariffs, so it is left for after this build.

## 8. Decisions for Angela

- D1 Cohorts: the EVA's 48 profiles x tenure (3) x income quintile (5) = 720 cohorts (recommended), or fewer.
- D2 Cost shift: network component as revenue / simulated grid kWh, as a switch (recommended), or keep the recorded
  c/kWh (no feedback from adoption to other households' bills).
- D3 Residential series: Australian Energy Statistics Table F (long, 1999-2019, recommended) with AEMO as a check.
- D4 Self-consumption shares by household type: literature values fixed (recommended), or calibrated.

Decided (13 Nov 2026): D1 720 cohorts; D2 the revenue-recovery network component, as a switch; D3 Australian Energy
Statistics Table F with AEMO as a check; D4 fixed literature values for self-consumption.

## 8a. The cost-shift debate since Nelson, Simshauser and Kelley (2011), and what the model can add

Positions:

- **Regressive transfer.** Nelson, Simshauser and Kelley (2011) and Nelson, Simshauser and Nelson (2012, Queensland)
  argue that premium feed-in tariffs and volumetric network charges move costs from adopters to non-adopters, who
  are poorer, and that the merit-order effect of rooftop PV is transient and not a welfare gain. Simshauser (2016)
  extends this to networks: costs follow the evening peak, solar output does not, so volumetric charges let adopters
  avoid network costs they still cause; demand (capacity) tariffs remove the transfer.
- **The merit-order offset.** McConnell et al. (2013, Energy Policy 58) model the NEM retrospectively: rooftop PV
  lowers wholesale prices, mostly at summer peaks, and over their period the saving to all consumers offsets the
  cost of the feed-in tariffs, so the tariffs need not be regressive. Their model leaves out distribution costs, which
  is the other half of the argument.
- **Who adopts is not simply the rich.** The income gradient is weaker and less stable than the 2011 argument
  assumes. Best and Chareunsy (2022, Energy Economics 112): income is negatively related to uptake in area data and
  positively in household data, and the positive link fades with controls; uptake falls at the highest incomes; wealth,
  tenure and dwelling matter more than income. Their review of Australian studies counts positive, negative and
  inconclusive income effects in similar numbers. Best, Burke and Nishitateno (2019): wealth and tenure. Best,
  Chareunsy and Li (2021, Ecological Economics 180) still find inequity in both the feed-in tariffs and the SRES.
  The dividing line is ownership and housing more than income: renters and flat-dwellers cannot adopt.
- **The other cause of rising bills.** Network charges drove much of the 2008-2013 rise in Australian bills, through
  network investment; the Grattan Institute (Wood, Blowers and Griffiths 2018) estimates up to $20 b of
  over-investment, in NSW, Queensland and Tasmania rather than Victoria. A model that attributes the rise in
  non-adopters' bills to solar alone would overstate the transfer.
- **Rooftop solar reduces hardship for those who have it.** Hammerle and Burke (AJARE, ABS cross-section, instrumental
  variable): PV is associated with a large fall in the probability of energy poverty. Dodd and Nelson (2022): hardship
  customers, more often low-income renters, could cut grid use by about 40 % with PV. The justice question moves from
  "who pays for the subsidy" to "who is excluded from the asset".

The death spiral (falling grid sales -> higher per-kWh charges -> more adoption -> ...):

- Named for the NEM by Simshauser (2014, Australian Economic Review 47(4), "From first place to last").
- Later work finds it needs a "perfect storm" of high intrinsic adoption, rising utility costs and favourable customer
  finances (Laws et al. 2017, Applied Energy 185), and that it can be avoided with tariff and regulatory interventions
  (Castaneda et al. 2017, Energy Policy 110, system dynamics, Colombia).
- The Australian responses were tariff reform, not a collapse: the AEMC's distribution network pricing rule
  (November 2014, in force 1 December 2014) required cost-reflective network tariffs and tariff structure statements,
  hence demand and time-of-use network tariffs; Passey et al. (2017, Energy Policy 109) show that demand charges are
  only cost-reflective if they are measured at the network's peak. After the ERSS period, the AEMC's DER access and
  pricing rule (12 August 2021) allowed export (two-way) tariffs (AER export tariff guidelines, May 2022), and
  feed-in tariffs fell to near the wholesale value. These post-2020 changes are for the future-scenarios article.

What the model can add (the ABM contribution, sections 4 and 8 D2): it has, in one run, the merit-order effect
(rooftop output taken off the wholesale demand, so the price falls), the feed-in tariff cost, the network cost
recovery and the adoption decision by tenure, dwelling and income. It can therefore report the net transfer to and
from non-adopters by group, year by year, as the sum of the three terms instead of taking one side, and test whether
the 2009-2019 feedback loop came near a spiral in Victoria (switch on and off). Two things to check before the build:
the form of price control of the Victorian distributors over 2001-2019 (a revenue cap passes volume loss to the
per-kWh charge, a price cap leaves it with the network), and the volume of premium feed-in tariff payments and how
they were recovered (distributors' pass-through).

## 9. Steps

1. Data: census cohorts with tenure and income; residential series; system prices, rebates, certificates, feed-in
   tariffs (`INPUTS.md` rows and build scripts); consumption benchmarks.
2. Residential demand by profile (section 5), with the old constant as a switch; report the change in the validation
   statistics.
3. Cohort agents and the adoption rule (sections 2-3), behind a switch (`rooftopAdoption: recorded | endogenous`);
   the recorded mode stays the base case until the calibration is accepted.
4. Calibration and validation (section 6).
5. Level report (section 7).
6. Cost-shift switch (D2) and the justice outputs: adoption and bills by income and tenure, with and without PV.

## References

- Best, R., Burke, P.J., Nishitateno, S. (2019). Understanding the determinants of rooftop solar installation:
  evidence from household surveys in Australia. Australian Journal of Agricultural and Resource Economics 63(4), 922-939.
- Grimm, V. et al. (2005). Pattern-oriented modeling of agent-based complex systems. Science 310, 987-991.
- Macintosh, A., Wilkinson, D. (2011). Searching for public benefits in solar subsidies: a case study on the
  Australian government's residential photovoltaic rebate program. Energy Policy 39(6), 3199-3209.
- McCarthy, B., Liu, H.-B. (2022). Australasian Journal of Environmental Management (household rooftop solar, regional
  city survey).
- Nelson, T., Simshauser, P., Kelley, S. (2011). Australian residential solar feed-in tariffs: industry stimulus or
  regressive form of taxation? Economic Analysis and Policy 41(2), 113-129.
- Palmer, J., Sorda, G., Madlener, R. (2015). Modeling the diffusion of residential photovoltaic systems in Italy: an
  agent-based simulation. Technological Forecasting and Social Change 99, 106-131.
- Rai, V., Robinson, S.A. (2015). Agent-based modeling of energy technology adoption: empirical integration of social,
  behavioral, economic, and environmental factors. Environmental Modelling and Software 70, 163-177.
- Robinson, S.A., Rai, V. (2015). Determinants of spatio-temporal patterns of energy technology adoption: an
  agent-based modeling approach. Applied Energy 151, 273-284.
- Simpson, G., Clifton, J. (2016). Subsidies for residential solar photovoltaic energy systems in Western Australia:
  distributional, procedural and outcome justice. Renewable and Sustainable Energy Reviews 65, 262-273.
- Simpson, G., Clifton, J. (2017). Testing Diffusion of Innovations Theory with data: financial incentives, early
  adopters, and distributed solar energy in Australia. Energy Research and Social Science 29, 12-22.

Page numbers and volumes not yet checked against the papers are to be confirmed when the PDFs are saved to
`Sandbox/papers/GR4SPArticle_ERSS/`.
- Best, R., Chareunsy, A. (2022). The impact of income on household solar panel uptake: exploring diverse results
  using Australian data. Energy Economics 112, 106124.
- Best, R., Chareunsy, A., Li, H. (2021). Equity and effectiveness of Australian small-scale solar schemes.
  Ecological Economics 180, 106890.
- Castaneda, M. et al. (2017). Myths and facts of the utility death spiral. Energy Policy 110, 105-116.
- Dodd, T., Nelson, T. (2022). Australian household adoption of solar photovoltaics: a comparative study of hardship
  and non-hardship customers (journal to confirm).
- Hammerle, M., Burke, P.J. Solar PV and energy poverty (AJARE; year to confirm).
- Laws, N.D. et al. (2017). On the utility death spiral and the impact of utility rate structures on the adoption of
  residential solar photovoltaics and energy storage. Applied Energy 185, 627-641.
- McConnell, D. et al. (2013). Retrospective modeling of the merit-order effect on wholesale electricity prices from
  distributed photovoltaic generation in the Australian National Electricity Market. Energy Policy 58, 17-27.
- Nelson, T., Simshauser, P., Nelson, J. (2012). Queensland solar feed-in tariffs and the merit-order effect: economic
  benefit, or regressive taxation and wealth transfers? Economic Analysis and Policy 42(3), 277-301.
- Passey, R., Haghdadi, N., Bruce, A., MacGill, I. (2017). Designing more cost reflective electricity network tariffs
  with demand charges. Energy Policy 109, 642-649.
- Simshauser, P. (2014). From first place to last: the National Electricity Market's policy-induced "energy market
  death spiral". Australian Economic Review 47(4), 540-562.
- Simshauser, P. (2016). Distribution network prices and solar PV: resolving rate instability and wealth transfers
  through demand tariffs. Energy Economics 54, 108-122.
- Wood, T., Blowers, D., Griffiths, K. (2018). Down to the wire: a sustainable electricity network for Australia.
  Grattan Institute.
- AEMC (2014). Distribution network pricing arrangements, final determination, 27 November 2014. AEMC (2021). Access,
  pricing and incentive arrangements for distributed energy resources, final determination, 12 August 2021.
