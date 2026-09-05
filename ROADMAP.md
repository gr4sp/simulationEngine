# GR4SP model roadmap

Proposed changes to the model itself, recorded as they are identified so that the
evolution of GR4SP carries them forward. Each entry states what the code does now,
what it should do, and why, with file and line references to the behaviour as found.

Items here are *not* defects unless marked as such. They are capabilities the
architecture already implies but the implementation does not yet deliver.

Item 9 records the two links that were designed, and whose data and classes exist,
but that were never wired up for time reasons. They are the first things to pick up.

**Before landing any of these, see [`docs/versioning.md`](docs/versioning.md).** It
classifies every entry below by whether it can change without moving the numbers the
methods article reports, and proposes how the frozen article calibration and the
updated model live in one codebase.

---

## 1. Report indicators at every level of the SPM nesting, not only at the root

**Status:** open. Identified 22 August 2026 while checking claims for the EMS article.

### What the code does now

`Spm.computeIndicators` computes an emissions intensity for *every* SPM. Each SPM
applies its own network loss factor and adds the intensity of the SPMs it contains
(`src/core/Technical/Spm.java:336-343`), so a distinct `genEmissionIntensityIndex`
exists at the generation gate, after transmission, after distribution, after
sub-distribution, and as delivered.

The emissions **mass**, however, is computed only at the root of the recursion:

```java
if (recursionLevel == 0) {
    ...
    this.currentEmissions = consumption * this.getGenEmissionIntensityIndex();
}
```
(`src/core/Technical/Spm.java:349-361`)

Consumption enters the recursion only at the SPM the `EndUserUnit` is attached to
(`src/core/Social/EndUserUnit.java:243`), and every contained SPM keeps
`currentEmissions = 0`.

**Consequence, and this one is a defect:** `src/core/SaveData.java:632-643` iterates
`spm_register` and writes a per-SPM emissions series for every SPM instance. For all
SPMs below the root that series is a column of zeros. In the Victorian configuration
that is SPMs 6, 5, 7 and 9 â€” four of the five.

### What it should do

Report a mass at each level, so that the recursive quantification can be read at
whatever level of analysis a question needs.

Two routes, in increasing order of effort and value:

1. **Attribute delivered consumption at each level.** Multiply each SPM's own
   `genEmissionIntensityIndex` by the consumption that passes through it. Because
   each level's intensity already carries the compounded loss factors of the levels
   inside it, the differences between levels decompose the delivery losses. Cheap,
   and it makes the existing per-SPM series meaningful instead of zero.
2. **Attach consumption to SPMs.** Let an SPM know the demand of the consumers
   nested within it, rather than receiving a single `consumption` argument from the
   root. This is the structurally correct version: a street, a feeder or a
   building SPM would then report its own total, because the recursion roots there.
   It is what makes per-scale analysis real rather than notional.

Route 2 also removes the `recursionLevel == 0` special case, which currently makes
the meaning of `currentEmissions` depend on where a call started.

### Also to extend

The renewable share and generation autonomy indicators are defined recursively over
the same nesting (Section 4 of the EMS article). Whatever consumption-attribution
rule is chosen for emissions should be applied to them at the same time, so that all
three indicators are readable at the same set of levels.

### Why it matters

The claim that a single run reports indicators at whatever level an SPM is defined
is the cross-scale contribution of the model. Today it holds for intensities and for
whichever single level carries the consumption. Closing this gap makes it hold for
totals as well, and would let one run answer questions at the state, network and
neighbourhood levels at once.

### Related configuration note, not a code change

The Victorian case is configured at maximum aggregation:
`simulationSettings/VIC.yaml:50` sets `maxHouseholdsPerConsumerUnit: 2147483647`,
so all Victorian households form a single `EndUserUnit` on a single SPM. Lowering
that value already produces multiple end-user units, each rooting its own recursion.
The `spm` table also defines types that no run instantiates: `2 DER generation`,
`3 DER generation and battery`, `4 off-grid + battery`, `8 Industry`,
`10/12 (+ battery)`. Exercising those types is a configuration and data exercise,
not a code change, and is a natural companion to the work above.

---

## 2. The OTC arena is instantiated, populated with the wrong contracts, and never read

**Status:** open. Identified 28 August 2026 while resolving a contradiction between the
EMS article and its ODD supplement.

### What the code does now

The `arenas` table defines three arenas for the Victorian case:

| id | name | type |
|---|---|---|
| 1 | Bulk | OTC |
| 2 | Retail household | Retail |
| 3 | Spot | Spot |

All three are constructed (`LoadData.java:333`). `Arena.step()` then acts on only one
of them: the entire body of the method is inside
`if (type.equalsIgnoreCase("Spot"))`, so the Retail and OTC arenas have no step
behaviour at all. Retail is still used, because `EndUserUnit` queries it on demand
for tariffs (`EndUserUnit.java:109-117`).

The OTC arena is not queried by anything. It is, however, filled with contracts:

```java
if (arena.getType().equalsIgnoreCase("OTC") || arena.getType().equalsIgnoreCase("Retail")) {
    arena.getBilateral().add(contract);
}
```
(`LoadData.java:436-438`)

Those contracts come from `tariffshistoric` and are all built with
`Arena.EndConsumer` (999) as the buyer â€” household retail tariffs. An OTC arena is
meant to hold swaps, caps and PPAs between registered market participants, not
end-consumer tariffs.

So the Bulk arena is constructed, loaded with contracts of the wrong kind, and never
consulted. **No output depends on it**, which is why this has gone unnoticed.

### Why it matters

Three reasons, in increasing order of importance:

1. **It is misleading to a reader of the code.** The arena registry suggests
   over-the-counter trade is represented. It is not.
2. **It wastes the load.** Every historical tariff contract is instantiated twice.
3. **It documents an intention that was never implemented, and that intention leaked
   into the papers.** A code comment at `Generator.java:231-233` says the minimum
   capacity factor "makes the assumption that the capacity factor didn't fall below
   that threshold by selling through OTCs instead of SPOT". Both the EMS article and
   its ODD had inherited that story and stated that utility-scale hedging enters the
   model through the MinCF floor. It does not. The thesis motivates MinCF technically
   (`chap5.tex:185`: the operative limits of the technology, plant deterioration and
   cycling costs), and the parameter values agree â€” brown coal 0.47, wind 0.28. Both
   documents were corrected on 28 August 2026.

