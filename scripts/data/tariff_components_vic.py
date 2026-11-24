"""Victorian residential tariff components for simulationSettings/tariff_components_VIC.csv (opt-in tariffComponentsFile).

With tariffComponentsFile set, the simulated tariff is additive (EndUserUnit.computeTariff):
    tariff = (simulated wholesale + network + environmental + retail) x (1 + GST)
instead of simulated wholesale / R_w. This script writes the three non-wholesale components and the GST rate for each
calendar year of bills, in real January-2019 cents per kWh excluding GST (the model's tariff unit; cpi_conversion is 1
in January 2019). The recorded wholesale component and the recorded bill are written alongside for diagnostics only.

Components
    network       = distribution + transmission + metering (OG: standard metering is in distribution, AMI separate;
                    AEMC: ACS metering)
    environmental = feed-in tariff schemes + VEET/VEU + RET (LRET + SRES)
    retail        = OG "retail services"; AEMC "residual" (retail costs and margin; the AEMC computes it as the offer
                    less the other components, so it absorbs any error in them)
    carbon price  = NOT added: the model already puts the carbon price in generator offers (carbonPriceFile), so the
                    simulated wholesale carries it. The recorded line is kept for the record.

Sources (all files in Sandbox/papers/GR4SPArticle_ERSS/data/)
    OG17  Oakley Greenwood (Feb 2017) Causes of residential electricity bill changes in Victoria, 1995 to 2017,
          Table 3, p. 19 (Bill-component-movement-analysis-final-27Feb2017.pdf): 4,000 kWh, no off-peak hot water,
          average market offer, $/yr in 2016 dollars; calendar years (Jan 2017 for 2017). Used for 2001-2002, 2007-2017.
          Its "Subtotal all network related costs" row repeats the wholesale + retail subtotal (a typesetting error);
          subtotals are rebuilt here from the components.
    OG14  Oakley Greenwood (Dec 2014), same title to 2014, Table 2, p. 14 (2014 dollars). Used for 2003-2006, which
          OG17 omits. Same method; the two editions agree within rounding where both report a year.
    AEMC17 2017 Residential Electricity Price Trends data (EPR0056), sheet VIC, Figure E.3: 2016-17. Cross-check of
          2017 only.
    AEMC18 Databook - 2018 Residential Electricity Price Trends, sheet VIC, Figure E.1, minimum market offer basis
          (the basis of the R_w register row for 2018): column "2017/18" = bills of calendar 2018.
    AEMC19 2019 Residential Electricity Price Trends final report, Figure 2.7, p. 10: base year 2019.
    AEMC20 2020 Residential Electricity Price Trends report, Figure 2.10, p. 13: 2019/20 (base) to 2022/23.
          Calendar year Y = mean of financial years (Y-1)/Y and Y/(Y+1), as in the R_w register for 2020.
    All AEMC values are nominal c/kWh excluding GST for the Victorian representative consumer (3,865 kWh).

Years with no source
    1999-2000: 2001 carried back (as the R_w register does); GST 0 in 1999, 0.05 in 2000 (GST from 1 July 2000).
    2007 retail: interpolated between 2006 and 2008 (OG17's residual retail is squeezed to $54/yr by the drought
    year's recorded wholesale); status "constructed".
    After 2023 the model holds the last row (2023 = 2022/23) constant in real terms: the 2019/20 information set.

Real dollars: real 2016$/2014$ and nominal year-Y values are moved to 2019$ with the Melbourne CPI
(simulationSettings/cpi_melbourne.csv); after 2020 the CPI grows at annualCpi (VICfuture.yaml, 0.0233).

Basis (--basis)
    The two families measure different offers. OG is the average undiscounted market offer (lowest offer of each
    retailer); AEMC is a representative (lower) offer. In the 2017 overlap (OG Jan 2017 against AEMC17 2016-17, the same
    bills) OG's non-wholesale part is about 6 c/kWh higher, mostly in retail. The R_w register carries the same break
    (wholesale share 0.24 in 2017, 0.40 in 2018).
    published  each source as published (a step down in 2018 that is a change of basis, not of costs)
    og         AEMC years moved to the OG basis: the 2017 overlap gap in network + environmental + retail is added to
               the AEMC retail (residual) component, one constant in real terms (the residual is AEMC's own balancing
               item, so it is where a basis difference lands)

Usage: python scripts/data/tariff_components_vic.py [--basis og|published] [OUT_CSV]
       (default basis og, default OUT simulationSettings/tariff_components_VIC.csv)
"""
import os
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), os.pardir, os.pardir)
args = sys.argv[1:]
BASIS = "og"
if args[:1] == ["--basis"]:
    BASIS, args = args[1], args[2:]
