# Saltelli sample size: subsampling the thesis's hypothetical-past ensemble (R3 step 4, 11 Nov 2026)

`saltelli_convergence.py` on `gr4sp_SOBOLhypopast2021-Mar-03_includ_wholesale_month.tar.gz` (k = 24, N = 2,099 after
dropping block 827 with the two empty runs). Total-order ST, median over 1999-2019, each N compared with the full sample.
Numbers: `saltelli_convergence.csv`.

| Output | Top five same as full from | Top four same from | Rho over the top ten >= 0.9 from |
|---|---|---|---|
| Wholesale price | N = 64 | N = 64 | N = 64 |
| Tariff | N = 64 (order from 128) | N = 64 | N = 64 |
| GHGE | N = 64 | N = 64 | N = 1,200 (0.77-0.82 below) |
| RE share | N = 1,200 | N = 64 | N = 1,200 (0.67-0.71 below) |

Reading:
- The main parameters are found with small samples: the top four of every output are the same from N = 64, and the
  mean ST confidence half-width falls to about 0.015 at N = 256 and 0.010 at N = 512.
- What needs N >= 1,200 is the order of small, near-tied effects: the fifth place of RE share (wind, OCGT,
  semiScheduleGenSpotMarket and nonScheduleMinCapMarketGen alternate) and places 6-10 of GHGE.
- The N = 1,200 pass overstates convergence: its runs are 57 % of the full sample it is compared with.
- This is the old model's convergence. The repaired model has different active parameters (`PARAMETER_AUDIT_R3.md`),
  so it shows the sample size to plan for, not a guarantee.

Cost of a redesigned rerun with k = 29 (2k + 2 = 60 runs per base sample): N = 256 is 15,360 runs, N = 512 is 30,720,
N = 1,200 is 72,000, against 125,940 for the thesis's full size. Suggested: run N = 256, then extend the same Sobol
sequence to N = 512 and stop if the top five and the ST intervals hold.

Against the parameter audit: in this design, two of the three most influential parameters for wholesale price and tariff
(`priceChangePercentageOcgt`, `importPriceFactor`) and two of the top five for GHGE and RE share
(`priceChangePercentageWater`, `priceChangePercentageBrownCoal`) are inactive in the repaired model. The old rankings
cannot carry over for any of the four outputs.