### What it should do

Two routes, depending on intent:

1. **If OTC is to stay unexercised:** do not load contracts into it. Restrict the
   branch at `LoadData.java:436-438` to `Retail`, and either drop the `Bulk` row from
   the `arenas` table or keep it as a declared-but-empty arena. Cheapest, and it makes
   the registry honest.
2. **If OTC is to be exercised:** give it its own contract source. An OTC arena needs
   contracts between two named market participants with a strike price and a term, not
   `EndConsumer` tariffs, and it needs a settlement rule â€” a contract for difference
   against the spot price is the standard form in the NEM. This is the route that would
   let the suite say something about hedging and investment timing, which the article
   currently lists as outside its scope.

Note that route 2 changes what the model can claim, not just how it is coded. Physical
bilateral supply at small scale is already represented, through the off-market
treatment (`Arena.java:607-609`, priced at BasePrice/MaxCF); what is missing is
financial hedging at utility scale.

---

## 3. Solar never reaches the market: `solarSurplusCapacity` is declared but never assigned

**Status:** open, **and this one is a defect.** Identified 3 September 2026 while
checking the SPM claims for the EMS article.

### What the code does now

`Generator.solarSurplusCapacity` is declared (`src/core/Technical/Generator.java:39`)
and read in two places, but **nothing ever writes to it.** The two lines that would
compute it are commented out inside `computeAvailableSolarCapacity`:

```java
//solarSurplusCapacity = availableCapacity - (numUnits * consumption / 1440 );
//if(solarSurplusCapacity < 0 ) solarSurplusCapacity = 0;
```
(`src/core/Technical/Generator.java:307-309`)

There is no setter. The field therefore holds `0.0` for every generator for the whole
run. Three consequences follow, none of them signalled at runtime:

1. **Solar never bids.** `createBids` substitutes the surplus for solar's available
   capacity, then skips any bid of zero capacity
   (`src/core/Relationships/Arena.java:290-291`, `:296`). A semi-scheduled solar farm
   flagged `inPrimaryMarket = true` still contributes no bid.
2. **Solar contributes nothing to the off-market stream.** The off-market branch adds
   `getSolarSurplusCapacity()`, that is zero, to `availableCapacityOffMarket` and to
   the off-market price weighting (`src/core/Relationships/Arena.java:508-510`).
3. **The whole of each solar generator's output is instead netted off demand as
   self-consumption.** `consumptionSuppliedBySolar` accumulates the full half-hourly
   capacity for any generator whose `fuelSourceDescriptor` is `Solar`
   (`src/core/Relationships/Arena.java:497-500`), and that total is subtracted before
   clearing (`:533`). The branch keys on **fuel type, not on market participation**,
   so utility-scale solar above the 30 MW threshold is treated exactly like rooftop PV.

The generation is still recorded (`Arena.java:503`) and still counts in the renewable
share (`src/core/SaveData.java:753-754`, `:2055-2057`), so the effect is invisible in
the RE indicator and shows up only in wholesale demand, dispatch and price.

### Why it matters

The model's account of distributed generation rests on a self-consumption/export
split that is not actually computed. Every solar unit behaves as if 100 % of its
output were consumed behind the meter, which understates the demand reaching the
wholesale market and removes utility solar from price formation altogether. The
effect on the 1998-2019 validation window is small, because Victoria's first
large-scale solar farms date from about 2017, but it grows with every year of the
forecast horizon and with any scenario that raises solar penetration.

**It also means the documentation describes a design that does not run.** The EMS
supplement states that "for rooftop solar, only the surplus after household
self-consumption enters this calculation" (`supplement_ODD.tex:203`) and lists the
variable in the entity table (`:120`). Both describe the intent.

### What it should do

1. **Restore the computation, and give it a real basis.** The commented-out line
   divides monthly consumption by 1,440 half-hours, a flat profile. Self-consumption
   depends on the coincidence of the household load shape with the solar profile, so
   a half-hourly load shape, or at minimum a self-consumption fraction as a settings
   parameter, would be a defensible replacement.
2. **Separate the two branches by market participation, not fuel type.** Utility-scale
   solar should bid its available capacity like any other semi-scheduled generator;
   only behind-the-meter PV should be netted off demand.
3. **Assert rather than fail silently.** A generator that reaches the bidding step
   with zero available capacity, when its nameplate and capacity factor are both
   positive, should log a warning.

---

## 4. No demand below the root SPM, so no local balancing, no islanding, and surplus is discarded

**Status:** open. Identified 3 September 2026. Extends item 1, and should be planned
with it.

### What the code does now

`Spm.computeIndicators` receives a single `consumption` argument and passes **the same
value** to every SPM it recurses into (`src/core/Technical/Spm.java:273`, `:342`).
What travels upward is an emissions intensity, each level scaled by its own
`1/(1-losses)` factor. **No SPM below the root has any demand of its own**, so no SPM
balances the generation it contains against the load it serves.

The balancing that does happen is done once, in the Arena, over a flat list of
generators gathered from every SPM (`src/core/Relationships/Arena.java:436-443`), and
subtracted from system demand at `:533`.

Surplus has nowhere to go. Two independent clamps discard it:

```java
//If non scheduled covered more than the demand, set the demand of the wholesale to 0
if(totalDemandWholesale < 0.0 ) totalDemandWholesale = 0.0;
```
(`src/core/Relationships/Arena.java:538-539`)

```java
//If onsite Generation is greater than consumption, set consumption to 0
//Because we have not created a market to sell the surplus to other SPMs
if(consumption < 0) consumption = 0;
```
(`src/core/Social/EndUserUnit.java:236-240`)

The second comment states the gap exactly. There is also no connectivity state
anywhere: network assets carry a loss factor and nothing else, so there is no notion
of a connection that could be opened.

