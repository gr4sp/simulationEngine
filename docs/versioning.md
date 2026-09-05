# Keeping the article reproducible while the model moves on

**Status: proposal, for decision. Written 5 September 2026. Changes no code.**

## The question

The methods article reports GR4SP as it stood at its 2019 base year: a business-as-usual
run to 2019, from which scenarios, sensitivity and uncertainty analyses depart. Those
results must stay reproducible with fidelity for as long as the article stands.

At the same time it is 2026, and `ROADMAP.md` records eleven improvements the
architecture already implies but the implementation does not yet deliver. Some of them
cannot possibly change the article's numbers. Some certainly would.

So: one model with a switch, or two models? This document argues for neither extreme, and
recommends **one codebase with two named calibrations, anchored by an immutable tag and
enforced by the regression test that already exists.**

---

## First: fidelity does not rest on the code alone

The most common way to lose reproducibility here is not a code change. It is a data
refresh. Three inputs have to be pinned *together*:

| Input | The article's version | How it drifts |
|---|---|---|
| **Code** | a git tag on the submitted commit | ordinary development on `master` |
| **Settings** | `simulationSettings/VIC.yaml` | scenario edits, parameter retuning |
| **Database** | `backupDB/DB-2021-8-21.sql` | `scripts/data/fetch_*.py` overwrite `gr4spdb` in place |

The database is the sharpest of the three, and it was measured: a refreshed `gr4spdb`
matches the shipped 2021 dump up to 2019 and then **diverges from it by up to 23%**.
Nothing in the Java has to change for the article's figures to stop reproducing — someone
running the update scripts is enough.

This matters for the architecture, because it means **the two-version split is forced by
the data whatever we decide about the code**. An updated GR4SP needs a current database;
the article's GR4SP needs the 2021 dump. There is no switch that avoids that. The
question is only whether the *code* forks as well.

---

## Classifying the roadmap by whether it can move the 2019 numbers

Working through every roadmap entry gives a more favourable split than expected — and for
a specific reason: **the deferred links are unwired.** Several changes that sound
structural are inert today because nothing reads their output. That inertness is exactly
what makes them safe to land now.

### Class A — result-neutral: land on the main line, no switch needed

These cannot move the article's numbers, because what they touch is either unread or
outside the model.

| Item | Why it is neutral |
|---|---|
| **1** (route 1): per-SPM emissions mass | Those series are currently **columns of zeros** for the four non-root Victorian SPMs (`SaveData.java:632-643`). The article reads the root only, which is computed separately and is unaffected. Filling zeros with real numbers adds information and removes none. |
| **5 / 9.1**: `actorasset` → `actors` key migration, and validity dates on the relation | Item 5's own finding is that actors are loaded and then used for almost nothing. Repointing a foreign key and adding `valid_from`/`valid_to` changes no quantity the simulation computes, *until* something reads it — at which point it becomes Class B or C and is re-assessed. |
| **8**: AEMO CDEII results, 2011–2025 | A new *comparator*, not a model change. It extends validation seven years past the Victorian Government series without touching what is being validated. |
| **11** (steps 1–2): CDEII 4.1 reading, emission-factor comparison | Reading a procedure against the implementation, and comparing seeded factors against AEMO's published per-DUID values, are analyses. Acting on a discrepancy is a separate, later decision. |
| **2** (partial): correcting which contracts populate the OTC arena | The arena is never read. Fixing what goes into it changes nothing until it is read. |

**Rule for Class A: the regression test must stay green with no configuration change.**
That is the definition, not a hope — if it goes red, the item was misclassified.

### Class B — behaviour-changing, but expressible as configuration

These change results, and each is a single parameter, predicate or schedule rather than a
change of structure. They belong behind the calibration bundle described below.

| Item | The switchable thing |
|---|---|
| **summer capacity factor** (`1e13ab1`, currently frozen behaviour-neutral) | The precedent. The fix is committed but deliberately inert, pending the right summer percentage. |
| **10**: tariff averaging window | Item 10 *itself* proposes making the six-month window a setting, so July–June and full-calendar-year bases can be compared without a code change. |
| **6**: market price cap | Static calibration value today; a dated schedule read from settings. |
| **7**: actors registered outside the modelled region | A widened SQL predicate in `LoadData.selectActors`. Currently loads 486 of 925 rows and drops 21 of the 58 asset-holding actors, Origin and Snowy Hydro among them. |
| **6**: secondary spot arena | Implemented already; unconfigured for Victoria. Pure configuration — nothing to build. |
| **3 + 6**: solar surplus and the feed-in tariff arena | `Arena` supports a `fiTs` type with no row in the `arenas` table. Data and a remuneration rule, then enabled by configuration. |

### Class C — structural: cannot be reduced to a flag

These change the shape of the model, not a value in it. They are why the tag matters.

