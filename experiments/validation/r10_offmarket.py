"""R10 (28 Nov 2026): off-market units and demand, follow-ups to the R6 answer of 28 Nov.

Reads, all committed:
  - the offer-rule log of seed 1 (offer_rules_2026-11-28/log_s1/offer_rules_log.csv.gz, gr4sp 629d33e): per month and
    generator, market, output and LCOE-rule price;
  - R10_DIR/generationassets_VIC.csv: the Victorian rows of gr4spdb.generationassets (DB-2021-8-21), exported 28 Nov;
  - R10_DIR/generation_consumption_historic_1998_2020.csv: the monthly record the demand is calibrated to
    (total_consumption_gwh, OpenElectricity by fuel; LoadData.selectConsumption);
  - R10_DIR/pricesetter_s1/price_setter_1999.csv: seed 1 with -Dgr4sp.priceSetterLog for 1999 (point 4a);
  - simulationSettings/VIC.yaml (demandRecordFuels, basePriceMWh, capacity factors).
Writes R10_DIR/r10_offmarket.md and the tables behind it.

Usage: python experiments/validation/r10_offmarket.py [R10_DIR=experiments/validation/r10_offmarket_2026-11-28]
"""
import os, sys
import pandas as pd
import yaml

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
R10 = os.path.abspath(sys.argv[1]) if len(sys.argv) > 1 else os.path.join(REPO, "experiments", "validation", "r10_offmarket_2026-11-28")
LOG = os.path.join(REPO, "experiments", "validation", "offer_rules_2026-11-28", "log_s1", "offer_rules_log.csv.gz")


def md(t, fmt="{:.1f}"):
    head = [str(t.index.name or "")] + [str(c) for c in t.columns]
    rows = ["| " + " | ".join(head) + " |", "|" + "---|" * len(head)]
    for i, r in t.iterrows():
        rows.append("| " + " | ".join([str(i)] + [(fmt.format(v) if isinstance(v, float) else str(v)) for v in r]) + " |")
    return "\n".join(rows)


cfg = yaml.safe_load(open(os.path.join(REPO, "simulationSettings", "VIC.yaml"), encoding="utf-8"))
recorded = [f.strip() for f in cfg["demandRecordFuels"].split(";")]
x = pd.read_csv(LOG, encoding="cp1252")
x["year"] = x.month.str[:4].astype(int)
x = x[(x.year >= 1999) & (x.year <= 2019)]
assets = pd.read_csv(os.path.join(R10, "generationassets_VIC.csv"))
rec = pd.read_csv(os.path.join(R10, "generation_consumption_historic_1998_2020.csv"))
rec["year"] = rec.date.str[:4].astype(int)
recy = rec[(rec.year >= 1999) & (rec.year <= 2019)].groupby("year").sum(numeric_only=True)
L = ["# R10: off-market units and demand (base case, seed 1, v0.3.3 code and settings)\n",
     f"Written by `experiments/validation/r10_offmarket.py {os.path.relpath(R10, REPO)}`. Sources in the script's docstring.\n"]

# ---------- Point 1: demand basis of non-solar off-market output
off = x[(x.market == "off") & (x.fuel.str.lower() != "solar")].copy()
off["in record"] = off.apply(lambda r: r.fuel in recorded or f"{r.fuel} {r.tech}" in recorded, axis=1)
by = off.groupby(["year", "in record"])["generated (MWh)"].sum().unstack(fill_value=0.0) / 1e3
by.columns = ["added back by N28 (GWh)" if not c else "fuel in the record, not added back (GWh)" for c in by.columns]
fuel_rec = off[off["in record"]].groupby(["year", "fuel"])["generated (MWh)"].sum().unstack(fill_value=0.0) / 1e3
mkt = x[x.market != "off"]
wind_model = x[x.fuel == "Wind"].groupby("year")["generated (MWh)"].sum() / 1e3
wind_mkt = mkt[mkt.fuel == "Wind"].groupby("year")["generated (MWh)"].sum().reindex(by.index, fill_value=0.0) / 1e3
t1 = pd.DataFrame({"off-market non-solar (GWh)": by.sum(axis=1)}).join(by)
for f in fuel_rec.columns:
    t1[f"  of which {f} (GWh)"] = fuel_rec[f]
