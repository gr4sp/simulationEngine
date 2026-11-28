# R11: Monte Carlo error and coverage of the seed band (ensemble_2026-11-28)

Written by `experiments/validation/r11_mc_error.py`. 100 seeds; bootstrap of the seeds, B = 2000 resamples, numpy default_rng(20261128). Monte Carlo SE (MCSE) = standard deviation of the summary over the resamples. 'Flag' = MCSE above half a unit of the last decimal the article prints. Seeds needed: seeds x (largest MCSE of the row / target)^2. 'Decimals supported': the most decimals with the row's largest MCSE at most half a unit of the last one (0: whole units). A band of width 0 (an indicator the seeds do not move: wind, which runs at a constant capacity factor) cannot cover the record by construction. Units: NSE and r none; Bias, RMSE and MAE in the indicator's unit (price $/MWh, tariff c/kWh, emissions Mt CO2-e, generation GWh, shares pp); NRMSE and the duration shares in %; median half-hourly price $/MWh.

## 1-2. Monte Carlo SE of every reported value

| Table | Row | Metric | Printed decimals | Median (MCSE) | 5th percentile (MCSE) | 95th percentile (MCSE) | Flag | Decimals supported | Seeds for printed decimal | Seeds for MCSE 0.05 |
|---|---|---|---|---|---|---|---|---|---|---|
| tbl:stats | Wholesale price, monthly | NSE | 2 | -0.266 (0.0466) | -0.935 (0.1220) | 0.067 (0.0361) | median, p05, p95 | 0 | 59566 | 596 |
| tbl:stats | Wholesale price, monthly | Bias | 1 | -12.32 (0.1620) | -14.00 (0.2853) | -10.42 (0.2968) | median, p05, p95 | 0 | 3524 | 3524 |
| tbl:stats | Wholesale price, monthly | RMSE | 1 | 33.17 (0.6127) | 28.47 (0.5538) | 41.01 (1.3209) | median, p05, p95 | -1 | 69788 | 69788 |
| tbl:stats | Wholesale price, monthly | r | 2 | 0.564 (0.0073) | 0.467 (0.0113) | 0.670 (0.0109) | median, p05, p95 | 1 | 514 | 6 |
| tbl:stats | Wholesale price, annual | NSE | 2 | 0.399 (0.0109) | 0.258 (0.0135) | 0.536 (0.0200) | median, p05, p95 | 1 | 1599 | 16 |
| tbl:stats | Wholesale price, annual | Bias | 1 | -12.60 (0.2004) | -14.41 (0.3192) | -10.37 (0.3937) | median, p05, p95 | 0 | 6202 | 6202 |
| tbl:stats | Wholesale price, annual | RMSE | 1 | 17.74 (0.1571) | 15.59 (0.3308) | 19.71 (0.1791) | median, p05, p95 | 0 | 4377 | 4377 |
| tbl:stats | Wholesale price, annual | r | 2 | 0.850 (0.0057) | 0.777 (0.0113) | 0.899 (0.0064) | median, p05, p95 | 1 | 509 | 6 |
| tbl:stats | Renewable energy share, annual | NSE | 2 | 0.969 (0.0001) | 0.968 (0.0001) | 0.972 (0.0005) | - | 2 | 2 | 1 |
| tbl:stats | Renewable energy share, annual | Bias | 2 | 0.104 (0.0014) | 0.079 (0.0049) | 0.120 (0.0021) | - | 2 | 96 | 1 |
| tbl:stats | Renewable energy share, annual | RMSE | 2 | 1.088 (0.0020) | 1.042 (0.0091) | 1.106 (0.0024) | p05 | 1 | 335 | 4 |
| tbl:stats | Renewable energy share, annual | r | 2 | 0.988 (0.0000) | 0.987 (0.0000) | 0.988 (0.0002) | - | 3 | 1 | 1 |
| tbl:stats | Tariff, against Oakley Greenwood | NSE | 2 | 0.515 (0.0042) | 0.485 (0.0118) | 0.559 (0.0109) | p05, p95 | 1 | 556 | 6 |
| tbl:stats | Tariff, against Oakley Greenwood | Bias | 1 | -3.32 (0.0218) | -3.49 (0.0152) | -2.96 (0.0524) | p95 | 0 | 110 | 110 |
| tbl:stats | Tariff, against Oakley Greenwood | RMSE | 1 | 3.85 (0.0168) | 3.66 (0.0458) | 3.96 (0.0428) | - | 1 | 85 | 85 |
| tbl:stats | Tariff, against Oakley Greenwood | r | 2 | 0.940 (0.0010) | 0.913 (0.0126) | 0.949 (0.0010) | p05 | 1 | 638 | 7 |
| tbl:stats | Tariff, against St Vincent de Paul | NSE | 2 | -52.604 (0.3015) | -56.479 (0.5708) | -50.251 (0.2131) | median, p05, p95 | -1 | 1303447 | 13035 |
| tbl:stats | Tariff, against St Vincent de Paul | Bias | 1 | 11.18 (0.0327) | 10.87 (0.0251) | 11.60 (0.0730) | p95 | 0 | 214 | 214 |
| tbl:stats | Tariff, against St Vincent de Paul | RMSE | 1 | 11.49 (0.0324) | 11.23 (0.0236) | 11.89 (0.0592) | p95 | 0 | 141 | 141 |
| tbl:stats | Tariff, against St Vincent de Paul | r | 2 | 0.284 (0.0096) | 0.173 (0.0205) | 0.469 (0.0222) | median, p05, p95 | 1 | 1965 | 20 |
| tbl:stats | Emissions, 2005-2018 | NSE | 2 | 0.926 (0.0005) | 0.917 (0.0011) | 0.932 (0.0012) | - | 2 | 6 | 1 |
| tbl:stats | Emissions, 2005-2018 | Bias | 2 | 0.070 (0.0064) | 0.004 (0.0120) | 0.134 (0.0070) | median, p05, p95 | 1 | 572 | 6 |
| tbl:stats | Emissions, 2005-2018 | RMSE | 2 | 1.409 (0.0046) | 1.348 (0.0117) | 1.487 (0.0094) | p05, p95 | 1 | 545 | 6 |
| tbl:stats | Emissions, 2005-2018 | r | 2 | 0.968 (0.0002) | 0.964 (0.0009) | 0.971 (0.0003) | - | 2 | 3 | 1 |
| tbl:stats | Emissions, 2000-2018 | NSE | 2 | 0.711 (0.0008) | 0.700 (0.0014) | 0.718 (0.0009) | - | 2 | 9 | 1 |
| tbl:stats | Emissions, 2000-2018 | Bias | 2 | -0.997 (0.0041) | -1.051 (0.0078) | -0.948 (0.0074) | p05, p95 | 1 | 242 | 3 |
| tbl:stats | Emissions, 2000-2018 | RMSE | 2 | 2.433 (0.0033) | 2.403 (0.0039) | 2.480 (0.0058) | p95 | 1 | 137 | 2 |
| tbl:stats | Emissions, 2000-2018 | r | 2 | 0.895 (0.0004) | 0.891 (0.0006) | 0.899 (0.0002) | - | 2 | 2 | 1 |
| tbl:fuel | Brown coal | NSE | 2 | 0.885 (0.0012) | 0.873 (0.0007) | 0.895 (0.0012) | - | 2 | 6 | 1 |
| tbl:fuel | Brown coal | Bias | 1 | 61.00 (0.9414) | 50.72 (1.4361) | 70.28 (0.8871) | median, p05, p95 | -1 | 82497 | 82497 |
| tbl:fuel | Brown coal | RMSE | 1 | 189.89 (0.9896) | 181.67 (1.0514) | 199.63 (0.5780) | median, p05, p95 | -1 | 44219 | 44219 |
| tbl:fuel | Brown coal | r | 2 | 0.947 (0.0006) | 0.941 (0.0007) | 0.953 (0.0010) | - | 2 | 5 | 1 |
| tbl:fuel | Gas | NSE | 2 | -1.346 (0.0240) | -1.714 (0.0774) | -1.065 (0.0444) | median, p05, p95 | 0 | 23939 | 240 |
| tbl:fuel | Gas | Bias | 1 | 9.04 (0.9510) | 0.38 (0.9194) | 19.56 (1.5307) | median, p05, p95 | -1 | 93726 | 93726 |
| tbl:fuel | Gas | RMSE | 1 | 155.96 (0.7949) | 146.34 (1.5418) | 167.75 (2.4124) | median, p05, p95 | -1 | 232780 | 232780 |
| tbl:fuel | Gas | r | 2 | 0.264 (0.0033) | 0.212 (0.0034) | 0.317 (0.0087) | p95 | 1 | 305 | 4 |
| tbl:fuel | Wind | NSE | 2 | 0.838 (0.0000) | 0.838 (0.0000) | 0.838 (0.0000) | - | 4 | 1 | 1 |
| tbl:fuel | Wind | Bias | 1 | 8.68 (0.0021) | 8.68 (0.0001) | 8.70 (0.0002) | - | 2 | 1 | 1 |
| tbl:fuel | Wind | RMSE | 1 | 61.31 (0.0025) | 61.31 (0.0001) | 61.33 (0.0003) | - | 2 | 1 | 1 |
| tbl:fuel | Wind | r | 2 | 0.921 (0.0000) | 0.921 (0.0000) | 0.921 (0.0000) | - | 5 | 1 | 1 |
| tbl:fuel | Hydro | NSE | 2 | 0.977 (0.0009) | 0.965 (0.0026) | 0.986 (0.0007) | - | 2 | 28 | 1 |
| tbl:fuel | Hydro | Bias | 1 | -3.37 (0.0599) | -4.33 (0.1438) | -2.56 (0.0668) | median, p05, p95 | 0 | 827 | 827 |
| tbl:fuel | Hydro | RMSE | 1 | 13.65 (0.2590) | 10.80 (0.2877) | 16.93 (0.6259) | median, p05, p95 | -1 | 15670 | 15670 |
| tbl:fuel | Hydro | r | 2 | 0.989 (0.0004) | 0.983 (0.0012) | 0.993 (0.0004) | - | 2 | 7 | 1 |
| tbl:fuel | Rooftop solar | NSE | 2 | 0.980 (0.0001) | 0.979 (0.0001) | 0.981 (0.0001) | - | 3 | 1 | 1 |
| tbl:fuel | Rooftop solar | Bias | 1 | 0.96 (0.0111) | 0.84 (0.0244) | 1.06 (0.0117) | - | 1 | 24 | 24 |
| tbl:fuel | Rooftop solar | RMSE | 1 | 9.55 (0.0132) | 9.39 (0.0176) | 9.77 (0.0284) | - | 1 | 33 | 33 |
| tbl:fuel | Rooftop solar | r | 2 | 0.991 (0.0000) | 0.991 (0.0001) | 0.991 (0.0000) | - | 3 | 1 | 1 |
| tbl:fuel | Renewable energy share, monthly | NSE | 2 | 0.938 (0.0001) | 0.937 (0.0003) | 0.940 (0.0002) | - | 3 | 1 | 1 |
| tbl:fuel | Renewable energy share, monthly | Bias | 2 | -0.020 (0.0014) | -0.043 (0.0036) | -0.003 (0.0020) | - | 2 | 54 | 1 |
| tbl:fuel | Renewable energy share, monthly | RMSE | 2 | 1.544 (0.0015) | 1.529 (0.0023) | 1.563 (0.0043) | - | 2 | 73 | 1 |
| tbl:fuel | Renewable energy share, monthly | r | 2 | 0.970 (0.0000) | 0.969 (0.0002) | 0.970 (0.0001) | - | 3 | 1 | 1 |
| supplement | Wholesale price, monthly | MAE | 1 | 20.44 (0.1051) | 19.09 (0.1393) | 22.49 (0.2823) | median, p05, p95 | 0 | 3187 | 3187 |
| supplement | Wholesale price, monthly | NRMSE | 2 | 72.054 (1.3418) | 61.845 (1.2125) | 89.075 (2.7446) | median, p05, p95 | -1 | 30131697 | 301317 |
| supplement | Wholesale price, annual | MAE | 1 | 14.82 (0.1587) | 12.89 (0.2809) | 16.36 (0.1228) | median, p05, p95 | 0 | 3156 | 3156 |
| supplement | Wholesale price, annual | NRMSE | 2 | 38.275 (0.3439) | 33.633 (0.7224) | 42.525 (0.3987) | median, p05, p95 | -1 | 2087172 | 20872 |
| supplement | Renewable energy share, annual | MAE | 2 | 0.615 (0.0014) | 0.591 (0.0034) | 0.632 (0.0021) | - | 2 | 46 | 1 |
| supplement | Renewable energy share, annual | NRMSE | 2 | 10.099 (0.0182) | 9.671 (0.0888) | 10.261 (0.0219) | median, p05, p95 | 0 | 31542 | 316 |
| supplement | Tariff, against Oakley Greenwood | MAE | 1 | 3.33 (0.0189) | 3.09 (0.0389) | 3.50 (0.0429) | - | 1 | 74 | 74 |
| supplement | Tariff, against Oakley Greenwood | NRMSE | 2 | 11.934 (0.0518) | 11.370 (0.1451) | 12.298 (0.1379) | median, p05, p95 | 0 | 84208 | 843 |
| supplement | Tariff, against St Vincent de Paul | MAE | 1 | 11.18 (0.0324) | 10.87 (0.0249) | 11.60 (0.0713) | p95 | 0 | 204 | 204 |
| supplement | Tariff, against St Vincent de Paul | NRMSE | 2 | 54.009 (0.1504) | 52.810 (0.1170) | 55.927 (0.2625) | median, p05, p95 | 0 | 275564 | 2756 |
| supplement | Emissions, 2005-2018 | MAE | 2 | 1.103 (0.0037) | 1.039 (0.0055) | 1.168 (0.0065) | p05, p95 | 1 | 167 | 2 |
| supplement | Emissions, 2005-2018 | NRMSE | 2 | 2.306 (0.0072) | 2.206 (0.0190) | 2.434 (0.0156) | median, p05, p95 | 1 | 1448 | 15 |
| supplement | Emissions, 2000-2018 | MAE | 2 | 1.859 (0.0036) | 1.815 (0.0053) | 1.913 (0.0048) | p05 | 1 | 114 | 2 |
| supplement | Emissions, 2000-2018 | NRMSE | 2 | 3.960 (0.0053) | 3.913 (0.0064) | 4.037 (0.0097) | median, p05, p95 | 1 | 373 | 4 |
| supplement | Brown coal | MAE | 1 | 142.44 (0.6179) | 136.13 (1.1397) | 150.51 (1.0003) | median, p05, p95 | -1 | 51961 | 51961 |
| supplement | Brown coal | NRMSE | 2 | 4.942 (0.0261) | 4.728 (0.0276) | 5.195 (0.0151) | median, p05, p95 | 1 | 3054 | 31 |
| supplement | Gas | MAE | 1 | 120.72 (0.4776) | 114.47 (1.0308) | 129.11 (1.5666) | median, p05, p95 | -1 | 98169 | 98169 |
| supplement | Gas | NRMSE | 2 | 105.895 (0.5450) | 99.367 (1.0948) | 113.900 (1.6371) | median, p05, p95 | -1 | 10721004 | 107211 |
| supplement | Wind | MAE | 1 | 43.18 (0.0022) | 43.18 (0.0001) | 43.19 (0.0002) | - | 2 | 1 | 1 |
| supplement | Wind | NRMSE | 2 | 35.047 (0.0014) | 35.046 (0.0001) | 35.055 (0.0002) | - | 2 | 9 | 1 |
| supplement | Hydro | MAE | 1 | 3.37 (0.0618) | 2.56 (0.0684) | 4.33 (0.1425) | median, p05, p95 | 0 | 813 | 813 |
| supplement | Hydro | NRMSE | 2 | 6.380 (0.1182) | 5.051 (0.1321) | 7.915 (0.2987) | median, p05, p95 | 0 | 356914 | 3570 |
| supplement | Rooftop solar | MAE | 1 | 4.50 (0.0079) | 4.39 (0.0114) | 4.59 (0.0112) | - | 1 | 6 | 6 |
| supplement | Rooftop solar | NRMSE | 2 | 16.910 (0.0238) | 16.622 (0.0302) | 17.297 (0.0498) | median, p05, p95 | 1 | 9902 | 100 |
| supplement | Renewable energy share, monthly | MAE | 2 | 1.035 (0.0019) | 1.019 (0.0021) | 1.051 (0.0033) | - | 2 | 45 | 1 |
| supplement | Renewable energy share, monthly | NRMSE | 2 | 14.746 (0.0138) | 14.606 (0.0216) | 14.930 (0.0399) | median, p05, p95 | 1 | 6373 | 64 |
| text (price basis) | B: time vs time (main table), monthly | NSE | 2 | -0.203 (0.0419) | -0.905 (0.1444) | 0.082 (0.0330) | median, p05, p95 | 0 | 83417 | 835 |
| text (price basis) | B: time vs time (main table), monthly | Bias | 1 | -12.16 (0.1582) | -13.76 (0.1949) | -10.11 (0.2925) | median, p05, p95 | 0 | 3422 | 3422 |
| text (price basis) | B: time vs time (main table), monthly | RMSE | 1 | 32.01 (0.5590) | 27.96 (0.4917) | 40.28 (1.4416) | median, p05, p95 | -1 | 83129 | 83129 |
| text (price basis) | B: time vs time (main table), annual | NSE | 2 | 0.430 (0.0085) | 0.343 (0.0072) | 0.553 (0.0155) | median, p05, p95 | 1 | 966 | 10 |
| text (price basis) | B: time vs time (main table), annual | Bias | 1 | -12.16 (0.1535) | -13.76 (0.1987) | -10.11 (0.2919) | median, p05, p95 | 0 | 3410 | 3410 |
| text (price basis) | B: time vs time (main table), annual | RMSE | 1 | 17.54 (0.1306) | 15.53 (0.2786) | 18.83 (0.1051) | median, p05, p95 | 0 | 3106 | 3106 |
| text (price basis) | C: demand vs demand (supplement), monthly | NSE | 2 | -0.443 (0.0576) | -1.397 (0.1562) | 0.021 (0.0527) | median, p05, p95 | 0 | 97649 | 977 |
| text (price basis) | C: demand vs demand (supplement), monthly | Bias | 1 | -12.44 (0.1666) | -15.12 (0.3418) | -9.55 (0.4636) | median, p05, p95 | 0 | 8598 | 8598 |
| text (price basis) | C: demand vs demand (supplement), monthly | RMSE | 1 | 42.73 (0.8376) | 35.20 (0.9729) | 55.09 (1.8191) | median, p05, p95 | -1 | 132371 | 132371 |
| text (price basis) | C: demand vs demand (supplement), annual | NSE | 2 | 0.398 (0.0117) | 0.257 (0.0087) | 0.551 (0.0171) | median, p05, p95 | 1 | 1165 | 12 |
| text (price basis) | C: demand vs demand (supplement), annual | Bias | 1 | -12.44 (0.1689) | -15.12 (0.3374) | -9.55 (0.4459) | median, p05, p95 | 0 | 7955 | 7955 |
| text (price basis) | C: demand vs demand (supplement), annual | RMSE | 1 | 19.91 (0.1992) | 17.20 (0.3370) | 22.12 (0.1289) | median, p05, p95 | 0 | 4542 | 4542 |
| tbl:duration | 1999-2008 | below15 | 0 | 70.1 (0.1342) | 68.7 (0.2127) | 71.4 (0.1074) | - | 0 | 19 | 1810 |
| tbl:duration | 1999-2008 | median_price | 0 | 10.0 (0.0000) | 9.9 (0.0016) | 10.0 (0.0000) | - | 2 | 1 | 1 |
| tbl:duration | 1999-2008 | above300 | 2 | 0.034 (0.0020) | 0.016 (0.0025) | 0.060 (0.0072) | p95 | 1 | 210 | 3 |
| tbl:duration | 2009-2016 | below15 | 0 | 41.9 (0.2071) | 39.5 (0.1772) | 44.0 (0.2145) | - | 0 | 19 | 1841 |
| tbl:duration | 2009-2016 | median_price | 0 | 24.5 (0.2329) | 22.0 (0.3750) | 27.7 (0.3680) | - | 0 | 57 | 5625 |
| tbl:duration | 2009-2016 | above300 | 2 | 0.079 (0.0058) | 0.040 (0.0054) | 0.191 (0.0135) | median, p05, p95 | 1 | 727 | 8 |
| tbl:duration | 2017-2019 | below15 | 0 | 35.7 (0.1639) | 33.5 (0.3719) | 37.6 (0.1126) | - | 0 | 56 | 5534 |
| tbl:duration | 2017-2019 | median_price | 0 | 83.1 (0.9382) | 72.3 (1.9508) | 92.7 (0.9023) | median, p05, p95 | -1 | 1523 | 152233 |
| tbl:duration | 2017-2019 | above300 | 2 | 0.205 (0.0060) | 0.124 (0.0096) | 0.449 (0.0443) | median, p05, p95 | 1 | 7847 | 79 |
| tbl:duration | 1999-2019 | below15 | 0 | 54.4 (0.1003) | 53.3 (0.1400) | 55.6 (0.1750) | - | 0 | 13 | 1225 |
| tbl:duration | 1999-2019 | median_price | 0 | 13.3 (0.0529) | 13.0 (0.0000) | 13.5 (0.0322) | - | 0 | 2 | 112 |
| tbl:duration | 1999-2019 | above300 | 2 | 0.085 (0.0032) | 0.059 (0.0019) | 0.138 (0.0100) | p95 | 1 | 400 | 4 |
| tbl:index | Recursive intensity vs NGA scope 2 plus losses | NSE | 2 | 0.433 (0.0011) | 0.412 (0.0047) | 0.450 (0.0015) | - | 2 | 89 | 1 |
| tbl:index | Recursive intensity vs NGA scope 2 plus losses | mean relative bias (%) | 1 | -5.31 (0.0075) | - | - | - | 1 | 3 | 3 |
| tbl:index | Recursive intensity vs NGA scope 2 plus losses | r | 2 | 0.933 (0.0003) | - | - | - | 3 | 1 | 1 |
| tbl:index | System intensity vs NGA scope 2 | NSE | 2 | 0.635 (0.0006) | 0.616 (0.0035) | 0.648 (0.0013) | - | 2 | 48 | 1 |
| tbl:index | System intensity vs NGA scope 2 | mean relative bias (%) | 1 | -3.99 (0.0076) | - | - | - | 1 | 3 | 3 |
| tbl:index | System intensity vs NGA scope 2 | r | 2 | 0.949 (0.0002) | - | - | - | 3 | 1 | 1 |
| tbl:index | System intensity vs AEMO CDEII | NSE | 2 | 0.230 (0.0022) | 0.204 (0.0026) | 0.251 (0.0035) | - | 2 | 51 | 1 |
| tbl:index | System intensity vs AEMO CDEII | mean relative bias (%) | 1 | -7.77 (0.0105) | - | - | - | 1 | 5 | 5 |
| tbl:index | System intensity vs AEMO CDEII | r | 2 | 0.954 (0.0004) | - | - | - | 3 | 1 | 1 |

