# Legacy per-scenario experiment scripts

These are the original **one-file-per-scenario** EMA scripts for the BAU / JT /
LCT / ST scenarios. They were retired by the **task 3.1a** consolidation and
replaced by a single config-driven pipeline in `experiments/`:

| Retired here (per scenario) | Replaced by (one file, all scenarios) |
|---|---|
| `gr4spModel<X>.py` (model definition) | `scenarios.yaml` + `gr4sp_model.py` |
| `connector<X>.py` (JPype bridge) | `gr4sp_connector.py` |
| `runExperiments<X>.py` (runner) | `run_experiment.py <SCENARIO>` |

## Why they were retired

The four `connector<X>.py` files were near-identical: `connectorLCT`, `connectorST`
and `connectorJT` had **zero** substantive differences, and `connectorBAU` differed
by a **single line** (it forced the after-base-year `energyEfficiency` to
`category(0)` instead of the sampled value). The `gr4spModel<X>.py` files differed
only in which parameters were declared uncertainties vs constants. Adding a new
scenario meant copying ~650 lines across three files and hand-editing the split.

The consolidation preserves behaviour exactly:

- **Model definitions** — `gr4sp_model.getModel(<name>)` reproduces each original
  `getModelAfterBaseYear()` identically (same uncertainties, constants, outcomes).
  Guarded by `experiments/validate_scenarios.py`, which compares the consolidated
  builder against the originals kept here.
- **Connector** — `gr4sp_connector.py` is `connectorJT.py` plus one config knob
  (`after_base_year_energy_efficiency_zero`, set per scenario in `scenarios.yaml`)
  that restores BAU's single divergent line. All other logic is byte-identical.

## Notes

- `runExperimentsBAU.py` was already **broken** before retirement: it imported
  `getModel` from `gr4spModelBAU`, which only defines `getModelAfterBaseYear`
  (the `getModel` variant is commented out). The consolidated `run_experiment.py BAU`
  fixes this.
- The **latent BAU quirk** — sampling `energyEfficiency` while forcing it to `0`
  in the after-base-year forecast — is preserved faithfully (not "fixed"), pending
  review. It is flagged in `gr4sp_connector.py`.
- Other scenario families (EET, EET3Regime, SOBOL, LHS, WhatIfPastSOBOL) still use
  their own scripts in `experiments/` and were **not** part of this consolidation.

These files are kept for provenance and as the reference the validator checks
against. They are no longer wired into the active pipeline.
