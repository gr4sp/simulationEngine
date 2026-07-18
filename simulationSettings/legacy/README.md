# Retired settings files

These YAMLs are kept for the historical record only. The simulation engine can
not load them: `Gr4spSim.simulParametres()` hardcodes `yamlFileName = "VIC"`,
so only `VIC.yaml` (+ `VICfuture.yaml`) is ever read, and the published 2021
scenario runs (JT / LCT / ST / BAU, Zenodo record 8320754) were parameterised
in-memory through the EMA workbench pipeline (`experiments/connector.py`), not
through per-scenario YAML files.

| File | Why retired |
|------|-------------|
| `BAURegional.yaml` | Pre-2021 Settings schema (flat `arena:` block, `priceMinMWh` price model) — no longer parses. |
| `BAUVIC26.yaml` | Pre-2021 schema (`priceMinMWh`) — no longer parses. |
| `JTVIC.yaml` | Pre-2021 schema (`priceMinMWh`, `IncludePublicallyAnnouncedGen` casing) — no longer parses. |
| `RTVIC.yaml` | Same as JTVIC. |
| `LCTVIC.yaml` | Parses, but is identical to `VIC.yaml` except three comment lines — no scenario content. |
| `LCTVICfuture.yaml` | Near-identical duplicate of `VICfuture.yaml`. |

The old `priceMinMWh` values have different semantics than the current
`basePriceMWh` LCOE model, so a mechanical field rename would silently change
scenario economics — if one of these scenarios is ever needed again, re-derive
its parameters against the current schema (see `VIC.yaml`) instead of copying
values across.

`SettingsYamlTest` validates every YAML remaining in `simulationSettings/`
against the current schema; this folder is deliberately outside its scan.