### Largest MCSE per column of each table

| Table | Metric | Largest MCSE, median | Largest MCSE, 5th percentile | Largest MCSE, 95th percentile |
|---|---|---|---|---|
| tbl:stats | NSE | 0.3015 (Tariff, against St Vincent de Paul) | 0.5708 (Tariff, against St Vincent de Paul) | 0.2131 (Tariff, against St Vincent de Paul) |
| tbl:stats | Bias | 0.2004 (Wholesale price, annual) | 0.3192 (Wholesale price, annual) | 0.3937 (Wholesale price, annual) |
| tbl:stats | RMSE | 0.6127 (Wholesale price, monthly) | 0.5538 (Wholesale price, monthly) | 1.3209 (Wholesale price, monthly) |
| tbl:stats | r | 0.0096 (Tariff, against St Vincent de Paul) | 0.0205 (Tariff, against St Vincent de Paul) | 0.0222 (Tariff, against St Vincent de Paul) |
| tbl:fuel | NSE | 0.0240 (Gas) | 0.0774 (Gas) | 0.0444 (Gas) |
| tbl:fuel | Bias | 0.9510 (Gas) | 1.4361 (Brown coal) | 1.5307 (Gas) |
| tbl:fuel | RMSE | 0.9896 (Brown coal) | 1.5418 (Gas) | 2.4124 (Gas) |
| tbl:fuel | r | 0.0033 (Gas) | 0.0034 (Gas) | 0.0087 (Gas) |
| supplement | MAE | 0.6179 (Brown coal) | 1.1397 (Brown coal) | 1.5666 (Gas) |
| supplement | NRMSE | 1.3418 (Wholesale price, monthly) | 1.2125 (Wholesale price, monthly) | 2.7446 (Wholesale price, monthly) |
| text (price basis) | NSE | 0.0576 (C: demand vs demand (supplement), monthly) | 0.1562 (C: demand vs demand (supplement), monthly) | 0.0527 (C: demand vs demand (supplement), monthly) |
| text (price basis) | Bias | 0.1689 (C: demand vs demand (supplement), annual) | 0.3418 (C: demand vs demand (supplement), monthly) | 0.4636 (C: demand vs demand (supplement), monthly) |
| text (price basis) | RMSE | 0.8376 (C: demand vs demand (supplement), monthly) | 0.9729 (C: demand vs demand (supplement), monthly) | 1.8191 (C: demand vs demand (supplement), monthly) |
| tbl:duration | below15 | 0.2071 (2009-2016) | 0.3719 (2017-2019) | 0.2145 (2009-2016) |
| tbl:duration | median_price | 0.9382 (2017-2019) | 1.9508 (2017-2019) | 0.9023 (2017-2019) |
| tbl:duration | above300 | 0.0060 (2017-2019) | 0.0096 (2017-2019) | 0.0443 (2017-2019) |
| tbl:index | NSE | 0.0022 (System intensity vs AEMO CDEII) | 0.0047 (Recursive intensity vs NGA scope 2 plus losses) | 0.0035 (System intensity vs AEMO CDEII) |
| tbl:index | mean relative bias (%) | 0.0105 (System intensity vs AEMO CDEII) | - | - |
| tbl:index | r | 0.0004 (System intensity vs AEMO CDEII) | - | - |

