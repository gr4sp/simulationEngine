# Retired notebooks

These notebooks are kept for the record but are **no longer maintained**. They
were retired on 2026-07-13 because each actively loads a result archive that was
never deposited to Zenodo (record 8320754) — i.e. an old exploratory/test
simulation run, superseded by the final scenarios. They are therefore not
reproducible from the public data and are not part of the active analysis set.

They remain in git history and here in full; the archives they reference are
still listed (marked `retire`) in `../reproducibility/archive_manifest.csv`.

> Note: their relative paths (`../simulationData`, `../assesmentData`,
> `../EMAworkbench`, `..`) were written for the parent `notebookGr4sp/` folder.
> From here they are one level too shallow, so reviving a notebook means fixing
> its paths (and modernising its code — see `../reproducibility/`).

| Notebook | Retired because it loads (not on Zenodo) | Superseded by |
|---|---|---|
| `ema_gr4sp_EET_ABY.ipynb` | `gr4sp_EET2020ABY-Dec-02` | `ema_gr4sp_EET_HypoPast` (active) |
| `ema_gr4sp_EET-3RegimeValidation.ipynb` | `gr4sp_SOBOLhypopast2021-Mar-03_...` | — |
| `ema_gr4sp_Envelopes.ipynb` | `gr4sp_SOBOL2020-Dec-23` | envelopes now in `Uncertainty Analysis` |
| `ema_gr4sp_PRIM-Scenarios-HypoPast.ipynb` | `gr4sp_SOBOL-HypoPast-2021-Jan-20` | `ScenariosAnalysis` |
| `ema_gr4sp_SOBOL_HypoPast.ipynb` | `gr4sp_SOBOL-HypoPast-2021-Jan-20` | `ema_gr4sp_SOBOL_ABY` (active) |
| `ema_gr4sp_PRIM-Scenarios.ipynb` | `gr4sp_LCT2021-Aug-04` (only this one; also loads 2 Zenodo archives) | `ScenariosAnalysis` |
| `gr4sp_energy_vulnerability.ipynb` | `gr4sp_2020-Mar-05-SOBOL` | the `gr4sp_energy_vulnerability_IMAP*` family (active) |

`ema_gr4sp_PRIM-Scenarios` was *partially* reproducible (2 of its 3 archives are
on Zenodo). If a unique output from it is ever needed, repoint its
`LCT2021-Aug-04` load to a deposited LCT archive (`LCT2021-Sep-01`) and revive it
rather than starting over.
