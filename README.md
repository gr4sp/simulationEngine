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
| [Python 3.8+](https://www.python.org/downloads/) | 3.8 or later | Experiments and notebooks only |

---

## Quick Start

### Step 1 — Clone

```bash
git clone https://github.com/angelara/gr4sp.git
cd gr4sp
```

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

Install Python dependencies:

```bash
pip install JPype1 pandas ipyparallel SALib numpy scipy matplotlib
```

Then run an experiment from the `experiments/` folder:

```bash
cd experiments
python3 runExperimentsBAU.py
```

`settingsExperiments.json` is pre-configured — the JVM path and classpath are detected automatically. No manual editing required.

---

## Analysing Results

Jupyter notebooks for scenario analysis, sensitivity analysis, and visualisation are in `experiments/notebookGr4sp/`. Open them in VS Code or JupyterLab.

---

## Troubleshooting

| Error | Cause | Fix |
|---|---|---|
| `NullPointerException` in `LoadData` | PostgreSQL not configured to trust local connections | Follow the trust configuration in Step 2 |
| `Problems reading YAML file` | Running simulation from wrong directory | Always run from the project root (`gr4sp/`) |
| `gradlew: Permission denied` | Fresh clone on Linux/macOS | `chmod +x gradlew` |
| Java not found during setup | `JAVA_HOME` not yet in terminal PATH | The setup script auto-detects from `JAVA_HOME`; or restart VS Code |
| `pg_restore` warnings about `adminpack` | Extension not available in newer PostgreSQL | Harmless — data is restored correctly |