### Why it matters

Three capabilities the architecture implies but does not deliver:

1. **Islanded operation.** A microgrid or off-grid SPM cannot be disconnected and run
   against its own load, because it has no own load and no connection state. The
   `spm` table already defines the types that would need it (`4 off-grid + battery`,
   `2/3 DER generation`), and none is instantiated.
2. **Export between SPMs.** Local surplus is neither exported nor curtailed, it is
   silently dropped. A neighbourhood that generates more than it consumes reports the
   same result as one that generates exactly what it consumes.
3. **Consistency between generation and demand placement.** Because demand is
   exogenous and is never decomposed by SPM, the model cannot check that a generator
   and the load it serves belong to the same place. Adding a generator whose demand
   is not in the exogenous series silently reduces wholesale demand and raises the
   renewable share, with nothing to flag it.

### What it should do

Route 2 of item 1, attaching consumption to SPMs, is the precondition for all three.
On top of it:

- give each SPM a balance step that nets its own generation against its own demand and
  passes a signed residual to its parent, replacing the two clamps with an explicit
  export or curtailment rule;
- add a connection state to the SPM interface (`ConnectionPoint`) so an SPM can be
  islanded, with unmet demand recorded locally rather than passed up;
- add an initialisation check that every generator's SPM lies on a path to an SPM
  carrying demand.

Only the first is needed to make the current claims exact. The second is what would
let GR4SP answer questions about local energy systems, which is the direction the
`spm` table's unused types already anticipate.

---

## 5. Actors are loaded, then used for almost nothing

**Status:** open, **and the disabled loaders are a defect.** Identified
3 September 2026 while checking the EMS article's data-inputs section.

### What the code does now

`selectActors` loads the `actors` table filtered by region
(`src/core/LoadData.java:1337-1338`), giving each actor a name, registration and
change dates, registration number, role and business structure. For the Victorian
case that is **486 of the 925 rows** in the table.

Both relationship loaders are then commented out:

```java
//selectActorActorRelationships("actoractor93");
//LoadData.selectActorAssetRelationships(this, "actorasset");
```
(`src/core/Gr4spSim.java:418`, `:420`)

`selectActorAssetRelationships` is fully written (`src/core/LoadData.java:1386-1460`)
and handles generation, network and SPM assets. The `actorasset` table holds **408
rows**, almost all `OWN` at 100 %, against generation assets. **None of it is read.**

The only actor-asset relationship that exists at runtime is created in code: each
household `EndUserUnit` is given a `USE` relation to SPM 1
(`src/core/Gr4spSim.java:375-380`).

So the 486 loaded actors have no portfolios, no relationships to each other, and no
behaviour. They are consumed in exactly one place, a count of how many are active at
each step (`src/core/SaveData.java:648-655`). **Generator ownership survives only as
the `owner_name` string attribute carried on the generator itself**, and it is
generators, not actors, that bid.

Two further faults visible in the data:

- **`change_date` is a sentinel for anything still active.** The SECV row runs
  `1921-01-10` to `2050-10-16`, so it counts as an active actor for the whole run,
  decades after it was disaggregated.
- **The region filter drops actors the analysis names.** Snowy Hydro, Origin and
  several AGL entities are registered `NSW` in the table and are therefore not loaded
  for a Victorian run, though they operate Victorian plant.

### Why it matters

Actors are one of the three elements of the socio-technical vocabulary the suite is
built on, alongside SPMs and arenas. At present the actor layer is closer to a
registry than a model: it can say how many organisations existed in a given year, and
nothing about who owned what or who traded with whom. Any question about ownership
concentration, market power, or the effect of privatisation on portfolios needs the
loaders switched back on.

### What it should do

1. **Re-enable `selectActorAssetRelationships` against `actorasset` -- but not
   before remapping its keys.** See the blocker below: the table's `actorid` column
   does not reference the `actors` table the model loads. Once remapped, this makes
   portfolios real and lets ownership be reported per actor.

   ### BLOCKER: `actorasset.actorid` references `actors_old`, not `actors`

   Checked 3 September 2026 against `backupDB/DB-2026-08-11.sql`. Of the 57 distinct
   `actorid` values in `actorasset`, matching each row's own `actorname` against the
   name the id resolves to:

   | resolved against | matches | mismatches | id absent |
   |---|---|---|---|
   | `actors` (what the loader uses) | 13 | 43 | 1 |
   | `actors_old` | **54** | 2 | 1 |

   Examples of what the loader would do today:

   | id | `actorasset.actorname` | `actors_old` (correct) | `actors` (what it would attach to) |
   |---|---|---|---|
   | 214 | Pacific Hydro Investments Pty Ltd | Pacific Hydro Investments Pty Ltd | National Power UK (Hazelwood Partner) |
   | 215 | AGL Energy | AGL Energy | Pacific Corp USA (Hazelwood Partner) |
   | 244 | Snowy Hydro Ltd | Snowy Hydro Ltd | ERM Power Retail Pty Ltd |
   | 248 | SP AusNet | SP AusNet | LUMO ENERGY AUSTRALIA PTY LTD |

   So enabling the loader unchanged would **silently attach Victorian generation and
   network assets to the wrong organisations**, with no error raised: the loader
   simply queries `actorasset WHERE actorid = <actors.id>` and takes what comes back.
   `actorid` 0 (`TBA`, 24 rows) exists in neither table.

   The fix is a data migration, not a code change: rebuild `actorasset` with
   `actors` ids, matching on name against `actors_old` and resolving the two
   mismatches and the `TBA` rows by hand. Add a foreign key afterwards so the two
   tables cannot drift again. Note the actor--actor source
   (`experiments/simulationData/ActorActorRel_V08.csv`) **is** keyed to the current
   `actors` ids, so only the asset side needs migrating.

   Two further facts worth knowing before the work is scoped:

   - `actorasset` holds **160 Generation and 248 Network asset rows, and no SPM
     rows.** Because `EndUserUnit.step` reacts only to relations whose asset is an
     SPM (`src/core/Social/EndUserUnit.java:229-231`), loading the table changes no
     model output today. It populates the object graph and nothing reads it.
   - The table is a **public-regime dataset.** The SECV alone holds 129 of the 408
     rows and SP AusNet a further 89. AGL Loy Yang, EnergyAustralia Pty Ltd,
     EnergyAustralia Yallourn and AGL Electricity hold **zero rows each**, so the
     private-regime portfolios are not in the data at all.