assert BASIS in ("og", "published"), BASIS
OUT = args[0] if args else os.path.join(ROOT, "simulationSettings", "tariff_components_VIC.csv")
ANNUAL_CPI_FORECAST = 0.0233
KWH_OG = 4000.0

cpi = {}
with open(os.path.join(ROOT, "simulationSettings", "cpi_melbourne.csv")) as f:
    for line in f.read().splitlines()[1:]:
        if line and not line.startswith("#"):
            y, v = line.split(",")[:2]
            cpi[int(y)] = float(v)
for y in range(max(cpi) + 1, 2051):
    cpi[y] = cpi[y - 1] * (1 + ANNUAL_CPI_FORECAST)


def to2019(price_year):
    return cpi[2019] / cpi[price_year]


# ---- Oakley Greenwood, $/yr for 4,000 kWh (real dollars of the edition) -----------------------------------------
OG_FIELDS = ["distribution", "transmission", "ami", "fit", "wholesale", "retail", "veet", "ret", "carbon", "gst", "total"]
OG17 = {  # Table 3, 2016$
    2001: [311, 40, 0, 0, 216, 373, 0, 0, 0, 94, 1034],
    2002: [309, 28, 0, 0, 226, 367, 0, 1, 0, 93, 1024],
    2007: [289, 50, 6, 0, 402, 54, 0, 4, 0, 81, 886],
    2008: [273, 48, 11, 0, 233, 309, 0, 7, 0, 88, 968],
    2009: [272, 45, 18, 0, 246, 338, 30, 7, 0, 96, 1051],
    2010: [271, 60, 78, 1, 213, 416, 12, 10, 0, 106, 1166],
    2011: [280, 47, 89, 3, 156, 449, 37, 33, 0, 109, 1202],
    2012: [299, 60, 100, 15, 215, 349, 54, 48, 71, 121, 1330],
    2013: [323, 70, 121, 24, 257, 382, 38, 49, 120, 138, 1522],
    2014: [354, 40, 127, 28, 223, 501, 36, 31, 64, 140, 1545],
    2015: [364, 87, 142, 19, 154, 399, 35, 32, 0, 123, 1355],
    2016: [369, 57, 85, 23, 215, 421, 34, 34, 0, 124, 1361],
    2017: [362, 61, 65, 16, 336, 364, 36, 57, 0, 130, 1425],
}
OG14 = {  # Table 2, 2014$ (years OG17 omits)
    2003: [310, 30, 0, 0, 151, 399, 0, 2, 0, 89, 981],
    2004: [306, 35, 0, 0, 182, 325, 0, 3, 0, 85, 937],
    2005: [299, 56, 0, 0, 159, 322, 0, 3, 0, 84, 922],
    2006: [277, 47, 1, 0, 212, 261, 0, 2, 0, 80, 881],
}

