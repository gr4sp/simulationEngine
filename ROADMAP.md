# GR4SP model roadmap

Proposed changes to the model itself, recorded as they are identified so that the
evolution of GR4SP carries them forward. Each entry states what the code does now,
what it should do, and why, with file and line references to the behaviour as found.

Items here are *not* defects unless marked as such. They are capabilities the
architecture already implies but the implementation does not yet deliver.

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
that is SPMs 6, 5, 7 and 9 — four of the five.

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
`Arena.EndConsumer` (999) as the buyer — household retail tariffs. An OTC arena is
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
   cycling costs), and the parameter values agree — brown coal 0.47, wind 0.28. Both
   documents were corrected on 28 August 2026.

### What it should do

Two routes, depending on intent:

1. **If OTC is to stay unexercised:** do not load contracts into it. Restrict the
   branch at `LoadData.java:436-438` to `Retail`, and either drop the `Bulk` row from
   the `arenas` table or keep it as a declared-but-empty arena. Cheapest, and it makes
   the registry honest.
2. **If OTC is to be exercised:** give it its own contract source. An OTC arena needs
   contracts between two named market participants with a strike price and a term, not
   `EndConsumer` tariffs, and it needs a settlement rule — a contract for difference
   against the spot price is the standard form in the NEM. This is the route that would
   let the suite say something about hedging and investment timing, which the article
   currently lists as outside its scope.

Note that route 2 changes what the model can claim, not just how it is coded. Physical
bilateral supply at small scale is already represented, through the off-market
treatment (`Arena.java:607-609`, priced at BasePrice/MaxCF); what is missing is
financial hedging at utility scale.
