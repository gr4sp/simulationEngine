# Reproducibility manifest

These files track which EMA-workbench result archives (`*.tar.gz`) and input
CSVs each analysis notebook depends on, so the published results can be
reproduced from a minimal, curated data set hosted on Zenodo (the archives are
too large to commit and are `.gitignore`d).

## Files

- **`archive_manifest.csv`** — one row per `*.tar.gz` archive referenced by any
  notebook.
  - `active_in` / `n_active` — notebooks that load it in a live (uncommented)
    cell. `n_active = 0` means it only appears in commented-out lines.
  - `commented_in_count` — how many notebooks reference it only in comments.
  - `keep_for_zenodo?` — **fill this in.** Pre-marked `no (legacy - commented-only)`
    for archives never actively loaded (old testing runs). Set the rest to
    `yes` / `no` once the published notebooks are identified.

- **`notebook_outputs.csv`** — one row per notebook.
  - `published?` — **fill this in** (`yes`/`no`): does this notebook generate a
    figure/table used in the thesis or a paper?
  - `active_archives` — the archive(s) it loads live.
  - `output_files`, `first_titles`, `n_savefig`, `n_data_writes` — hints to help
    match a notebook to its published figures.

## How to derive the Zenodo keep-set

1. Mark `published? = yes` on the notebooks that feed the thesis/papers.
2. The union of those notebooks' `active_archives` is the keep-set.
3. Set `keep_for_zenodo? = yes` on exactly those archives; leave the rest `no`.
4. The completed `archive_manifest.csv` then drives both the Zenodo upload and
   the `fetch` script that downloads the archives into `../simulationData/`.