| Item | Why it cannot be a flag |
|---|---|
| **4**: demand below the root SPM | Local balancing, islanding and surplus handling change what the recursion computes. |
| **9.2 / 1** (route 2): attaching consumption at each scale | Changes the recursion's contract, not a parameter within it. |
| **2**: actually reading the OTC arena | Contracts entering dispatch changes clearing. |
| **6**: demand response, FCAS, network power flow, strategic bidding | Each adds a mechanism that does not exist. |
| **11** (step 3): five-minute settlement | The model dispatches at 30 minutes. The interval is structural to dispatch, not a setting. |

---

## Recommendation

**One repository, one codebase, two named calibrations. Not a fork, and not a maze of
`if (legacy)` branches.**

### 1. An immutable tag is what actually guarantees fidelity

Tag the submitted commit and cut a release; if the GitHub repository is linked to Zenodo,
that also mints a software DOI for the code availability statement. **This, not a runtime
flag, is the fidelity guarantee** — a tag cannot rot, and a reader who wants the article's
model exactly can check it out and ignore everything below. Everything else in this
document is about making `master` *also* able to reproduce the article, which is a
convenience, not the guarantee.

### 2. `master` keeps evolving, with two calibrations side by side

- `simulationSettings/VIC.yaml` stays the **frozen article calibration** and is not
  retuned.
- A new `simulationSettings/VIC2026.yaml` carries the updated model.
- Class A work lands directly, in both.
- Class B differences are selected by **one named bundle key** in the YAML — something
  like `calibration: article2019` versus `calibration: current` — and *not* by N
  independent booleans.

That last point is the whole design decision. A per-change flag looks cheaper for the
first two changes and is unmaintainable by the tenth: 2^N combinations, of which exactly
two are ever meant to be run, and no way to tell a deliberate combination from an
accident. A named bundle has one axis, is self-documenting in the settings file, and makes
"what did the article use?" a question with a one-word answer.

### 3. `SimulationRegressionIT` is the contract

`src/test/java/core/SimulationRegressionIT.java` already exists and already does exactly
the right thing: it runs a seeded simulation, compares the year-summary CSV cell-by-cell
against a committed golden baseline at `1e-6` relative tolerance, and fails loudly on any
divergence. It has a documented baseline-regeneration path
(`-Dgr4sp.updateBaseline=true`) for intended changes.

Adopting it as the versioning contract needs only a rule and one addition:

- **The rule.** Any change that turns it red must either be reverted or moved behind the
  calibration bundle. This converts "we hope the article still reproduces" into something
  CI enforces on every commit.
- **The addition.** The current baseline is a seeded 1998→2030 run at seed 42 — a good
  proxy, but not the article's run. A second baseline pinned to the actual reported BAU
  (`VICSimDataYearSummary_bau19982051.csv`, already committed and already used by
  `experiments/validation/validation_statistics.py`) would make the contract test the
  thing the article actually reports.

A cheap third layer: run `validation_statistics.py` in CI and assert the table it prints
is unchanged. It needs only numpy and pandas, no database, so it is nearly free.

### 4. Fork only at a stated threshold

Decide the threshold now rather than discovering it: **fork when a Class C change makes
the frozen calibration unrunnable on `master`** — when the article's configuration can no
longer be expressed at all, rather than merely being one bundle among two.

Concretely, the likely trigger is item 4 or item 9.2. Once consumption attaches to SPMs
throughout, "no demand below the root" stops being a setting and becomes a different
model. At that point the tag and its Zenodo DOI carry the article on their own, `master`
drops the frozen calibration, and the README says plainly which tag reproduces the paper.
Until then, keeping both in one tree costs one YAML key and one test.

---

## Suggested sequence

1. Tag and release the submitted state. *(Nothing else is safe to start before this.)*
2. Add the article-BAU golden baseline to `SimulationRegressionIT`, and put
   `validation_statistics.py` in CI.
3. Land Class A items. Each one should be green with no configuration change; if it is
   not, it was misclassified and moves to Class B.
4. Introduce the `calibration` bundle key with exactly two values, and move the frozen
   summer-CF fix behind it as the first inhabitant — it is already written and already
   frozen, so it tests the mechanism without new modelling risk.
5. Take Class B items one at a time, each with a stated effect on the validation table.
6. Re-assess before the first Class C item, against the fork threshold above.

## What this document does not decide

- The right summer capacity factor percentage (a scientific question, currently blocking
  the frozen fix).
- Whether to refresh the shipped database, and to what vintage. That is a scientific
  decision about what the updated model should represent, not a packaging one.
- Whether the article's ambiguous `historic_tariff_contribution` year labels are calendar
  or financial years (roadmap item 10). That has to be settled against the original
  extraction, and it affects the frozen calibration if the answer is that the labels are
  wrong.