2. **Decide what `actoractor93` is for, then either load it or remove it.** A
   commented-out call to a table with a year in its name is the kind of thing that
   reads as an implemented capability and is not one.
3. **Replace the `change_date` sentinel** with a null, and test for null rather than
   comparing against a date in 2050.
4. **Filter actors by the assets they hold, not only by their registered region,** so
   that an interstate-registered company operating Victorian plant is loaded.
5. **Let actors bid.** This is the largest of the five and changes what the model can
   claim: today a generator bids by rule from its own settings, and an actor holding a
   portfolio would be the natural place for any strategic or portfolio-level behaviour.
   The EMS article is explicit that bidding is non-strategic by design, so this is a
   change of scope rather than a fix.

---

## 6. Scope limits carried from the EMS article

**Status:** documented, not defects. These are deliberate simplifications recorded in
Section 6 of the EMS article, collected here so that the roadmap holds the full set.
Each would be a substantial piece of work and each changes what the suite can claim.

| Limit | Where it lives now | What lifting it would need |
|---|---|---|
| **Networks are parameters.** Loss factors and access charges only, so no congestion, locational pricing or hosting-capacity limits | `Spm.computeNetworksLosses`; `networkassets` table | Network assets are already a class in the SPM structure, so a power-flow or transport model could attach there. Needs topology, which the `ConnectionPoint` class holds only nominally |
| **No ancillary services.** FCAS is absent | -- | A further arena type with its own clearing rule, alongside the Spot arena |
| **The market price cap is a static calibration value**, not the NEM's actual schedule | `VIC.yaml`, `marketPriceCap` per technology | A dated schedule read from settings, which is a small change with a real effect on scarcity-pricing questions |
| **Demand is exogenous** and never responds to price | `monthly_consumption_register`, `total_demand_halfhour` | Demand response is the obvious first step and needs item 4's per-SPM demand first |
| **Bidding is non-strategic.** Every bid is `BasePrice/CF` from settings, so the same inputs always give the same bid | `Generator.priceMWhLCOE` | See item 5.5. Note this is a design choice the article defends, not an oversight |
| **The feed-in tariff arena is never constructed.** `Arena` supports a `fiTs` type, but the `arenas` table has no row of that type | `Arena.java:68-69`; `arenas` table, 3 rows | Data first: a feed-in tariff series, and a rule for which exports are remunerated. Pairs naturally with item 3 |
| **The secondary spot market is implemented but not configured for Victoria.** It is a hypothetical arena, correctly described as such in the supplement | `Arena.java:76`, `:366-370`; `VIC.yaml:121-130` sets no `secondary` | Nothing to build. It is exercised by configuration alone |

---

## 7. Include an actor whose assets operate in the modelled market, whatever its region of registration

**Status:** open. Raised by Angela, 3 September 2026.

### What the code does now

`selectActors` filters on the actor's own registered region:

```java
"SELECT ... FROM actors WHERE region = '" + data.settings.getAreaCode() + "';"
```
(`src/core/LoadData.java:1337-1338`)

For the Victorian case that loads **486 of the 925 rows**. Region here is the
region recorded against the organisation, not the region its plant operates in.

### Why it matters

Companies routinely register in one NEM region and operate generation in another.
Of the 58 actors that hold rows in `actorasset`, **21 are dropped by the region
filter**, and they are not marginal ones:

| Actor | Registered | Victorian involvement |
|---|---|---|
| National Power UK, International Power Hazelwood, Great Energy Alliance | OTHER / NSW | Hazelwood partners |
| Duke Energy Bairnsdale Operations | NSW | Bairnsdale |
| Alinta DEBO, Alinta Energy Finance | NSW | Victorian plant and retail |
| AGL Energy Services, AGL Energy Sales & Marketing | NSW | Victorian generation and retail |
| Snowy Hydro Ltd | NSW | dispatches into Victoria |
| Hydro-Electric Corporation (Tasmania) | TAS | Basslink counterparty |

The EMS article names AGL, Origin, EnergyAustralia and Snowy Hydro as the companies
that took the SECV's place. **Origin and Snowy Hydro are NSW-registered rows and a
Victorian run does not load either.**

### What it should do

Select an actor if **either** its registered region is the modelled region **or** it
holds a relationship to an asset located in it. Concretely, replace the single
region predicate with a union: the current query, plus actors reachable through
`actorasset` from assets whose own region matches. That requires item 5's key
migration first, since the join is only sound once `actorasset` points at `actors`.

Keep the registered region as a separate reported attribute. Where an organisation
is registered is a real fact about it, and worth distinguishing from where it
operates -- the distinction is itself part of the ownership story after
privatisation, when interstate and overseas capital entered the Victorian system.

---

## 8. Newer AEMO source data and documentation now in the repository

**Status:** available, not yet used. Added 3 September 2026.

### CDEII results, 2011 to 2025

`experiments/simulationData/` holds AEMO's published CO2EII summary results as
`co2eii_summary_results_<year>.csv` for **2011--2025**, with 2014 split into
two parts. The series is complete; no year is missing. Each file
carries `CONTRACTYEAR, WEEKNO, SETTLEMENTDATE, REGIONID, TOTAL_SENT_OUT_ENERGY,
TOTAL_EMISSIONS, CO2E_INTENSITY_INDEX`. Alongside them,
`co2eii_available_generators.csv` gives per-DUID emission factors with their
`CO2E_ENERGY_SOURCE` and `CO2E_DATA_SOURCE` (500 rows).

Two uses, both worth taking:

1. **Extend the validation window.** The EMS article validates GHGE to 2018 against
   the Victorian Government series. AEMO's regional intensity index is an
   independent second comparator on the same quantity, and it runs seven years
   further. Note it is an intensity, so it compares against the SE's CDEII rather
   than the emissions mass, and REGIONID must be filtered to VIC1.
2. **Refresh the generator emission factors.** The model's factors come from the
   `generationassets` table; `co2eii_available_generators.csv` carries AEMO's own
   current values per DUID, which would let the seeded factors be checked against
   the published ones rather than assumed.

### AEMO documentation, cited by reference

The following primary AEMO material was consulted on 3 September 2026 for unpacking
the market rules the model encodes. These are third-party publications and are **not
redistributed in this repository** -- they are `.gitignore`d under `docs/`, and are
obtained from AEMO's website:

- **NEM Generation Information, July 2026** (AEMO, *Energy Systems -> Electricity ->
  NEM Forecasting and Planning -> Forecasting and Reliability -> NEM Generation
  Information*) -- the commitment status definitions and the five project commitment
  criteria (Land, Contracts, Planning, Finance, Construction), plus current unit-level
  data. **This is the authority for the `unit_status` values the model filters on.**
  Note the database uses the spelling `Publically Announced` and a status `Emerging`
  that is **not** in AEMO's current set (Publicly Announced, Anticipated, Committed,
  Committed*, In Commissioning, In Service, Announced Withdrawal). Worth reconciling.
- **Generating Unit Expected Closure Year, July 2026** (AEMO, published alongside the
  Generation Information workbook) -- current closure years, against which the model's
  `expected_closure_date` and the brown-coal retirement shift can be checked.
- **CDEII Procedures version 4.1**, effective 9 August 2026 (AEMO) -- see item 11.

And, bearing specifically on the arena work, the **settlement residue auction** pair:

- **Auction Participation Agreement (APA)** (AEMO) -- the executable agreement a party
  signs to bid in the Settlements Residue Auction. It carries the **participation and
  bidding rules**: who may be an auction participant, what units are auctioned, how
  bids are made and settled, and the obligations attaching to a unit once acquired.
- **Guide to NEM Settlements Residue Auction Interface** (AEMO) -- the operational
  companion, covering how auctions are run and results published.

Together these are the source material for a **settlement residue / inter-regional
hedging arena**, which the model does not currently have. `Arena.java` supports
`spot`, `otc` and `fiTs` types; a directional inter-regional right is a fourth kind of
arena with its own participation rule and clearing, and it is the mechanism by which
Victorian participants hedge exposure across the interconnectors. That makes it the
natural pairing with item 6's "networks are parameters" limit -- settlement residues
exist *because* of inter-regional price separation, which the model does not yet
represent. Read the APA for the rules, the interface guide for the mechanics.

Reading all of these against `Arena.java` and `VIC.yaml` is the cheapest available
route to finding where the encoded rules have drifted from the current NER, and the
closure workbook bears directly on the retirement scenarios.

---

## 9. The two deferred links: actor--asset over time, and SPM--demand at each scale

**Status:** open, and these are the priority follow-ups. Recorded 3 September 2026,
from Angela: the design work is done and the infrastructure is present in both cases,
the connection was simply not made before the thesis and the EMS article were
written. Neither is an oversight, and neither is a defect. They are unfinished
wiring, and the article is entitled to describe the capability on that basis.

### 9.1 Actor--asset: ownership, and ownership that changes

**What exists.** The `actorasset` table (408 rows), the `ActorAssetRelationship`
class, a fully written loader (`src/core/LoadData.java:1386-1460`) handling
generation, network and SPM assets, and the relationship taxonomy in `actortype`.

**What is missing, in the order it has to be done.**

1. **The key migration.** `actorasset.actorid` points at `actors_old`, not `actors`.
   This is the blocker in item 5 and nothing else can proceed until it is cleared.
2. **Validity dates on the relation itself.** This is the one that matters for the
   substantive question, because **assets do change hands**, and that is a fact about
   the Victorian system rather than a modelling nuisance: the SECV's plant was broken
   up and sold, Hazelwood passed through several consortia, Loy Yang A changed owner
   more than once. The model cannot express any of it. `ActorAssetRelationship`
   carries only `actor`, `type`, `percentage`, `asset`
   (`src/core/Relationships/ActorAssetRelationship.java:10-13`), and the `actorasset`
   table has **no date columns**. An asset therefore either has a relation or it does
   not, for the whole run. The only time filtering available today acts on the *asset*
   (generators are keyed by operational dates) and on the *actor*
   (`registration_date` / `change_date`), never on the relation between them.

   The fix is a `valid_from` / `valid_to` pair on both the table and the class, and a
   lookup that resolves an asset's owner **at a given date**. That is what turns a
   static ownership register into a representation of divestment, acquisition and
   consolidation.
3. **Actors that hold assets in the region but are registered elsewhere** -- item 7.

**Where the analysis went instead.** The organisational side of this question was
carried through the **Sectoral Network Analysis**, which works from the actor registry
and the actor--actor strategic relationships (ownership, acquisition, merger,
rebranding, management) in
`experiments/simulationData/ActorActorRel_V08.csv`, processed with NetworkX outside
the Java SE. That analysis reaches back to 1880 and traces the consolidation from
hundreds of municipal undertakers to a single monopoly and then to privatised
gentailers. **Note that this CSV is keyed to the current `actors` ids**, so the
actor--actor side does not carry item 5's migration problem; only the asset side does.

So the sector's organisational structure and its change over time *are* represented
and analysed. What is not connected is the join from those organisations to the
physical assets inside the SE, which is what would let ownership concentration be
read against dispatch, emissions or price.

### 9.2 SPM--demand: attaching consumption at each scale

**What exists.** The recursive SPM structure, the recursive indicator functions, the
`EndUserUnit` class with its own consumption, `maxHouseholdsPerConsumerUnit` as the
lever that splits end users into multiple units, and the `spm` table's unused types
(`2 DER generation`, `3 DER generation and battery`, `4 off-grid + battery`,
`8 Industry`, `10/12 (+ battery)`).