# ---- AEMC, nominal c/kWh excl. GST ------------------------------------------------------------------------------
AEMC_FIELDS = ["lret", "sres", "fit", "veu", "transmission", "distribution", "metering", "wholesale", "residual", "total"]
AEMC = {
    "AEMC17 2016-17": [0.64, 0.35, 0.50, 0.21, 1.78, 11.11, 0.00, 9.79, 4.22, 28.60],
    "AEMC18 2017/18": [0.95, 0.63, 0.34, 0.19, 1.45, 8.62, 1.67, 12.86, 2.57, 29.29],
    "AEMC19 2019": [0.82, 0.76, 0.38, 0.35, 1.41, 8.86, 1.71, 12.12, 2.95, 29.36],
    "AEMC20 2019/20": [0.77, 0.86, 0.39, 0.26, 1.21, 9.15, 1.66, 13.68, 1.30, 29.28],
    "AEMC20 2020/21": [0.75, 0.96, 0.39, 0.27, 1.37, 9.06, 1.58, 10.12, 2.78, 27.29],
    "AEMC20 2021/22": [0.61, 0.97, 0.40, 0.29, 1.37, 8.78, 1.13, 7.29, 2.84, 23.68],
    "AEMC20 2022/23": [0.43, 0.91, 0.41, 0.27, 1.38, 8.79, 1.16, 8.59, 2.89, 24.83],
}
# calendar year of bills -> (AEMC entries averaged, source label)
AEMC_YEARS = {
    2018: (["AEMC18 2017/18"], "AEMC18 Fig E.1 minimum market offer, 2017/18 (bills of 2018)"),
    2019: (["AEMC19 2019"], "AEMC19 Fig 2.7, base year 2019"),
    2020: (["AEMC20 2019/20", "AEMC20 2020/21"], "AEMC20 Fig 2.10, mean of 2019/20 and 2020/21"),
    2021: (["AEMC20 2020/21", "AEMC20 2021/22"], "AEMC20 Fig 2.10 forecast, mean of 2020/21 and 2021/22"),
    2022: (["AEMC20 2021/22", "AEMC20 2022/23"], "AEMC20 Fig 2.10 forecast, mean of 2021/22 and 2022/23"),
    2023: (["AEMC20 2022/23"], "AEMC20 Fig 2.10 forecast, 2022/23 (held from here in real terms)"),
}


def og_row(year, vals, price_year, label):
    v = dict(zip(OG_FIELDS, vals))
    parts = sum(v[k] for k in OG_FIELDS if k not in ("gst", "total"))
    assert abs(parts + v["gst"] - v["total"]) <= 4, (year, parts + v["gst"], v["total"])
    k = to2019(price_year) * 100.0 / KWH_OG
    return dict(year=year,
                network=(v["distribution"] + v["transmission"] + v["ami"]) * k,
                environmental=(v["fit"] + v["veet"] + v["ret"]) * k,
                retail=v["retail"] * k,
                gst_rate=0.10,
                wholesale_recorded=v["wholesale"] * k,
                carbon_recorded=v["carbon"] * k,
                total_recorded=v["total"] * k,
                status="source", source=label)


def aemc_row(year, keys, label):
    rows = [dict(zip(AEMC_FIELDS, AEMC[key])) for key in keys]
    for key, r in zip(keys, rows):
        parts = sum(r[f] for f in AEMC_FIELDS if f != "total")
        assert abs(parts - r["total"]) <= 0.03, (key, parts, r["total"])
    m = {f: sum(r[f] for r in rows) / len(rows) for f in AEMC_FIELDS}
    k = to2019(year)
    return dict(year=year,
                network=(m["transmission"] + m["distribution"] + m["metering"]) * k,
                environmental=(m["lret"] + m["sres"] + m["fit"] + m["veu"]) * k,
                retail=m["residual"] * k,
                gst_rate=0.10,
                wholesale_recorded=m["wholesale"] * k,
                carbon_recorded=0.0,
                total_recorded=m["total"] * 1.1 * k,
                status="source" if year <= 2020 else "forecast", source=label)


rows = {}
for y, vals in OG17.items():
    rows[y] = og_row(y, vals, 2016, "OG17 Table 3 (2016$)" + (" Jan 2017" if y == 2017 else ""))
for y, vals in OG14.items():
    rows[y] = og_row(y, vals, 2014, "OG14 Table 2 (2014$)")
for y, (keys, label) in AEMC_YEARS.items():
    rows[y] = aemc_row(y, keys, label)
