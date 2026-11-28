# Offer rules by generator, base case (R6 question of 28 Nov 2026)

Answers the article session's question on off-market units and the LCOE rule (ODD Table 4). Code and settings as at
v0.3.3 (`git diff v0.3.3 -- src simulationSettings` was empty before this branch), plus the output-only diagnostic
`-Dgr4sp.offerRuleLog=<csv>` (`Arena.writeOfferRuleLog`, `Generator.offerRule`, branch `feature/output-logs`).

- `off_s1/`: base case, seed 1, switch unset. `log_s1/`: the same with the switch set, plus
  `offer_rules_log.csv.gz` (one row per month and active generator, 1998-2050; Windows-1252 text).
- `offer_rules.md`: every number quoted in the answer, written by
  `python experiments/validation/offer_rules.py analyse experiments/validation/offer_rules_2026-11-28 1`.
  It checks both runs against `ensemble_2026-11-24/run_s1` (all three CSVs identical byte for byte), and reads the 100
  run folders of `ensemble_2026-11-24` for the 2019 off-market weight.
- `offer_rules_tariff_2019.csv`: the per-seed 2019 values behind the 100-seed table.
- Runs: `python experiments/validation/offer_rules.py run experiments/validation/offer_rules_2026-11-28 1` (about 4 min).
  The `_work/` folder the runs leave is not kept.

Points the tables need to be read correctly:
- `generated (MWh)` is the month's output of the unit. Off-market solar carries no weight in the off-market price: it
  enters with `getSolarSurplusCapacity()`, which is never set (Arena, off-market loop).
- The off-market LCOE price is `Generator.priceMWhLCOE`. Off-market units never bid (`bidsInSpot` = 0), so it is
  BasePrice / maxCapacityFactor, with BasePrice and the price cap raised by CPI every year (`Arena.applyInflation`).
  Ballarat Base Hospital (diesel, `generator_parameters_VIC.csv` l.75, capacity factor 0) produces nothing, and its
  price sits at the inflated cap (13,657 $/MWh in 2019) with no weight.