**What is missing.** Demand attaches only where an `EndUserUnit` sits, and in the
Victorian configuration that is one unit on one SPM, because
`simulationSettings/VIC.yaml:50` sets `maxHouseholdsPerConsumerUnit` to
`2147483647`. Every SPM below the root therefore computes an intensity but no mass,
cannot balance its own generation against its own load, and cannot be islanded.
This is items 1 and 4, and 9.2 is the name for doing them together.

**Why the two are one piece of work.** Both are the same shape: a relation that the
schema and the classes already anticipate, that no run currently resolves. Doing 9.2
first is the better order, because per-SPM demand is what makes per-actor ownership
worth reading -- knowing who owns a feeder matters once the feeder reports its own
consumption, emissions and renewable share.

### What the EMS article says, and why that is defensible

Section 3.2.2 describes the actor--asset relation as definable for any class of SPM
asset, and Section 3.2.1 describes indicators as computable wherever an SPM is
defined. Both are statements about the architecture, and both are true of it: the
tables, the classes and the loader exist, and enabling them is configuration and data
work rather than redesign. The article should not claim that the Victorian runs
exercise either, and the wording should stay on the capability rather than the run.

---

## 10. The tariff update uses a half-year of prices and a lagged rate, and neither matches the comparator

**Status:** open. Raised by Angela, 3 September 2026, while checking Â§4.4 of the EMS
article. She proposed the diagnosis and the code confirms it.

### What the code does now

The retail tariff is set once a year, in January, and is held flat until the next
January (`src/core/Social/EndUserUnit.java:140`). Two inputs go into it, and both are
drawn from the *previous* calendar year:

1. **The wholesale price** is the mean of the **last six monthly averages**
   (`EndUserUnit.java:174-186`), so a January update reads **July to December of the
   preceding year** and nothing else. January to June of that year never enters, and
   neither does any month of the year the tariff applies to.
2. **The wholesale contribution rate** `R_w` is read for `year = currentYear - 1`
   (`EndUserUnit.java:146-151`), a deliberate one-year lag, because retailers set
   tariffs from costs they have already observed.

So the simulated tariff for calendar year *Y* is
`mean(wholesale price, Julâ€“Dec of Y-1) / R_w[Y-1]`.

The CPI conversion, by contrast, uses the year of the update itself
(`EndUserUnit.java:164-165`), so it is contemporaneous while `R_w` is lagged.

### Why it matters

**The comparators are built on different periods.** The ACCC reports retail cost
stacks by **financial year** (July to June); the St Vincent de Paul series reports
**calendar-year** offers. The model's figure is a **second-half-of-the-previous-year**
average. None of the three shares a period with the other two.

The retail tariff is the weakest of the four validated indicators, and the two
comparator sources disagree with each other by more than the simulation differs from
either (EMS article, Table 4). **A timing-basis mismatch of six to twelve months is a
credible contributor to that, and it has never been tested.** It is cheap to test:
recompute the simulated tariff on a July-to-June basis and on a full-calendar-year
basis, and see how the statistics move.

### âš ï¸ An open question about the year labels themselves

`historic_tariff_contribution` is keyed by a bare `year` integer, 22 rows covering
**1999 to 2020 with no gaps**, and the code treats that key as a **calendar year**.
But the underlying ACCC data are financial-year figures. **It is not recorded anywhere
whether the label is the financial year's start year or its end year**, so there may
be a systematic six-month offset in `R_w` on top of the deliberate one-year lag.

Angela's reading is that `2020` may have been entered to mean "the record is complete
through December 2019" rather than "2020 is itself observed". If that is right, the
last genuinely observed year is 2019, and the register carries one row more than it
has data for.

**This needs checking against the original extraction, not against the database**,
because the database preserves no provenance.

**Partly settled, 5 September 2026** -- see
[`experiments/validation/README.md`](experiments/validation/README.md), section
*Provenance of the tariff data*. The comparator's source is a **submission to** the
ACCC inquiry by the Victorian Electricity Distribution Businesses (30 June 2017,
analysis by Oakley Greenwood), **not** the ACCC's own *Retail Electricity Pricing
Inquiry: Final Report* of June 2018. Its Figure 1 covers 1995, 2001-2002 and
2007-2017 only, which is exactly why the comparator has no 2003-2006 and nothing
after 2017.

The register has the opposite problem: **22 rows, 1999-2020, no gaps, but only
1999-2002 and 2006-2016 are genuine.** 1999-2001 repeat the 1999 row verbatim;
2003-2005 are a hand-built ramp, not an interpolation; 2017-2020 have no
recoverable source, and 2017 and 2018 violate the shares-sum-to-one identity by
-5.10 and +9.21 percentage points. The calendar-versus-financial-year question
below is therefore moot for the constructed rows and live only for the genuine
ones.

Superseded note, kept for the record: until it was settled, it was noted that
`historic_tariff_contribution` is attributed in the article to the ACCC's *Retail
Electricity Pricing Inquiry: Final Report* of **June 2018**, which cannot be the
source of the 2019 and 2020 rows â€” those must come from a later ACCC publication.

### What it should do

1. **Record the provenance.** Add a `source` and a `period_start` / `period_end` to
   the table, so a year label can never again be ambiguous between calendar and
   financial years.
2. **Make the averaging window a setting** rather than a hard-coded six, so the
   July-to-June and calendar-year bases can be run and compared without a code change.
3. **Align the CPI basis with the `R_w` basis**, or state deliberately why they
   differ. At present one is contemporaneous and the other lagged, which is defensible
   but undocumented outside the ODD.

---

## 11. Update the emissions accounting against CDEII Procedures version 4.1

**Status:** open. Added 3 September 2026, after Angela asked for the current AEMO
procedures to be cited alongside the version the model was built on.

### What the model implements

GR4SP's `CDEIII` is an extension of AEMO's Carbon Dioxide Equivalent Intensity Index,
made under **clause 3.13.14(a) of the National Electricity Rules**. The two formulas
the model builds on are AEMO's own, and they were checked against the source on
3 September 2026:

| AEMO | Formula | GR4SP |
|---|---|---|
| Formula 2 | `CDE_i = EF_i x E_i`, where `E` is **sent out** generation measured at the connection point, excluding the intra-regional loss factor | Equation 3 of the EMS article, verbatim |
| Formula 4 | `CDEII = sum(CDE) / sum(E)` | Equation 4 of the EMS article, verbatim |

The model then extends this recursively through the nested SPMs, which is the part
that is GR4SP's own (`Spm.computeIndicators`).

### What has changed since

AEMO's published version history:

| Version | Effective | Note |
|---|---|---|
| **4.1** | **9 August 2026** | Updated for the *National Electricity Amendment (Shortening the settlement cycle) Rule 2024 No. 22* |
| 4.0 | 10 June 2019 | The version the model was built against |
| 3.0 | 11 December 2014 | Updated the source of emission factor data |
| 2.0 | 23 July 2013 | |
| 1.0 | 2 December 2010 | First issue |

âš ï¸ **There is no 2016 version.** If a locally held copy is dated 2016 it is a
re-hosted copy of version 3.0 (December 2014), not a separate release.

### What to do

1. **Read version 4.1 against the implementation.** The settlement-cycle change is the
   substantive one: the NEM moved to five-minute settlement on 1 October 2021, and the
   model dispatches at 30 minutes. That is already declared as a scope limit in the
   EMS article for the validation window, which closes before the change, but any run
   extended past 2021 inherits the old interval.
2. **Check the emission factor source.** Version 3.0 changed where the factors come
   from, and the model's factors are seeded in `generationassets`. AEMO's current
   per-DUID factors are already in the repository at
   `experiments/simulationData/co2eii_available_generators.csv` (500 rows, with
   `CO2E_ENERGY_SOURCE` and `CO2E_DATA_SOURCE`), so this is a comparison, not a
   collection exercise. See item 8.
3. **Consider the NEM region supplementary indices.** Section 4 of version 4.1
   specifies a **per-region** intensity index, published daily per region with the
   total energy and total emissions behind it. That is a closer comparator for a
   Victorian model than the NEM-wide index, and it is the natural pairing with the
   `co2eii_summary_results_<year>.csv` files already in `simulationData` (item 8).

Both procedures versions are now cited in the EMS article (`AEMO2019_CDEII` for the
version the model implements, `AEMOcdeii2026` for the current one).

---

## 12. Decide whether the model should run on NEM market time

**Status:** open. Found 5 September 2026 while rehearsing a from-scratch install.

### What was wrong, and what was done about it

GR4SP is date-driven throughout: half-hourly demand and solar series are bucketed
into months and years, and the retail tariff updates each January
(`EndUserUnit.java:140`). Every one of those steps read the **JVM default timezone**,
which is a property of the machine, not of the model.

The same code, the same database and the same seed therefore gave different answers
in different places. Measured on one machine by varying only the ambient timezone
(Australia/Melbourne vs UTC), for a seeded 1998-2030 run:

| Series | Melbourne | UTC | Shift |
|---|---|---|---|
| System Production Water | 1,073,321.98 | 1,071,825.36 | -0.14% |
| System Production Coal | 43,287,162.05 | 43,284,528.94 | -0.006% |
| Primary Wholesale ($/MWh) | 17.5295 | 17.5251 | -0.025% |
| Percentage Renewable Production | 0.0240801 | 0.0240488 | -0.13% |

Hydro moves most, which is consistent with a shifted day boundary changing the demand
profile and so the dispatch at the margin. This is small but not negligible, and it
meant the published results were reproducible only on a machine set to Melbourne
time -- including, in practice, no continuous integration runner and few overseas
readers.

`Gr4spSim.TIMEZONE` now pins the default to `Australia/Melbourne`, chosen because it
is the timezone the reported results were produced in. **The published numbers are
unchanged by the fix**; what changes is that they are now obtained everywhere.
`./gradlew test -Dgr4sp.tz=UTC` is the guard.

### The open question

**Australia/Melbourne observes daylight saving. The NEM does not** -- market settlement
runs on Australian Eastern Standard Time year-round, UTC+10, and AEMO's dispatch and
settlement data are stamped in it. So for roughly half of each year the model's day
boundaries sit an hour away from the market's.

Two candidate answers, and the choice is a modelling decision rather than a packaging
one:

1. **Keep `Australia/Melbourne`.** Consumption is a human activity that follows local
   clock time, so household demand profiles arguably should shift with daylight saving.
2. **Move to fixed UTC+10 (NEM market time).** Dispatch, prices and settlement are
   market processes, and aligning the model to them removes a systematic half-year
   offset against every AEMO comparator -- including the CO2EII series in item 8 and
   the wholesale price validation.

The two are not exclusive: demand could be bucketed in local time while market
operations run on NEM time, which is what actually happens. That is the most faithful
option and the most work.

**Whichever is chosen, option 2 changes the published numbers**, so under
`docs/versioning.md` this is a Class B change and belongs behind the calibration
bundle, not on the frozen article calibration.


---

## 13. Represent the contract arenas the NEM actually has

**Status:** open. Raised 5 September 2026 while verifying the EMS article's OTC
citation against the AER's *Wholesale electricity market performance report,
December 2018*.

### The distinction the article now draws, and the model does not

The AER (Box 3.5, "Contract markets") is explicit that participants trade hedge
products in **two distinct ways**, and that these differ in their rules rather than
their purpose:

| | Over the counter | Exchange traded |
|---|---|---|
| Mechanism | direct contracting between counterparties, often via a broker | anonymous order book on the ASX, novated to a clearing house |
| Products | "more flexible and can be sculpted to suit the requirements of the counterparties" | "standardised to promote trading" |
| Same instrument | swaps, contracts for difference | futures |
| Transparency | parties and prices private; only AFMA voluntary survey aggregates | prices and volumes published, parties anonymous |

Participants are **not only generators and retailers**: Box 3.5 names "financial
intermediaries and speculators", who hold no physical position at all.

