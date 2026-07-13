# Reproducibility manifest

Tracks which EMA-workbench result archives (`*.tar.gz`) each analysis notebook
depends on, and maps them to the published Zenodo deposit so the notebooks can
be reproduced. The archives are too large to commit and are `.gitignore`d.

## Where the data lives

The 14 **final** result archives are published on Zenodo, record
[8320754](https://zenodo.org/records/8320754) (~1.3 GB total). They are pulled
into `../simulationData/` by:

```
python ../simulationData/fetch_results.py          # the 14 *.tar.gz archives
python ../simulationData/fetch_results.py --all    # also the input CSV/XLSX/ZIP
python ../simulationData/fetch_results.py --list    # list without downloading
```

Files already present (matching size) are skipped, so the download resumes
cleanly if interrupted.

## Files here

- **`archive_manifest.csv`** — one row per `*.tar.gz` referenced by any notebook.
  - `on_zenodo_8320754?` — whether it is in the published deposit (auto-filled).
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
  after editing notebooks or when the Zenodo record changes (update
  `ZENODO_ARCHIVES` to match).

## Status of the archives not on Zenodo

15 archives are marked `retire`: 9 appear only in commented-out cells, and 6 are
old test runs actively loaded by superseded notebooks (`ema_gr4sp_EET_ABY`,
`ema_gr4sp_Envelopes`, `ema_gr4sp_*_HypoPast`, `ema_gr4sp_EET-3RegimeValidation`,
`ema_gr4sp_PRIM-Scenarios` (partial), and the base `gr4sp_energy_vulnerability`).
These were exploratory tests, deliberately not deposited; they are kept on record
here in case their data is useful later, but the 7 notebooks depending on them
are not reproducible from the public deposit.