t1["record wind (GWh)"] = recy["wind_gwh"]
t1["model wind, market (GWh)"] = wind_mkt
t1["model wind, off-market (GWh)"] = fuel_rec.get("Wind", 0.0)
t1["model wind, total (GWh)"] = t1["model wind, market (GWh)"] + t1["model wind, off-market (GWh)"]
t1["model wind - record wind (GWh)"] = t1["model wind, total (GWh)"] - t1["record wind (GWh)"]
t1["record total (GWh)"] = recy["total_consumption_gwh"]
t1["model wind - record wind, share of record total (%)"] = 100 * t1["model wind - record wind (GWh)"] / t1["record total (GWh)"]
t1 = t1.fillna(0.0)
t1.index.name = "year"
t1.to_csv(os.path.join(R10, "point1_demand_basis.csv"))
L.append("## 1. Demand basis of the non-solar off-market output\n")
L.append(f"The half-hourly demand is AEMO's, scaled each month to `total_consumption_gwh` (OpenElectricity generation by "
         f"fuel, solar and imports included; `LoadData` calibration, l.1088-1135) plus, since N28, the output of off-market "
         f"units whose fuel is not in `demandRecordFuels` ({'; '.join(recorded)}). `Arena` then subtracts all off-market output. "
         "So off-market output is added back when its fuel is outside the record, and assumed to be in the record otherwise. "
         "The question is whether the record holds the off-market output of the recorded fuels (wind and water here).\n")
L.append(md(t1[["off-market non-solar (GWh)", "added back by N28 (GWh)", "fuel in the record, not added back (GWh)"]
               + [c for c in t1.columns if c.startswith("  of which")]]) + "\n")
L.append("Wind: the record against the model's market and off-market rows. The record (OpenElectricity) has no wind "
         "before 2008 (17 GWh in 2008), while the model has wind rows from 2001. In those years the monthly target lacks all wind, "
         "and the model's whole wind output (market rows dispatched, off-market rows subtracted) takes the place of "
         "recorded coal and gas.\n")
L.append(md(t1[["record wind (GWh)", "model wind, market (GWh)", "model wind, off-market (GWh)", "model wind, total (GWh)",
                "model wind - record wind (GWh)", "model wind - record wind, share of record total (%)"]], "{:.2f}") + "\n")
pre = t1.loc[2001:2008]
L.append(f"2001-2008: the model's wind with no counterpart in the record is {pre['model wind - record wind (GWh)'].sum():.0f} GWh "
         f"({pre['model wind - record wind (GWh)'].min():.0f}-{pre['model wind - record wind (GWh)'].max():.0f} GWh a year, "
         f"{pre['model wind - record wind, share of record total (%)'].min():.2f}-{pre['model wind - record wind, share of record total (%)'].max():.2f} % "
         "of the record total). From 2009 the record holds wind, and whether it holds the off-market rows cannot be told from "
         "the monthly totals by fuel; the difference then mixes that with the model's wind error.\n")

# ---------- Point 2: S and SS rows off-market, by station
sss = x[(x.market == "off") & (x.dispatch.isin(["S", "SS"]))]
L.append("## 2. Scheduled and semi-scheduled rows off-market, 1999-2019\n")
L.append(f"`Settings.isMarketPaticipant` compares each register row with `minCapMarketGen` ({cfg['arena']['scheduled']['minCapMarketGen']} MW "
         "for scheduled and semi-scheduled). Station total: every Victorian row of the same DUID (or site name where "
         "the DUID is empty) in `generationassets`, all statuses.\n")
