# Seed ensembles compared: ensemble_2026-11-24 (old) and ensemble_2026-11-28 (new)

Written by `experiments/validation/ensemble_compare.py` from the two `seed_summary.csv` files. Median and 5th-95th percentile over the seeds. A width of 0 (statistics identical across seeds) gives '-' in the last column.

| Indicator | Metric | Unit | old: median (5-95 %) | new: median (5-95 %) | change in median (unit) | change / old 5-95 % width (%) |
|---|---|---|---|---|---|---|
| Wholesale price (monthly) | NSE | - | -0.29 (-0.96 to 0.05) | -0.27 (-0.94 to 0.07) | +0.02 | +2 |
| Wholesale price (monthly) | Bias | $/MWh | -12.46 (-14.04 to -10.51) | -12.32 (-14.00 to -10.42) | +0.14 | +4 |
| Wholesale price (monthly) | RMSE | $/MWh | 33.48 (28.71 to 41.27) | 33.17 (28.47 to 41.01) | -0.31 | -2 |
| Wholesale price (monthly) | MAE | $/MWh | 20.53 (19.26 to 22.49) | 20.44 (19.09 to 22.49) | -0.09 | -3 |
| Wholesale price (monthly) | NRMSE | % | 72.72 (62.36 to 89.63) | 72.05 (61.85 to 89.08) | -0.66 | -2 |
| Wholesale price (annual) | NSE | - | 0.39 (0.26 to 0.54) | 0.40 (0.26 to 0.54) | +0.01 | +4 |
| Wholesale price (annual) | Bias | $/MWh | -12.66 (-14.38 to -10.47) | -12.60 (-14.41 to -10.37) | +0.06 | +2 |
| Wholesale price (annual) | RMSE | $/MWh | 17.91 (15.47 to 19.62) | 17.74 (15.59 to 19.71) | -0.17 | -4 |
| Wholesale price (annual) | MAE | $/MWh | 15.02 (12.93 to 16.48) | 14.82 (12.89 to 16.36) | -0.20 | -6 |
| Wholesale price (annual) | NRMSE | % | 38.64 (33.39 to 42.34) | 38.28 (33.63 to 42.53) | -0.36 | -4 |
| RE share (annual) | NSE | - | 0.97 (0.97 to 0.97) | 0.97 (0.97 to 0.97) | +0.00 | +2 |
| RE share (annual) | Bias | pp | 0.10 (0.08 to 0.12) | 0.10 (0.08 to 0.12) | +0.00 | +3 |
| RE share (annual) | RMSE | pp | 1.09 (1.04 to 1.11) | 1.09 (1.04 to 1.11) | -0.00 | -2 |
| RE share (annual) | MAE | pp | 0.62 (0.59 to 0.64) | 0.62 (0.59 to 0.63) | -0.00 | -10 |
| RE share (annual) | NRMSE | % | 10.11 (9.68 to 10.27) | 10.10 (9.67 to 10.26) | -0.01 | -2 |
| Tariffs vs Oakley Greenwood | NSE | - | 0.51 (0.48 to 0.56) | 0.51 (0.48 to 0.56) | +0.00 | +1 |
| Tariffs vs Oakley Greenwood | Bias | c/kWh | -3.35 (-3.51 to -3.00) | -3.32 (-3.49 to -2.96) | +0.03 | +6 |
| Tariffs vs Oakley Greenwood | RMSE | c/kWh | 3.85 (3.67 to 3.97) | 3.85 (3.66 to 3.96) | -0.00 | -1 |
| Tariffs vs Oakley Greenwood | MAE | c/kWh | 3.37 (3.11 to 3.52) | 3.33 (3.09 to 3.50) | -0.03 | -8 |
| Tariffs vs Oakley Greenwood | NRMSE | % | 11.94 (11.39 to 12.32) | 11.93 (11.37 to 12.30) | -0.01 | -1 |
| Tariffs vs St Vincent de Paul | NSE | - | -52.68 (-56.63 to -50.20) | -52.60 (-56.48 to -50.25) | +0.08 | +1 |
| Tariffs vs St Vincent de Paul | Bias | c/kWh | 11.18 (10.87 to 11.62) | 11.18 (10.87 to 11.60) | +0.00 | +0 |
| Tariffs vs St Vincent de Paul | RMSE | c/kWh | 11.49 (11.23 to 11.91) | 11.49 (11.23 to 11.89) | -0.01 | -1 |
| Tariffs vs St Vincent de Paul | MAE | c/kWh | 11.18 (10.87 to 11.62) | 11.18 (10.87 to 11.60) | +0.00 | +0 |
| Tariffs vs St Vincent de Paul | NRMSE | % | 54.05 (52.79 to 56.00) | 54.01 (52.81 to 55.93) | -0.04 | -1 |
| GHGE (annual) | NSE | - | 0.70 (0.68 to 0.71) | 0.69 (0.67 to 0.70) | -0.01 | -29 |
| GHGE (annual) | Bias | Mt | -0.64 (-0.71 to -0.58) | -0.55 (-0.63 to -0.49) | +0.09 | +67 |
| GHGE (annual) | RMSE | Mt | 2.85 (2.79 to 2.93) | 2.89 (2.83 to 2.97) | +0.04 | +29 |
| GHGE (annual) | MAE | Mt | 2.00 (1.94 to 2.06) | 2.04 (1.98 to 2.10) | +0.04 | +33 |
| GHGE (annual) | NRMSE | % | 4.66 (4.57 to 4.79) | 4.73 (4.63 to 4.86) | +0.07 | +29 |
| GHGE (annual, full) | NSE | - | 0.37 (0.35 to 0.38) | 0.37 (0.35 to 0.39) | +0.00 | +7 |
| GHGE (annual, full) | Bias | Mt | -1.73 (-1.79 to -1.68) | -1.63 (-1.69 to -1.58) | +0.10 | +97 |
| GHGE (annual, full) | RMSE | Mt | 3.50 (3.46 to 3.55) | 3.49 (3.45 to 3.55) | -0.01 | -7 |
| GHGE (annual, full) | MAE | Mt | 2.68 (2.63 to 2.73) | 2.67 (2.63 to 2.72) | -0.01 | -9 |
| GHGE (annual, full) | NRMSE | % | 5.69 (5.63 to 5.78) | 5.68 (5.62 to 5.77) | -0.01 | -7 |
| Coal generation (monthly) | NSE | - | 0.89 (0.88 to 0.90) | 0.89 (0.87 to 0.89) | -0.00 | -11 |
| Coal generation (monthly) | Bias | GWh | 57.02 (46.62 to 66.17) | 61.00 (50.72 to 70.28) | +3.99 | +20 |
| Coal generation (monthly) | RMSE | GWh | 188.04 (180.20 to 197.51) | 189.89 (181.67 to 199.63) | +1.85 | +11 |
| Coal generation (monthly) | MAE | GWh | 140.81 (134.79 to 148.67) | 142.44 (136.13 to 150.51) | +1.64 | +12 |
| Coal generation (monthly) | NRMSE | % | 4.89 (4.69 to 5.14) | 4.94 (4.73 to 5.20) | +0.05 | +11 |
| Gas generation (monthly) | NSE | - | -1.31 (-1.66 to -1.05) | -1.35 (-1.71 to -1.07) | -0.04 | -6 |
| Gas generation (monthly) | Bias | GWh | 7.29 (-1.22 to 17.57) | 9.04 (0.38 to 19.56) | +1.75 | +9 |
| Gas generation (monthly) | RMSE | GWh | 154.78 (145.83 to 165.98) | 155.96 (146.34 to 167.75) | +1.17 | +6 |
| Gas generation (monthly) | MAE | GWh | 119.43 (113.28 to 127.95) | 120.72 (114.47 to 129.11) | +1.29 | +9 |
| Gas generation (monthly) | NRMSE | % | 105.10 (99.02 to 112.70) | 105.89 (99.37 to 113.90) | +0.80 | +6 |
| Wind generation (monthly) | NSE | - | 0.84 (0.84 to 0.84) | 0.84 (0.84 to 0.84) | -0.00 | -0 |
| Wind generation (monthly) | Bias | GWh | 8.68 (8.68 to 8.70) | 8.68 (8.68 to 8.70) | +0.00 | +0 |
| Wind generation (monthly) | RMSE | GWh | 61.31 (61.31 to 61.33) | 61.31 (61.31 to 61.33) | +0.00 | +0 |
| Wind generation (monthly) | MAE | GWh | 43.18 (43.18 to 43.19) | 43.18 (43.18 to 43.19) | +0.00 | +0 |
| Wind generation (monthly) | NRMSE | % | 35.05 (35.05 to 35.06) | 35.05 (35.05 to 35.06) | +0.00 | +0 |
| Hydro generation (monthly, input) | NSE | - | 0.97 (0.96 to 0.98) | 0.98 (0.97 to 0.99) | +0.00 | +13 |
| Hydro generation (monthly, input) | Bias | GWh | -3.70 (-4.60 to -2.87) | -3.37 (-4.33 to -2.56) | +0.33 | +19 |
| Hydro generation (monthly, input) | RMSE | GWh | 14.42 (11.81 to 17.43) | 13.65 (10.80 to 16.93) | -0.77 | -14 |
| Hydro generation (monthly, input) | MAE | GWh | 3.70 (2.87 to 4.60) | 3.37 (2.56 to 4.33) | -0.33 | -19 |
| Hydro generation (monthly, input) | NRMSE | % | 6.74 (5.52 to 8.15) | 6.38 (5.05 to 7.91) | -0.36 | -14 |
| Rooftop PV generation (monthly, input) | NSE | - | 0.98 (0.98 to 0.98) | 0.98 (0.98 to 0.98) | +0.00 | +0 |
| Rooftop PV generation (monthly, input) | Bias | GWh | 0.96 (0.84 to 1.06) | 0.96 (0.84 to 1.06) | +0.00 | +0 |
| Rooftop PV generation (monthly, input) | RMSE | GWh | 9.55 (9.39 to 9.77) | 9.55 (9.39 to 9.77) | +0.00 | +0 |
| Rooftop PV generation (monthly, input) | MAE | GWh | 4.50 (4.39 to 4.59) | 4.50 (4.39 to 4.59) | +0.00 | +0 |
| Rooftop PV generation (monthly, input) | NRMSE | % | 16.91 (16.62 to 17.30) | 16.91 (16.62 to 17.30) | +0.00 | +0 |
| RE share (monthly) | NSE | - | 0.94 (0.94 to 0.94) | 0.94 (0.94 to 0.94) | +0.00 | +7 |
| RE share (monthly) | Bias | pp | -0.02 (-0.04 to -0.00) | -0.02 (-0.04 to -0.00) | +0.00 | +2 |
| RE share (monthly) | RMSE | pp | 1.55 (1.53 to 1.56) | 1.54 (1.53 to 1.56) | -0.00 | -7 |
| RE share (monthly) | MAE | pp | 1.04 (1.02 to 1.06) | 1.04 (1.02 to 1.05) | -0.01 | -18 |
| RE share (monthly) | NRMSE | % | 14.77 (14.63 to 14.94) | 14.75 (14.61 to 14.93) | -0.02 | -7 |
| GHGE (annual, July-June) | NSE | - | 0.93 (0.92 to 0.94) | 0.93 (0.92 to 0.93) | -0.01 | -40 |
| GHGE (annual, July-June) | Bias | Mt | -0.03 (-0.09 to 0.03) | 0.07 (0.00 to 0.13) | +0.10 | +78 |
| GHGE (annual, July-June) | RMSE | Mt | 1.36 (1.30 to 1.44) | 1.41 (1.35 to 1.49) | +0.05 | +40 |
| GHGE (annual, July-June) | MAE | Mt | 1.07 (1.00 to 1.13) | 1.10 (1.04 to 1.17) | +0.04 | +30 |
| GHGE (annual, July-June) | NRMSE | % | 2.22 (2.13 to 2.35) | 2.31 (2.21 to 2.43) | +0.09 | +40 |
| GHGE (annual, July-June, full) | NSE | - | 0.71 (0.69 to 0.71) | 0.71 (0.70 to 0.72) | +0.01 | +28 |
| GHGE (annual, July-June, full) | Bias | Mt | -1.10 (-1.15 to -1.05) | -1.00 (-1.05 to -0.95) | +0.10 | +102 |
| GHGE (annual, July-June, full) | RMSE | Mt | 2.45 (2.43 to 2.50) | 2.43 (2.40 to 2.48) | -0.02 | -29 |
| GHGE (annual, July-June, full) | MAE | Mt | 1.86 (1.82 to 1.92) | 1.86 (1.82 to 1.91) | -0.00 | -5 |
| GHGE (annual, July-June, full) | NRMSE | % | 4.00 (3.95 to 4.07) | 3.96 (3.91 to 4.04) | -0.03 | -29 |
