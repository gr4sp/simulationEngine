# Half-hourly output switch: checks (R9, 28 Nov 2026)

Base case, seed 1, branch `feature/output-logs` (code and settings of v0.3.3 plus the output-only switch
`halfHourOutput`, `Arena.writeHalfHour`). `half_hour_output.md` holds every number of the R9 answer, written by
`python experiments/validation/half_hour_output.py analyse experiments/validation/half_hour_output_2026-11-28 1`.

- `off_s1/`: `VIC.yaml` as is. `on_s1/`: `VIC.yaml` plus `halfHourOutput: true`. Each keeps the three monthly and yearly
  CSVs, `stdout.txt` and `run_time.json` (wall clock; the two runs ran one after the other).
- `on_s1/VICSimDataHalfHour_seed_1.csv` (346 MB, 1999-2050) is not committed (`.gitignore`).
  `python experiments/validation/half_hour_output.py run experiments/validation/half_hour_output_2026-11-28 1` regenerates
  both runs (about 9 min). The `_work/` folder the runs leave is not kept.
- The byte check is against `ensemble_2026-11-24/run_s1`, a local run folder (the ensemble's committed files are its
  per-seed summaries).
