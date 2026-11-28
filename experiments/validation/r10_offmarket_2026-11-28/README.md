# R10: off-market units and demand (28 Nov 2026)

`r10_offmarket.md` holds every number of the R10 answer, written by
`python experiments/validation/r10_offmarket.py experiments/validation/r10_offmarket_2026-11-28`.
Inputs, all committed:
- `../offer_rules_2026-11-28/log_s1/offer_rules_log.csv.gz` (seed 1, `629d33e`);
- `generationassets_VIC.csv`: the Victorian rows of `gr4spdb.generationassets` (DB-2021-8-21), exported 28 Nov 2026;
- `generation_consumption_historic_1998_2020.csv`: the monthly record the demand is calibrated to;
- `pricesetter_s1/`: seed 1, base case, run with `-Dgr4sp.priceSetterLog=... -Dgr4sp.priceSetterLog.from=1999
  -Dgr4sp.priceSetterLog.to=1999` (code of `923d19f`); its three CSVs are identical byte for byte to
  `ensemble_2026-11-24/run_s1` (local run folder). The `_work/` folder is not kept.