rows = []
for n, g in sss.groupby("name"):
    a = assets[assets.site_name.str.strip() == n.strip()]
    duid = a.duid.dropna().iloc[0] if a.duid.notna().any() else ""
    same = assets[assets.duid == duid] if duid and duid != "N/A" else assets[assets.site_name.str.strip() == n.strip()]
    rows.append({"row": n, "DUID": duid, "fuel": g.fuel.iloc[0], "dispatch": g.dispatch.iloc[0],
                 "row capacity (MW)": g["maxCapacity (MW)"].max(), "first month": g.month.min(),
                 "energy 1999-2019 (GWh)": g["generated (MWh)"].sum() / 1e3,
                 "rows of the station (count)": len(same),
                 "station capacity, all rows (MW)": same.nameplate_capacity_mw.sum(),
                 "station rows in the market, last month (MW)": mkt[mkt.name.isin(same.site_name.str.strip())
                                                                    & (mkt.month == g.month.max())]["maxCapacity (MW)"].sum()})
t2 = pd.DataFrame(rows).set_index("row")
t2.index.name = "off-market S/SS row"
t2.to_csv(os.path.join(R10, "point2_ss_offmarket.csv"))
L.append(md(t2) + "\n")
later = assets[assets.dispatch_type.isin(["S", "SS"]) & (assets.nameplate_capacity_mw > 0) & (assets.nameplate_capacity_mw < 30)
               & (pd.to_datetime(assets.full_commercial_use_date, errors="coerce") >= "2020-01-01")]
L.append("Rows of this kind that start in 2020 or later (forecast only), from `generationassets`:\n")
t2b = later[["site_name", "duid", "fuel_type", "nameplate_capacity_mw", "dispatch_type", "unit_status", "full_commercial_use_date"]].set_index("site_name")
t2b.index.name = "row"
L.append(md(t2b) + "\n")

# ---------- Point 3: Hume and Clover
L.append("## 3. Hume Dam VIC and Clover\n")
h = x[x.name.str.contains("Hume Dam VIC|Clover", regex=True)]
t3 = h.groupby(["name", "maxCapacity (MW)", "dispatch", "market"])["generated (MWh)"].sum().div(1e3).rename("energy 1999-2019 (GWh)").to_frame()
t3["energy 2010 (GWh)"] = h[h.year == 2010].groupby(["name", "maxCapacity (MW)", "dispatch", "market"])["generated (MWh)"].sum().div(1e3)
t3.index = [f"{a} ({b} MW, {c}, {d})" for a, b, c, d in t3.index]
t3.index.name = "row"
w = cfg["generators"]["Water"]
L.append(md(t3, "{:.3f}") + "\n")
L.append(f"An off-market unit runs at nameplate x its type's capacity factor in every half-hour, with no outages "
         f"(`Generator.computeAvailableCapacity`, l.407-413; `Arena` off-market loop). Both stations are type Water: "
         f"maxCapacityFactor {w['maxCapacityFactor']}, summer {w['maxCapacityFactorSummer']}, and neither has a row in "
         f"`generator_parameters_VIC.csv`. Hume is one 29 MW row; Clover is two rows of 16 and 13 MW, 29 MW together. "
         f"Same capacity and same factor give the same energy: 29 MW x 0.20 x 8,760 h = {29 * 0.2 * 8760 / 1e3:.3f} GWh "
         "a year (8,784 h in leap years).\n")

