"""Compare two seed ensembles row by row (R10, 28 Nov 2026: v0.3.3 ensemble_2026-11-24 against v0.3.4 ensemble_2026-11-28).

Reads OLD_DIR/seed_summary.csv and NEW_DIR/seed_summary.csv (written by seed_ensemble.py: indicator, metric, seed42,
median, p05, p95, seeds) and writes, for every indicator and metric of either file, the median and the 5-95 % range over
the seeds of each ensemble, the change in the median (new - old) and that change as a share of the old 5-95 % width.
Units are those of seed_summary.md: NSE has none; Bias, RMSE and MAE are in the indicator's unit; NRMSE in % of the
observed mean.

Usage: python experiments/validation/ensemble_compare.py OLD_DIR NEW_DIR OUT_MD
"""
import os
import sys

import pandas as pd

UNITS = {"Wholesale price": "$/MWh", "RE share": "pp", "Tariffs": "c/kWh", "GHGE": "Mt", "generation": "GWh"}


def unit(indicator, metric):
    if metric == "NSE":
        return "-"
    if metric == "NRMSE":
        return "%"
    for k, u in UNITS.items():
        if k in indicator:
            return u
    return "?"


def main(old_dir, new_dir, out_md):
    key = ["indicator", "metric"]
    old = pd.read_csv(os.path.join(old_dir, "seed_summary.csv")).set_index(key)
    new = pd.read_csv(os.path.join(new_dir, "seed_summary.csv")).set_index(key)
    order = list(old.index) + [i for i in new.index if i not in old.index]
    rows = ["| Indicator | Metric | Unit | old: median (5-95 %) | new: median (5-95 %) | change in median (unit) "
            "| change / old 5-95 % width (%) |", "|---|---|---|---|---|---|---|"]
    for ind, met in order:
        o = old.loc[(ind, met)] if (ind, met) in old.index else None
        n = new.loc[(ind, met)] if (ind, met) in new.index else None
        fo = f"{o['median']:.2f} ({o['p05']:.2f} to {o['p95']:.2f})" if o is not None else "-"
        fn = f"{n['median']:.2f} ({n['p05']:.2f} to {n['p95']:.2f})" if n is not None else "-"
        if o is not None and n is not None:
            d = n["median"] - o["median"]
            w = o["p95"] - o["p05"]
            dd, dw = f"{d:+.2f}", (f"{100 * d / w:+.0f}" if w > 1e-9 else "-")
        else:
            dd = dw = "-"
        rows.append(f"| {ind} | {met} | {unit(ind, met)} | {fo} | {fn} | {dd} | {dw} |")
    head = (f"# Seed ensembles compared: {os.path.basename(os.path.normpath(old_dir))} (old) and "
            f"{os.path.basename(os.path.normpath(new_dir))} (new)\n\n"
            "Written by `experiments/validation/ensemble_compare.py` from the two `seed_summary.csv` files. Median and "
            "5th-95th percentile over the seeds. A width of 0 (statistics identical across seeds) gives '-' in the last "
            "column.\n\n")
    with open(out_md, "w", encoding="utf-8", newline="\n") as f:
        f.write(head + "\n".join(rows) + "\n")
    print(f"wrote {out_md} ({len(order)} rows)")


if __name__ == "__main__":
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    main(*sys.argv[1:])
