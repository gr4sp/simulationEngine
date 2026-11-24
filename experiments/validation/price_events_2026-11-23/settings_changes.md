# Settings of the event runs (23 Nov 2026)

Each run copies `simulationSettings/` as at `v0.3` and changes only the lines below. Code: the frozen model plus
the opt-in generator parameter `endYear` (used only by the Hazelwood runs) and the monthly emissions column, neither of
which changes a base-case output (100 seeds rerun: every run file identical to `ensemble_2026-11-20`).

| Run | Seeds | Change |
|---|---|---|
| `nocarbon` | 1-20, 42 | `VIC.yaml`: `carbonPriceFile` commented out (no carbon price; recorded 1 July 2012 to 30 June 2014) |
| `hazelwood` | 1-20, 42 | `generator_parameters_VIC.csv`: added `Hazelwood,endYear,2019` (all seven units close 31 Dec 2019, not 31 Mar 2017) |
| `gas2015` | 1-20, 42 | `fuel_price_gas_VIC.csv`: every year from 2016 set to 4.150 $/GJ nominal (the 2015 value) |
| `hazelwood_gas2015` | 1-20, 42 | both of the last two changes |

The base case for the paired differences is the 100-seed rerun of the frozen model (same seeds).
Runner: `seed_ensemble.py EVENT_DIR 20 12` with `EVENT_DIR/settings` prepared as above; analysis: `price_events.py`.