# ---------- Point 4a: January 1999, from the price-setter log
ps = pd.read_csv(os.path.join(R10, "pricesetter_s1", "price_setter_1999.csv"), encoding="cp1252")
jan = ps[(ps.time.str[:7] == "1999-01") & (ps.market.str.lower() == "primary")].copy()
lcoe_units = ["Dartmouth", "Eildon", "McKay Creek Power Station", "West Kiewa"]
jan["set by the four units"] = jan.name.isin(lcoe_units)
n, k = len(jan), int(jan["set by the four units"].sum())
L.append("## 4a. January 1999: hydro on the LCOE rule\n")
L.append(f"Seed 1, price-setter log of 1999 (`pricesetter_s1/`, same run as the base case: its three CSVs are compared "
         f"below). January 1999 has {n} half-hours; the four hydro units on the LCOE rule set the primary price in {k}.\n")
t4 = pd.DataFrame({"half-hours": [n, n - k, k],
                   "mean price ($/MWh)": [jan.price.mean(), jan[~jan["set by the four units"]].price.mean(),
                                          jan[jan["set by the four units"]].price.mean() if k else float("nan")]},
                  index=["all half-hours", "without those half-hours", "only those half-hours"])
t4.index.name = "January 1999, primary market"
L.append(md(t4, "{:.2f}") + "\n")
setters = jan.groupby("fuel").size().rename("half-hours setting the price").to_frame()
setters.index.name = "price-setting fuel, January 1999"
L.append(md(setters, "{:.0f}") + "\n")
ms = pd.read_csv(os.path.join(R10, "pricesetter_s1", "VICSimDataMonthlySummary_seed_1.csv"))
jr = ms[ms["Time (Month)"].str[:7] == "1999-01"]["Primary Wholesale ($/MWh)"].iloc[0]
L.append(f"The model's monthly price for January 1999 is {jr:.4f} $/MWh (sum / (N + 1), R6 finding 2). The record "
         f"(`aemo_VIC1_monthly_vwp_199812_202006.csv`) is not part of this check.\n")

# ---------- Point 4b: BasePrice of market units (for R8)
L.append("## 4b. BasePrice of the market units (for R8)\n")
L.append("BasePrice and the capacity factors come from the type blocks of `VIC.yaml` (`generators`), not from the "
         "database: `Generator` reads `basePriceMWh` from settings (Generator.java:186), `Arena.applyInflation` raises it "
         "by CPI every year. The LCOE-rule price is BasePrice / historic capacity factor, with the factor kept between "
         "minCapacityFactor and maxCapacityFactor (BasePrice / maxCapacityFactor for a unit that has never bid). The "
         "logged price is `Generator.priceMWhLCOE` in the base case, December of each year: under L1 or L2 the units' "
         "dispatch, and so their historic factor, would differ.\n")
rows = []
for f, g in mkt.groupby("fuel"):
    key = {"Brown Coal": "Brown Coal", "Water": "Water", "Wind": "Wind", "Solar": "Solar", "Battery": "Battery"}.get(f)
    techs = sorted(g.tech.unique())
    for tch in techs:
        blk = cfg["generators"].get(key) if key else cfg["generators"].get(f"Gas Pipeline {tch}")
        if blk is None:
            continue
        r = {"fuel / tech": f"{f} / {tch}", "basePriceMWh (VIC.yaml)": blk.get("basePriceMWh"),
             "comment in VIC.yaml": "", "minCF": blk.get("minCapacityFactor"), "maxCF": blk.get("maxCapacityFactor"),
             "base / maxCF ($/MWh, before CPI)": blk.get("basePriceMWh") / blk.get("maxCapacityFactor")}
        for y in (1999, 2009, 2019):
            d = g[(g.tech == tch) & (g.month == f"{y}-12")]["lcoePrice ($/MWh)"]
            r[f"LCOE price {y}, min-max ($/MWh)"] = f"{d.min():.1f}-{d.max():.1f}" if len(d) else "-"
        rows.append(r)
t5 = pd.DataFrame(rows).set_index("fuel / tech").drop(columns="comment in VIC.yaml")
L.append(md(t5, "{:.2f}") + "\n")
open(os.path.join(R10, "r10_offmarket.md"), "w", encoding="utf-8").write("\n".join(L) + "\n")
print("\n".join(L))