## 3. Coverage: record inside the 5-95 % band of the seeds

| Indicator | Periods (count) | Inside (count) | Inside (%) | Record above the band (count) | Record below the band (count) | Periods outside, above | Periods outside, below |
|---|---|---|---|---|---|---|---|
| Wholesale price (monthly) | 258 | 90 | 35 | 154 | 14 | 1999-01, 1999-02, 1999-03, 1999-04, 1999-05, 1999-06, 1999-07, 1999-08, 1999-09, 1999-10, 1999-11, 1999-12, ... (154 in all; r11_coverage.csv) | 2006-12, 2007-02, 2008-12, 2009-01, 2011-10, 2011-12, 2012-01, 2012-02, 2014-04, 2015-01, 2017-03, 2018-06, ... (14 in all; r11_coverage.csv) |
| Wholesale price (annual) | 22 | 7 | 32 | 14 | 1 | 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2010, 2013, 2015, ... (14 in all; r11_coverage.csv) | 2009 |
| RE share (annual) | 16 | 2 | 12 | 8 | 6 | 2009, 2010, 2011, 2012, 2013, 2016, 2018, 2019 | 2007, 2008, 2014, 2015, 2017, 2020 |
| Tariffs vs Oakley Greenwood | 13 | 1 | 8 | 12 | 0 | 2001, 2002, 2007, 2008, 2009, 2011, 2012, 2013, 2014, 2015, 2016, 2017 | - |
| Tariffs vs St Vincent de Paul | 10 | 0 | 0 | 0 | 10 | - | 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 |
| Coal generation (monthly) | 183 | 99 | 54 | 24 | 60 | 2005-12, 2006-12, 2008-02, 2008-12, 2009-03, 2009-06, 2009-12, 2010-01, 2010-03, 2010-07, 2011-10, 2011-12, ... (24 in all; r11_coverage.csv) | 2005-04, 2005-09, 2005-11, 2006-09, 2006-10, 2007-04, 2007-05, 2007-06, 2007-11, 2007-12, 2008-04, 2008-05, ... (60 in all; r11_coverage.csv) |
| Gas generation (monthly) | 183 | 87 | 48 | 48 | 48 | 2005-04, 2007-04, 2007-05, 2007-11, 2007-12, 2008-04, 2008-05, 2012-06, 2012-07, 2012-08, 2012-09, 2012-10, ... (48 in all; r11_coverage.csv) | 2005-05, 2005-06, 2005-07, 2005-08, 2005-11, 2005-12, 2006-01, 2006-02, 2006-03, 2006-05, 2006-06, 2006-07, ... (48 in all; r11_coverage.csv) |
| Wind generation (monthly) | 183 | 0 | 0 | 68 | 115 | 2009-07, 2009-08, 2009-09, 2009-10, 2009-11, 2009-12, 2010-01, 2010-02, 2010-03, 2010-04, 2010-06, 2010-08, ... (68 in all; r11_coverage.csv) | 2005-04, 2005-05, 2005-06, 2005-07, 2005-08, 2005-09, 2005-10, 2005-11, 2005-12, 2006-01, 2006-02, 2006-03, ... (115 in all; r11_coverage.csv) |
| Hydro generation (monthly, input) | 183 | 14 | 8 | 169 | 0 | 2005-04, 2005-06, 2005-07, 2005-08, 2005-09, 2005-10, 2005-11, 2005-12, 2006-01, 2006-02, 2006-03, 2006-04, ... (169 in all; r11_coverage.csv) | - |
| Rooftop PV generation (monthly, input) | 183 | 48 | 26 | 77 | 58 | 2007-02, 2007-07, 2007-10, 2007-12, 2008-02, 2008-05, 2008-06, 2008-10, 2008-11, 2008-12, 2009-03, 2009-04, ... (77 in all; r11_coverage.csv) | 2007-05, 2007-06, 2008-07, 2008-08, 2009-01, 2009-07, 2009-08, 2010-07, 2010-08, 2011-04, 2011-06, 2011-08, ... (58 in all; r11_coverage.csv) |
| RE share (monthly) | 183 | 18 | 10 | 75 | 90 | 2005-09, 2006-09, 2009-02, 2009-07, 2009-08, 2009-09, 2009-10, 2009-11, 2009-12, 2010-01, 2010-02, 2010-03, ... (75 in all; r11_coverage.csv) | 2005-06, 2006-03, 2006-04, 2006-05, 2006-11, 2006-12, 2007-01, 2007-02, 2007-04, 2007-05, 2007-06, 2007-07, ... (90 in all; r11_coverage.csv) |
| GHGE (annual, July-June) | 14 | 4 | 29 | 5 | 5 | 2005, 2012, 2016, 2017, 2018 | 2006, 2007, 2008, 2013, 2014 |
| GHGE (annual, July-June, full) | 19 | 4 | 21 | 10 | 5 | 2000, 2001, 2002, 2003, 2004, 2005, 2012, 2016, 2017, 2018 | 2006, 2007, 2008, 2013, 2014 |
| Recursive intensity vs NGA scope 2 plus losses | 16 | 1 | 6 | 14 | 1 | 2000, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2017, ... (14 in all; r11_coverage.csv) | 2015 |
| System intensity vs NGA scope 2 | 16 | 1 | 6 | 14 | 1 | 2000, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2017, ... (14 in all; r11_coverage.csv) | 2015 |
| System intensity vs AEMO CDEII | 9 | 0 | 0 | 9 | 0 | 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 | - |
