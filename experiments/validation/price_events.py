"""Price events, one removed at a time (23 Nov 2026).

Each event run repeats the base case with one recorded event taken out; the same seeds draw the same forced outages,
so the difference base minus event run, seed by seed, is the effect the model gives that event:
  nocarbon   no carbon price (VIC.yaml carbonPriceFile unset; recorded: 1 July 2012 to 30 June 2014)
  hazelwood  Hazelwood kept open to 31 December 2019 (generator parameter endYear 2019; recorded closure 31 March 2017)
  gas2015    gas fuel price held at the 2015 value, 4.15 $/GJ nominal, from 2016 (recorded DWGM 6.7-9.1 $/GJ 2016-19)
  hazelwood_gas2015  both of the last two together, to measure their interaction (joint effect minus the sum of the
                     two single effects, seed by seed)

Usage: python price_events.py BASE_DIR EVENTS_DIR
  BASE_DIR/run_s<seed>, EVENTS_DIR/<event>/run_s<seed>, both from seed_ensemble.py
  writes EVENTS_DIR/price_events.csv (per event, seed, year) and EVENTS_DIR/price_events.md
"""
import glob
import os
import sys

import pandas as pd

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import validation_statistics as vs  # noqa: E402

EVENTS = {"nocarbon": ("No carbon price", range(2011, 2016)),
          "hazelwood": ("Hazelwood open to 2019", range(2016, 2020)),
          "gas2015": ("Gas price held at 2015", range(2015, 2020)),
          "hazelwood_gas2015": ("Hazelwood open and gas price held at 2015, together", range(2016, 2020))}


def annual(run):
    year, month, *_ = vs.load_inputs(run)
    p = month["Primary Wholesale ($/MWh)"]
    return pd.DataFrame({
        "price": p.groupby(p.index.year).mean(),  # calendar-year mean of monthly prices, as in seed_years.csv
        "ghge_mt": year["System GHG Emissions (tCO2-e)"] / 1e6,
        "coal_twh": year["System Production Coal"] / 1e6,
        "gas_twh": year["System Production Gas"] / 1e6})


def main():
    base_dir, ev_dir = sys.argv[1], sys.argv[2]
    rec = vs.load_price_comparator()
    rec = rec.groupby(rec.index.year).mean()
    rows, lines = [], ["# Price events, one removed at a time", "",
                       "Effect = base case minus the run without the event, seed by seed (same seeds, same outages); "
                       "median (5 % to 95 %) over the seeds. Prices nominal $/MWh, calendar-year mean of monthly prices.",
                       ""]
    for ev, (label, yrs) in EVENTS.items():
        runs = sorted(glob.glob(os.path.join(ev_dir, ev, "run_s*")))
        for run in runs:
            seed = run.rsplit("run_s", 1)[1]
            b, e = annual(os.path.join(base_dir, f"run_s{seed}")), annual(run)
            d = (b - e).add_prefix("effect_")
            rows.append(pd.concat([b.add_prefix("base_"), e.add_prefix("event_removed_"), d], axis=1)
                        .assign(event=ev, seed=int(seed)))
        t = pd.concat([r for r in rows if r.event.iloc[0] == ev])
        g = t.groupby(level=0)

        def q(c):
            return g[c].median(), g[c].quantile(0.05), g[c].quantile(0.95)

        lines += [f"## {label} ({len(runs)} seeds)", "",
                  "| Year | Recorded price ($/MWh) | Base price ($/MWh) | Without the event ($/MWh) "
                  "| Price effect ($/MWh) | Emissions effect (Mt CO2-e) | Coal effect (TWh) | Gas effect (TWh) |",
                  "|---|---|---|---|---|---|---|---|"]
        for y in yrs:
            def f(c):
                m, lo, hi = (s.loc[y] for s in q(c))
                return f"{m:+.1f} ({lo:+.1f} to {hi:+.1f})"
            lines.append(f"| {y} | {rec.get(y, float('nan')):.1f} | {g['base_price'].median().loc[y]:.1f} "
                         f"| {g['event_removed_price'].median().loc[y]:.1f} | {f('effect_price')} "
                         f"| {f('effect_ghge_mt')} | {f('effect_coal_twh')} | {f('effect_gas_twh')} |")
        lines.append("")
    t = pd.concat(rows).rename_axis("year")
    eff = t.reset_index().pivot_table(index=["year", "seed"], columns="event", values="effect_price")
    if {"hazelwood", "gas2015", "hazelwood_gas2015"} <= set(eff.columns):
        inter = (eff.hazelwood_gas2015 - eff.hazelwood - eff.gas2015).groupby(level="year")
        lines += ["## Interaction: Hazelwood closure and gas price rise", "",
                  "Joint price effect minus the sum of the two single effects, seed by seed ($/MWh); negative: the two "
                  "single effects overlap (each counts part of the same rise).", "",
                  "| Year | Interaction ($/MWh) |", "|---|---|"]
        for y in range(2017, 2020):
            g = inter.get_group(y)
            lines.append(f"| {y} | {g.median():+.1f} ({g.quantile(0.05):+.1f} to {g.quantile(0.95):+.1f}) |")
        lines.append("")
    t.to_csv(os.path.join(ev_dir, "price_events.csv"))
    open(os.path.join(ev_dir, "price_events.md"), "w", encoding="utf-8").write("\n".join(lines))
    print("\n".join(lines))


if __name__ == "__main__":
    main()
