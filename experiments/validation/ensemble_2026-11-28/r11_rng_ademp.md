# R11 points 5 and 6: the random-number generator, and an ADEMP summary (28 Nov 2026)

Code read at `gr4sp` `ef0fe81` (branch `repair/r10-fixes`); MASON 19 (`libraries/mason.19.jar`, read with `javap`).

## 5. Random-number generator

- **Generator.** One `ec.util.MersenneTwisterFast` per run: MASON's `SimState(long seed)` builds
  `new MersenneTwisterFast(seed)` (bytecode of `sim.engine.SimState.<init>(J)`). `Gr4spSim(long seed)` calls
  `super(seed)` (`src/core/Gr4spSim.java:230-232`).
- **Seeding.** Each run is one JVM started with `-seed N` (`seed_ensemble.py`, `run`); MASON's `doLoop`
  (`Gr4spSim.java:1302`) passes N to that constructor and runs one job (stdout "Job 0"). Without `-seed`, `doLoop` would
  seed from the clock (`System.currentTimeMillis` in `SimState.doLoop`); every validation run passes `-seed`. No
  submodel reseeds.
- **What draws from it (base case).**
  - Forced outages: `Arena` calls `g.updateOutage(currentTime, data.random)` for every active generator each
    half-hour (`src/core/Relationships/Arena.java:663-664`); `Generator.updateOutage` draws a breakdown with probability
    p for each unit in service and, on a breakdown, an exponential duration (`src/core/Technical/Generator.java:500-516`).
    Draws are taken only for units in service, so the sequence depends on the outage history and on the set and order of
    active generators, not on prices or dispatch.
  - The schedule: MASON's `Schedule` shuffles the steps that share a time and an ordering with the same generator
    (`Bag.shuffle(MersenneTwisterFast)` in `sim.engine.Schedule`). In GR4SP: `SimPolicies` and the arenas share ordering 0
    (`Gr4spSim.java:1204, 1209`), the household actors ordering 1 (`Gr4spSim.java:604`), `SaveData` ordering 2
    (`Gr4spSim.java:1205`). The ordering-0 shuffle is the random order of `SimPolicies` and `Arena` each month (R6).
- **Generators that are created but never drawn from in the base case.** `java.util.Random` objects without a seed:
  `SimPolicies.random` (`src/core/Policies/SimPolicies.java:18, 23`), never used; `Arena.randomGenerator`
  (`Arena.java:55, 80`), used only by the `RND` retail-tariff choice (`Arena.java:197-199`), while the base case sets
  `endConsumerTariff: AVG` (`simulationSettings/VIC.yaml:228`, applied at `Gr4spSim.java:508`). Endogenous rooftop
  adoption, which is not stochastic either, is off (`rooftopAdoption: recorded`, `VIC.yaml:83`). That the runs are
  reproducible byte for byte from the seed (e.g. seed 42 and seed 1 of this ensemble against the test runs) is
  consistent with this.
- **Shared streams.** None: each of the 100 runs is a separate process with its own generator, seeded 1 to 100. The
  streams of consecutive seeds are not independent by proof, but MersenneTwisterFast initialises its state from the
  seed through a scrambling function, and no run reads another's stream.
- **Paired seeds for R8 (point 4, reasoning before the check).** A variant uses the same stream as the base case of the
  same seed if it has the same active generators, in the same order, and the same steps in the schedule: then the
  outage draws and the shuffle draws are the same, whatever the offers. The R8 variants change offer rules only, so they
  should be paired in 1999-2019. To be checked on seed 1 when R8 runs (outage histories compared unit by unit and
  half-hour by half-hour), as R11 asks.

## 6. ADEMP summary (Morris, White and Crowther 2019, Table 1), from the repository's side

| ADEMP element | GR4SP validation |
|---|---|
| Aims | Hindcast of the Victorian electricity system, January 1999 to June 2020, conditional on the recorded inputs (demand, fuel and carbon prices, plant entry and closure, rooftop solar installations, monthly hydro energy): does the base case reproduce independent records of prices, tariffs, renewable share, emissions, generation by fuel and emissions intensity? |
| Data-generating mechanism | The base case of `v0.3.4` (settings `simulationSettings/`, database DB-2021-8-21) run for seeds 1-100. The seed drives the forced outages of coal, gas and hydro units and the order of steps that share an ordering in the schedule (point 5). Parameters are fixed: parameter uncertainty is studied separately (Morris screening, Sobol). |
| Estimands / targets | The recorded series: AEMO VIC1 monthly price (time-weighted; demand-weighted as a check), OpenNEM renewable share and generation by fuel, Oakley Greenwood and St Vincent de Paul tariffs, the Victorian inventory's emissions (July-June), NGA scope 2 (plus network losses) and AEMO CDEII intensities, and AEMO half-hourly prices for the duration curves. |
| Methods | The base case (one method). Variants run on paired seeds: the price events (no carbon price, Hazelwood open, gas at its 2015 price; 21 seeds) and, once run, the R8 offer-rule variants L1 and L2 (100 seeds). |
| Performance measures | NSE, bias, RMSE and r per seed (MAE and NRMSE in the supplement); reported as the median and the 5th-95th percentile over the seeds, with the bootstrap Monte Carlo SE of each (`r11_mc_error.md`, B = 2000); coverage of the record by the 5-95 % seed band (`r11_mc_error.md` section 3). |
