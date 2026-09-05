# GR4SP

**Generic Recursive Simulation of Socio-technical Systems for Service Provision**

GR4SP is an agent-based simulation model of the Victorian electricity system in Australia. The core engine is written in Java (using the [MASON](https://cs.gmu.edu/~eclab/projects/mason/) framework) and models how generators, consumers, networks, and policy interact over time. A Python layer built on [EMA Workbench](https://emaworkbench.readthedocs.io/) enables sensitivity analysis and large-scale scenario experiments. Jupyter notebooks in `experiments/notebookGr4sp/` support result analysis and visualisation. While the data is Victorian electricity-specific, GR4SP's structure can guide similar simulations in other contexts.

---

## Prerequisites

| Tool | Version | Purpose |
|---|---|---|
| [Java JDK 17+](https://adoptium.net) | 17 or later | Run the simulation |
| [PostgreSQL](https://www.postgresql.org/download/) | 14 or later | Electricity system database |
| [VS Code](https://code.visualstudio.com/) + [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) | Any recent | Edit and build |
| [Miniforge](https://github.com/conda-forge/miniforge) | Python 3.10+ | Experiments, notebooks, and the data-update scripts only |

---

## Quick Start

### Step 1 — Clone

```bash
git clone https://github.com/gr4sp/simulationEngine.git
cd simulationEngine
```

> **Windows: enable long paths first.** A few data files in `experiments/assesmentData/` have descriptive names up to 139 characters. Combined with a deep clone location, these can exceed the legacy 260-character Windows path limit and the clone fails with `error: unable to create file ...: Filename too long`. Enable long-path support once (Administrator PowerShell):
>
> ```powershell
> git config --system core.longpaths true
> ```
>
> Without Administrator rights, use `git config --global core.longpaths true`, or simply clone into a short path such as `C:\gr4sp`.

### Step 2 — Run the setup script

The setup script checks Java, builds the project, creates output directories, and loads the database.

**Windows** (VS Code terminal):
```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\setup.ps1
```

**Linux / macOS**:
```bash
chmod +x setup.sh && ./setup.sh
```

Both scripts will prompt for your PostgreSQL password and handle everything automatically.

> **One-time PostgreSQL configuration**
>
> GR4SP connects to the database without a password (JDBC). You need to configure PostgreSQL to trust local connections. Run the following **as Administrator** (Windows) or with `sudo` (Linux/Mac), then restart PostgreSQL.
>
> **Windows** (run each line separately in an Administrator PowerShell):
> ```powershell
> (Get-Content "C:\Program Files\PostgreSQL\18\data\pg_hba.conf") -replace '(host\s+all\s+all\s+127\.0\.0\.1/32\s+)scram-sha-256','${1}trust' -replace '(host\s+all\s+all\s+::1/128\s+)scram-sha-256','${1}trust' | Set-Content "C:\Program Files\PostgreSQL\18\data\pg_hba.conf"
> ```
> ```powershell
> Restart-Service postgresql-x64-18
> ```
>
> **Linux / macOS** — edit `/etc/postgresql/XX/main/pg_hba.conf` (where XX is your PostgreSQL version) and set the `host` lines for `127.0.0.1/32` and `::1/128` to `trust`, then run:
> ```bash
> sudo service postgresql restart
> ```

### Step 3 — Run the simulation

**Windows:**
```
.\runGr4sp.bat
```

**Linux / macOS:**
```bash
./runGr4sp.sh
```

The simulation runs the default scenario (`simulationSettings/VIC.yaml`). Results appear in `csv/` and `plots/` when complete.

### Step 4 — Verify your installation

One command checks the whole install at once — Java, the build, the database connection, and the model's own numerics:

```powershell
.\gradlew.bat test    # Windows
./gradlew test        # Linux / macOS
```

A correct fresh installation runs **43 tests across 10 classes, with none skipped**.

**The count is the point.** The two database-backed classes (`LoadDataIT` and `SimulationRegressionIT`) skip themselves when PostgreSQL is unreachable, so a green run reporting *fewer* tests means the database is not connected — not that there was less to check. If you see skips, revisit the PostgreSQL trust configuration in Step 2.

`SimulationRegressionIT` is the strongest check of the three: it runs a seeded simulation end to end and compares the year-summary output cell-by-cell against a reference committed to the repository. If it passes, your installation reproduces known-good numbers rather than merely running without crashing.

> **If you have refreshed the database, this one test will skip.** Its reference output corresponds to the shipped snapshot `backupDB/DB-2021-8-21.sql`. Once you run the scripts in `scripts/data/` your `gr4spdb` holds later data — rooftop solar to 2025 rather than 2019, demand to 2026 rather than mid-2021 — which legitimately produces a different run, so the test skips and tells you why rather than reporting a spurious failure. To run it anyway, restore the snapshot alongside your working database:
>
> ```bash
> createdb gr4spdb_ref && pg_restore -d gr4spdb_ref backupDB/DB-2021-8-21.sql
> ./gradlew test -Dgr4sp.db.url="jdbc:postgresql://localhost:5432/gr4spdb_ref?user=postgres"
> ```

The full HTML report is written to `build/reports/tests/test/index.html`.

---

## Run with Graphical UI

To watch the simulation progress with live plots:

**Windows:**
```
.\runGr4spUI.bat
```

**Linux / macOS:**
```bash
./runGr4spUI.sh
```

---

## Build from Source (VS Code)

The project uses Gradle. No manual `javac` commands needed.

- **VS Code**: open the Gradle panel (elephant icon in the left sidebar) → `gr4sp` → `Tasks` → `build` → double-click `build`
- **Terminal**:
  ```powershell
  .\gradlew.bat build   # Windows
  ./gradlew build       # Linux / macOS
  ```

Compiled classes go to `build/classes/java/main/`.

---

## Changing Simulation Settings

Scenario settings are defined in `simulationSettings/*.yaml`. The default scenario loaded at startup is `VIC.yaml`.

Key parameters:

| Parameter | Values | Description |
|---|---|---|
| `reportGeneration` | `"full"` / `"light"` | `full` saves all CSV data and plots (~10 MB); `light` saves summary files only (~75 KB) |
| `logLevel` | `"OFF"` / `"WARNING"` / `"ON"` | Simulation logging verbosity |
| `simulationDates.startDate` | `YYYY-MM-DD` | Simulation start date |
| `simulationDates.endDate` | `YYYY-MM-DD` | Simulation end date |

> **Note:** `folderOutput` is auto-detected from the working directory — do not edit it.

---

## Running Experiments (Python / EMA Workbench)

Create a conda environment with [Miniforge](https://github.com/conda-forge/miniforge) and install the experiment dependencies:

```powershell
conda create -n gr4sp python=3.12
conda activate gr4sp
pip install JPype1 pandas ipyparallel SALib numpy scipy matplotlib PyYAML
```

Each new terminal session, re-activate the environment with `conda activate gr4sp`.

EMA Workbench itself does not need installing — it is vendored in `experiments/EMAworkbench/` and imported from there.

Scenarios are defined in `experiments/scenarios.yaml`, which holds the uncertainties, constants, and run settings for each one. Run a scenario by name from the `experiments/` folder:

```bash
cd experiments
python run_experiment.py BAU        # BAU, JT, LCT, or ST
python run_experiment.py JT --dry-run    # print the run plan without executing
python run_experiment.py LCT --processes 4
```

Results are written to `experiments/simulationData/gr4sp_<SCENARIO><date>.tar.gz`. Note that the sampled scenarios (JT, LCT, ST) run thousands of simulations and take hours; `BAU` is a single deterministic run and is the quickest way to check your setup works.

`settingsExperiments.json` is pre-configured — the JVM path and classpath are detected automatically. No manual editing required, but the project must have been built first (`.\gradlew.bat build`) so that `build/runtime-libs/` exists.

---

## Updating Victorian Electricity Data

Scripts in `scripts/data/` refresh `gr4spdb` with current AEMO demand/price, Open Electricity generation, ERA5 solar/temperature, and CER rooftop solar data. They use the same `gr4spdb` database as the simulation, so re-run them periodically to keep scenarios current.

Install their dependencies (in the same `gr4sp` conda environment used for experiments, or a separate one):

```powershell
conda activate gr4sp
pip install -r scripts/data/requirements.txt
```

Copy `scripts/data/.env.example` to `scripts/data/.env` and fill in the required credentials (an Open Electricity API key from [platform.openelectricity.org.au](https://platform.openelectricity.org.au), and a Copernicus Climate Data Store key from [cds.climate.copernicus.eu](https://cds.climate.copernicus.eu) — accept the ERA5 dataset's license on its download page before first use). `.env` is gitignored; never commit it.

Then run each script from the project root as needed:

```powershell
python scripts/data/fetch_aemo_demand.py          # VIC1 demand & price (AEMO NEMWeb, no API key needed)
python scripts/data/fetch_openelectricity.py       # Generation & revenue by fuel type
python scripts/data/fetch_era5.py                  # Solar irradiance & temperature (CDS; can take hours — queues per month)
python scripts/data/fetch_cer_solar.py              # Rooftop solar installations by postcode
```

Each script is idempotent — safe to re-run, and re-running `fetch_openelectricity.py` after `fetch_era5.py` backfills the `temperaturec` column from ERA5's output. See the docstring at the top of each script for what it fetches and any known data-source limitations.

---

## Reproducing the Article's Results

The methods article reports the model as it stood at its 2019 base year. Reproducing
those numbers depends on **three pinned inputs, not one** — change any of them and the
results move:

| Pinned input | What reproduces the article |
|---|---|
| **Code** | the tagged release for the article (see [Citing GR4SP](#citing-gr4sp)) |
| **Settings** | `simulationSettings/VIC.yaml`, unmodified |
| **Database** | `backupDB/DB-2021-8-21.sql`, restored by `setup.ps1` / `setup.sh` |

> ⚠️ **Do not refresh the database before reproducing.** The scripts under `scripts/data/`
> ([Updating Victorian Electricity Data](#updating-victorian-electricity-data)) overwrite
> `gr4spdb` with current AEMO, Open Electricity, ERA5 and CER data. A refreshed database
> matches the shipped 2021 dump up to 2019 and then diverges from it by up to 23%. That
> is the intended behaviour for current work, but it will not reproduce the article. Run
> the reproduction first, or restore the dump before doing so.

The model pins its timezone to `Australia/Melbourne` (`Gr4spSim.TIMEZONE`), so results do not depend on your machine's regional settings. You do not need to change your system clock or locale to reproduce the article from anywhere in the world.

### What regenerates what

| Article artefact | Command |
|---|---|
| Validation statistics (simulation-mode validation table) | `python experiments/validation/validation_statistics.py` |
| Validation figures (RMSE bands) | `python experiments/validation/validation_statistics.py --figures ./out` |
| Sensitivity top-five rankings and the S1-vs-ST figure | `python experiments/sensitivity/sobol_s1_st_figure.py` |
| Scenario ensembles (BAU / JT / LCT / ST) | `python experiments/run_experiment.py <NAME>`, or `fetch_results.py` to download the published runs instead of re-running them |
| Which notebook needs which result archive | `experiments/notebookGr4sp/reproducibility/archive_manifest.csv` |

The first three read data that is committed to this repository. They need only
`numpy`, `pandas` and (for figures) `matplotlib` — no database, no Java build, and no
Zenodo download — so the headline numbers can be checked in a couple of minutes on a
fresh clone. `sobol_s1_st_figure.py` verifies the published rankings before it plots and
refuses to produce a figure if they do not reproduce.

Reproducing the scenario ensembles from scratch is a much larger undertaking: the
sampled scenarios run thousands of simulations and take hours. The published runs are on
Zenodo (see [Data archives](#data-archives)) and are the practical starting point.

GR4SP continues to be developed. [`docs/versioning.md`](docs/versioning.md) explains how
the frozen article calibration is kept reproducible alongside the evolving model, and
[`ROADMAP.md`](ROADMAP.md) records the improvements in progress.

---

## Analysing Results

Jupyter notebooks for scenario analysis, sensitivity analysis, and visualisation are in `experiments/notebookGr4sp/`. Open them in VS Code or JupyterLab.

Install the notebook dependencies first:

```powershell
conda activate gr4sp
pip install -r experiments/notebookGr4sp/requirements.txt
```

The notebooks analyse the results of EMA experiment runs, which are large `.tar.gz` archives that are not stored in git. The published run results are archived on Zenodo, split across versions [8320754](https://zenodo.org/records/8320754) (the 14 scenario archives) and [22172036](https://zenodo.org/records/22172036) (the validation ensemble). You can cite all versions by using the DOI [10.5281/zenodo.4667996](https://doi.org/10.5281/zenodo.4667996), which always resolves to the latest one. The fetch script merges both file lists, so one run gets them all:

```bash
cd experiments/simulationData
python fetch_results.py --list          # show what is available, download nothing
python fetch_results.py                 # fetch the 15 result archives (~2.2 GB)
python fetch_results.py --all           # also fetch the input CSV/XLSX files
python fetch_results.py JT              # fetch only archives matching a substring
```

The script skips files already present, so it is safe to re-run. Fetching only the archives a given notebook needs is usually enough — see `experiments/notebookGr4sp/reproducibility/` for which notebook depends on which archive.

---

## Troubleshooting

| Error | Cause | Fix |
|---|---|---|
| `NullPointerException` in `LoadData` | PostgreSQL not configured to trust local connections | Follow the trust configuration in Step 2 |
| `Problems reading the YAML settings in ...` | Running the simulation from somewhere other than the project root | Always run from the project root — the folder containing `simulationSettings/` |
| `gradlew: Permission denied` | Fresh clone on Linux/macOS | `chmod +x gradlew` |
| `Filename too long` during `git clone` (Windows) | Legacy 260-character path limit | `git config --system core.longpaths true`, then re-clone — or clone into a shorter path such as `C:\gr4sp` |
| Java not found during setup | `JAVA_HOME` not yet in terminal PATH | The setup script auto-detects from `JAVA_HOME`; or restart VS Code |
| `pg_restore` warnings about `adminpack` | Extension not available in newer PostgreSQL | Harmless — data is restored correctly |

---

## Data archives

One Zenodo deposit holds the data behind the published results, under the concept DOI
[10.5281/zenodo.4667996](https://doi.org/10.5281/zenodo.4667996). It always resolves to the latest one. Its versions do not
carry the same files, and a Zenodo version does not inherit the previous one's, so the version matters when
fetching rather than citing:

| Version | Record | Contents |
|---|---|---|
| 1.0 | [4667997](https://zenodo.org/records/4667997) | The PostgreSQL input database that initialises the simulation engine — generation and network assets, actors, demand, tariffs, and the historical registers. |
| 2.0 | [8320754](https://zenodo.org/records/8320754) | Additional input datasets, simulation outputs, and sensitivity and uncertainty analysis results — including the 14 scenario archives. |
| 3.0 | [22172036](https://zenodo.org/records/22172036) | The validation ensemble, the pinned business-as-usual run behind Table 4, and the EET screening table. |

`backupDB/` holds the database dumps the setup script restores from. Version 1.0 above is the archived, citable version of the input database.

---

## Citing GR4SP

If you use GR4SP, please cite the software using the metadata in [`CITATION.cff`](CITATION.cff).

The model and its Victorian application are documented in full in:

> Rojas Arévalo, A. M. (2022). *Sustainability transitions modelling and assessment of socio-technical energy systems: An Australian case.* PhD thesis, The University of Melbourne. <https://hdl.handle.net/11343/324500>

### Methods article — **PENDING**

A methods article describing the GR4SP suite is currently under review. The details
below will be filled in once it is available; until then, please cite the thesis above
and the software metadata in `CITATION.cff`.

<!-- PENDING: fill in on acceptance, and mirror into CITATION.cff -->
> Rojas Arévalo, A. M., et al. (year). *Title.* **Journal** — *under review*.
> DOI: `<pending>` · Preprint: `<pending>`

The version of the code that produces the article's results is marked by a git tag and
an accompanying release; see [Reproducing the Article's Results](#reproducing-the-articles-results)
for the three inputs that must be pinned together.