# 2007 retail: OG17 records $54/yr (other years $300-420) because the drought year's recorded wholesale ($402) squeezed
# the residual retail line. Paired with a simulated wholesale it would drop the tariff by ~7 c/kWh, so the retail line
# is interpolated between 2006 and 2008 (Angela, 12 Nov 2026); the recorded wholesale and bill columns stay as published.
r07 = rows[2007]
r07["retail_recorded"] = r07["retail"]
r07["retail"] = (rows[2006]["retail"] + rows[2008]["retail"]) / 2.0
r07["status"] = "constructed"
r07["source"] += f"; retail interpolated 2006-2008 (OG17 residual {r07['retail_recorded']:.3f}, squeezed by the 2007 wholesale)"
for y, gst in ((1999, 0.0), (2000, 0.05)):
    r = dict(rows[2001])
    r.update(year=y, gst_rate=gst, status="constructed",
             source="OG17 2001 carried back (no source for 1999-2000); GST from 1 July 2000")
    rows[y] = r

# ---- 2017 cross-check: OG17 (Jan 2017) against AEMC17 2016-17 ------------------------------------------------------
og = rows[2017]
a17 = dict(zip(AEMC_FIELDS, AEMC["AEMC17 2016-17"]))
OFFSET = (og["network"] + og["environmental"] + og["retail"]) - (
    a17["transmission"] + a17["distribution"] + a17["metering"] + a17["lret"] + a17["sres"] + a17["fit"] + a17["veu"]
    + a17["residual"]) * to2019(2017)
print(f"Basis gap in 2017 (OG minus AEMC, network + environmental + retail): {OFFSET:.2f} c/kWh 2019$ excl. GST")
if BASIS == "og":
    for y in AEMC_YEARS:
        rows[y]["retail"] += OFFSET
        rows[y]["source"] += f"; retail + {OFFSET:.2f} (2017 basis gap, OG basis)"
a = dict(zip(AEMC_FIELDS, AEMC["AEMC17 2016-17"]))
k17 = to2019(2017)
print("2017 cross-check, c/kWh 2019$ excl. GST    OG17 Jan 2017   AEMC17 2016-17")
for name, ogv, av in (("network", og["network"], (a["transmission"] + a["distribution"]) * k17),
                      ("environmental", og["environmental"], (a["lret"] + a["sres"] + a["fit"] + a["veu"]) * k17),
                      ("wholesale", og["wholesale_recorded"], a["wholesale"] * k17),
                      ("retail / residual", og["retail"], a["residual"] * k17),
                      ("bill incl. GST", og["total_recorded"], a["total"] * 1.1 * k17)):
    print(f"  {name:<40s}{ogv:10.2f}{av:15.2f}")
print()

cols = ["year", "network", "environmental", "retail", "gst_rate", "wholesale_recorded", "carbon_recorded",
        "total_recorded", "status", "source"]
with open(OUT, "w", newline="") as f:
    f.write(",".join(cols) + "\n")
    f.write("# Victorian residential tariff components, real January-2019 c/kWh excl. GST (total_recorded incl. GST),\n")
    f.write("# calendar years of bills; built by scripts/data/tariff_components_vic.py, which lists the sources.\n")
    f.write("# The model uses network, environmental, retail and gst_rate (tariffComponentsFile); the recorded wholesale,\n")
    f.write("# carbon (already in generator offers) and bill columns are for diagnostics. Years after the last row hold it.\n")
    for y in sorted(rows):
        r = rows[y]
        f.write(",".join([str(y)] + [f"{r[c]:.3f}" for c in cols[1:4]] + [f"{r['gst_rate']:.2f}"] +
                         [f"{r[c]:.3f}" for c in cols[5:8]] + [r["status"], '"' + r["source"] + '"']) + "\n")
        print(f"{y}  net {r['network']:6.2f}  env {r['environmental']:5.2f}  ret {r['retail']:5.2f}  "
              f"W_rec {r['wholesale_recorded']:6.2f}  bill {r['total_recorded']:6.2f}  {r['status']}")