**Power purchase agreements are a fourth thing again, and the AER does not list them
as a hedge product.** They appear in the report attributing *trading rights* over a
plant's output, and explaining how three retailers came to control roughly 40 per cent
of new capacity since 2013--14 "either through direct build or by entering into power
purchase agreements". A PPA is a long-horizon bilateral offtake tied to a named plant,
and it typically precedes the asset. The AER's own framing is that "contract markets
underpin investment signals in the national electricity market".

Below all of this sit the arrangements for small non-scheduled generation: negotiated
offtake with a retailer, feed-in tariffs for rooftop exports, or on-site consumption.

### What the model has now

`arenas` defines three rows for the Victorian case: `1 Bulk (OTC)`,
`2 Retail household (Retail)`, `3 Spot (Spot)`. **`Arena.step()` acts on the Spot
arena only** -- its whole body is inside `if (type.equalsIgnoreCase("Spot"))` -- so:

- the **OTC arena is inert** and, per item 2, is populated with the wrong contracts;
- there is **no exchange arena at all**, so the ASX side of the AER's split is absent;
- **PPAs are not represented**, and new capacity arrives through exogenous nameplate
  settings in the YAML instead of through any contracting decision;
- the **feed-in-tariff arena is not applied** in the published runs.

The article states these limits honestly (Section 3.2.3 and Section 6). It says
utility-scale hedging is not represented, that the off-market treatment prices small
non-scheduled output at the generator's minimum LCOE, and that an analyst could
activate these arenas. **That claim is only true once `Arena.step()` dispatches on
more than one type.**

### Why it is worth doing

1. **Hedging changes bidding.** A generator with a swap or cap over part of its output
   faces a different exposure to the spot price than one without. Bids in GR4SP are
   `BasePrice/CF` for every generator in every interval, so contract position cannot
   influence behaviour. This is the mechanism most likely to matter for the wholesale
   price series, which is the weakest of the validated indicators.
2. **PPAs are an investment channel.** Item 5 wants actors to do more than own assets;
   this is a concrete thing for them to do. It would let capacity be an outcome of
   contracting instead of a configured input.
3. **The arenas are the architecture's claim.** The paper's contribution is that
   coordination arenas are explicit and swappable. Three arena types of which one runs
   is thin evidence for that claim, and a reviewer with the code open can see it.

### Sequencing

Item 2 is the prerequisite: the OTC arena must step and read the right contracts
before an exchange arena or a PPA arena is worth adding. Item 9.1 (actor--asset
ownership over time) is the prerequisite for PPAs, since a PPA binds a named plant to
a named offtaker.

**This changes the published numbers**, so under `docs/versioning.md` it is a Class B
change and belongs behind the calibration bundle, not on the frozen article
calibration.

---

## 14. Emit sub-monthly prices, so the model can be compared on the market's own resolution

**Status:** open. Identified 5 September 2026 while testing whether the AER's annual
volume weighted average prices could extend the wholesale price validation.

### What the code does now

The spot arena clears at half-hourly resolution, but nothing sub-monthly survives into
the output. `SaveData` writes `Primary Wholesale ($/MWh)` as a **monthly arithmetic
mean**, and the annual figure is the mean of those months. The half-hourly clearing
prices the arena computes are aggregated away as they are produced.

### Why it matters, quantified

The AER reports **annual volume weighted average prices** (*Wholesale electricity
market performance report*, December 2018, figure 2.3), weighting each interval by
demand. Rebuilding that from `total_demand_halfhour` for VIC1 and decomposing where
the weighting actually acts:

| Weighting applied | mean effect | max effect |
|---|---|---|
| **across** months | +0.23 $/MWh | 0.89 $/MWh |
| **within** months | **+4.06 $/MWh** | **15.70 $/MWh** (2019) |

Essentially the entire effect is **intra-month covariance between price and demand** --
spikes landing in high-demand intervals. Production-weighting the simulated side moves
it by at most 3.69 $/MWh, because a monthly mean has almost no structure left to weight.

**So a demand weighted comparator carries information the model's output cannot
contain, and comparing the two charges the model for it.** This is not hypothetical:
OpenNEM's monthly "Volume Weighted Price" is weighted within each month, so the
published validation already compares a within-month weighted observation against an
unweighted monthly simulation. Against an unweighted observed mean over the same
months the monthly RMSE falls from 35.22 to 31.71 and the bias from -7.43 to -4.03.

This also bounds what the price indicator can ever demonstrate. Scarcity pricing,
volatility and the value of firming are intra-month phenomena; a monthly mean cannot
express any of them, so no amount of calibration will make the monthly row measure
them.

### What it should do

In increasing order of effort:

1. **Persist a within-period weighted price alongside the mean.** The arena already
   computes each interval's clearing price and the demand it serves, so writing
   `sum(price x demand) / sum(demand)` per month costs one accumulator and no new
   data. That alone makes the model directly comparable to OpenNEM and to the AER on
   their own definition, and closes the mismatch above.
2. **Emit a price duration curve, or the intra-month distribution.** Percentiles, or
   counts above the AER's reporting thresholds, would let the volatility indicators in
   sections 4 and 5 of the AER report be used as comparators at all.
3. **Emit the half-hourly series itself** for a selected window. Storage is the
   objection rather than the computation; a single BAU run at half-hourly resolution
   over 1998-2051 is large, which is presumably why it was aggregated in the first
   place.

Step 1 is cheap and is the prerequisite for treating any volume weighted series as a
comparator. Steps 2 and 3 are what would let GR4SP speak to market performance
questions rather than only to annual averages.

### Relationship to the validation

`experiments/validation/README.md`, section *Resolution: what a weighted comparator
can and cannot ask of the model*, records the measurements above and why the
extended-window comparison should use an **unweighted** observed mean until step 1
exists. Note the interaction with item 12: five minute settlement makes the
intra-period structure finer still, so if the dispatch interval is ever revisited the
output resolution should be settled at the same time.

**Step 1 does not change any published number** -- it adds an output column, so under
`docs/versioning.md` it is a Class A change. Steps 2 and 3 are also additive. What
*would* be Class B is switching the validation to compare against the new column.
