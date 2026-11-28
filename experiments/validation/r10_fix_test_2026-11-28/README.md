# R10: the two candidate fixes, measured (28 Nov 2026, branch test/r10-fixes)

`r10_fix_test.md` holds every number, written by
`python experiments/validation/r10_fix_test.py compare experiments/validation/r10_fix_test_2026-11-28`; the per-seed
differences are in `r10_fix_test_years.csv`. Runs: `python experiments/validation/r10_fix_test.py run
experiments/validation/r10_fix_test_2026-11-28 42,1,2,3,4,5 6` (about 20 min). Each `<variant>_s<seed>/` keeps the
three CSVs and stdout; the `_work/` folder is not kept.

- fix1: wind outside the record (test-only switch `-Dgr4sp.windOutsideRecord=true`, `LoadData.unrecordedOffMarketGWh`).
- fix2: `minCapMarketGen: 0` for scheduled and semi-scheduled units in `VIC.yaml` and `VICfuture.yaml` (settings only).
- both: fix1 + fix2. base_s42: both off; identical byte for byte to `ensemble_2026-11-24/run_s42`.

The base of every comparison is `ensemble_2026-11-24/run_s<seed>`, a local run folder (the ensemble's committed files
are its per-seed summaries). Neither fix is in a release branch.
