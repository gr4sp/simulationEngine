# Third-party notices

GR4SP's own source code — everything under `src/core/`, `experiments/` except
`experiments/EMAworkbench/`, `scripts/`, and the build files — is licensed under the MIT
licence in [`LICENSE.md`](LICENSE.md).

The repository also redistributes third-party components that are **not** covered by that
licence. They remain under the terms their authors set, listed below. Where a component is
vendored as source, its own copyright headers are authoritative.

## Java source vendored into `src/`

| Component | Files | Copyright | Licence |
|---|---|---|---|
| [MASON](https://cs.gmu.edu/~eclab/projects/mason/) simulation toolkit | `src/sim/field/network/stats/`, `src/sim/portrayal/network/stats/` | 2010 Sean Luke and George Mason University | Academic Free License v3.0 |
| [Mantissa](https://www.spaceroots.org/software/mantissa/) linear algebra | `src/sim/util/mantissa/` | 2001–2005 Luc Maisonobe | BSD-style (see the headers in those files) |

## Java libraries redistributed as jars in `libraries/`

All jars in `libraries/` are placed on the compile and runtime classpath by
`build.gradle` and copied, individually and unmodified, into `build/runtime-libs/`.

| Jar | Component | Licence | Used by GR4SP |
|---|---|---|---|
| `mason.19.jar` | MASON 19 | Academic Free License v3.0 | the simulation engine — scheduling, and the chart generator behind `savePlots` |
| `jfreechart-1.0.17.jar` | JFreeChart | **LGPL v2.1 or later** | plotting |
| `jcommon-1.0.21.jar` | JCommon | **LGPL v2.1 or later** | required by JFreeChart |

Seven further jars shipped with MASON — BeanShell, iText, the Java Media Framework, Java
3D (`j3dcore`, `j3dutils`), `vecmath` and `portfolio` — were removed in September 2026.
They carried no licence file and, in most cases, no manifest identifying them, and GR4SP
imported none of them: they back MASON's 3D portrayals and its interactive "save as PDF"
and movie-recording features, which GR4SP never invokes. Both a headless run and a
`reportGeneration: "full"` run (all 16 plots) were verified to complete without them, the
former producing output identical to the committed regression baseline.

### LGPL components

JFreeChart and JCommon are licensed under the GNU Lesser General Public Licence, version
2.1 or later. GR4SP links against them as a "work that uses the Library"; this does not
place GR4SP's own code under the LGPL.

To keep that arrangement compliant, each library is redistributed **unmodified and as a
separate jar**, so that anyone may substitute their own build of it. Do not merge these
jars into a single shaded or "fat" jar, and do not modify the libraries in place — changes
to the libraries themselves would have to be released under the LGPL.

Source for both, and the full licence text, are available from
<https://github.com/jfree/jfreechart> and <https://github.com/jfree/jcommon>.

## Python

| Component | Location | Licence |
|---|---|---|
| [EMA Workbench](https://github.com/quaquel/EMAworkbench) | `experiments/EMAworkbench/` | BSD 3-Clause |

The workbench is vendored as source without its upstream `LICENSE` file; the canonical
terms are those in the upstream repository.

## Data

Input data under `experiments/`, `csv/` and `backupDB/` derives from AEMO, the Australian
Bureau of Statistics, the Victorian Government, Open Electricity and Copernicus/ERA5, each
under its own terms of use. See `README.md` for the sources and `experiments/validation/`
for the provenance of the comparator series.
