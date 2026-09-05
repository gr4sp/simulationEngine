# Reproducibility manifest

Tracks which EMA-workbench result archives (`*.tar.gz`) each analysis notebook
depends on, and maps them to the published Zenodo deposit so the notebooks can
be reproduced. The archives are too large to commit and are `.gitignore`d.

## Where the data lives

The 15 **final** result archives are published on Zenodo under the concept DOI
[10.5281/zenodo.4667996](https://doi.org/10.5281/zenodo.4667996) (~2.2 GB total).
They are split across two versions, and neither version holds them all:

| Version | Holds |
|---|---|
| [8320754](https://zenodo.org/records/8320754) | the 14 scenario archives, ~1.3 GB |
| [22172036](https://zenodo.org/records/22172036) | the validation ensemble, 972 MB, plus the pinned BAU CSVs |

A Zenodo version does not inherit the previous one's files, so reading either
version alone sees only part of the deposit. `fetch_results.py` merges both
file lists. You can cite all versions by using the DOI
[10.5281/zenodo.4667996](https://doi.org/10.5281/zenodo.4667996), which always
resolves to the latest one. The archives are pulled into `../simulationData/` by:

```
python ../simulationData/fetch_results.py          # the 15 *.tar.gz archives
python ../simulationData/fetch_results.py --all    # also the input CSV/XLSX/ZIP
python ../simulationData/fetch_results.py --list    # list without downloading
```

Files already present (matching size) are skipped, so the download resumes
cleanly if interrupted.

## Files here

- **`archive_manifest.csv`** — one row per `*.tar.gz` referenced by any notebook.
  - `on_zenodo?` — whether it is in the published deposit, either version
    (auto-filled).
  - `status` — `fetched from Zenodo`, or `retire (...)` for legacy runs.
  - `active_in` / `n_active` — notebooks that load it in a live (uncommented)
    cell; `n_active = 0` means it only appears commented out.

- **`notebook_outputs.csv`** — one row per notebook.
  - `reproducible_from_zenodo?` — `yes` if every archive it actively loads is on
    Zenodo; `no` if it needs a retired archive; `n/a` if it loads no archive.
  - `active_archives` / `archives_not_on_zenodo` — its dependencies and any gaps.
  - `output_files`, `first_titles`, `n_savefig`, `n_data_writes` — hints for
    matching a notebook to its published figures.

- **`build_manifest.py`** — regenerates both CSVs from the notebooks. Re-run
  after editing notebooks or when the Zenodo deposit changes (update
  `ZENODO_ARCHIVES` to match; `fetch_results.py --list` enumerates it live).

## Status of the archives not on Zenodo

14 archives are marked `retire`: they appear only in commented-out cells or are
old test runs actively loaded by superseded notebooks (`ema_gr4sp_EET_ABY`,
`ema_gr4sp_Envelopes`, `ema_gr4sp_*_HypoPast`, `ema_gr4sp_PRIM-Scenarios`
(partial), and the base `gr4sp_energy_vulnerability`). These were exploratory
tests, deliberately not deposited; they are kept on record here in case their
data is useful later, but the notebooks depending on them are not reproducible
from the public deposit.

`ema_gr4sp_EET-3RegimeValidation` is no longer in that group. It was retired for
loading the validation ensemble, which had not been deposited; version 22172036
deposits it, so the notebook's inputs are now public even though the notebook
itself stays retired in `../legacy/` (its statistics are reproduced by
`../../validation/validation_statistics.py`).
