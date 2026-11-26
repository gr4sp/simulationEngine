# Order of SimPolicies and the spot Arena (26 Nov 2026)

R6 finding 1 (notes repository, `docs/methodsx_sequences/README.md`). `SimPolicies` and the arenas share MASON ordering 0
and MASON shuffles equal orderings, so rooftop PV installed in a month enters the market that month or the next,
depending on the seed. The test switch `-Dgr4sp.policiesOrder=before|after` (`Gr4spSim.start`, commit `847790f`) runs
`SimPolicies` always before or always after the arenas. Both variants draw the same random numbers, so after minus
before is the effect of the order alone. The base case (switch unset) is late in about half the months.

- `before_s<seed>/`, `after_s<seed>/`: year summary, monthly summary and monthly prices of each run; seeds 42 and 1-5.
- `default_s42/`: switch unset. Byte-identical to `ensemble_2026-11-24/run_s42` (local) and equal to the committed
  `ensemble_2026-11-24/seed_years.csv` (seed 42).
- `order_test.md`: every table and number quoted in the notes and in REPO_REQUESTS R6; `order_test_years.csv`: the same
  per seed and year.

Code: `gr4sp` `847790f` (v0.3.3 model, `simulationSettings/` as committed, database `DB-2021-8-21`), Java 17.

Re-derive the tables from these files:

    python experiments/validation/order_test.py compare experiments/validation/order_test_2026-11-26

Rerun the models (about 2 minutes per run; build first with `./gradlew compileJava`):

    python experiments/validation/order_test.py run OUT_DIR 42,1,2,3,4,5 6
