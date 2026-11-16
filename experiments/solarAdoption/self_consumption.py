"""Self-consumed share of a household rooftop system's output by household type (solar step 3, D4).

Method: each half-hour, the household uses its own PV output up to its load; self-consumed share = sum over the year
of min(load, PV) / sum of PV.
  Load: the Victorian average daily profile by household size (ACIL Allen 2019 for the ESC, Figure 4.2, homes of 1-5+
    persons, mostly 2016 smart-meter data; extracted by extract_esc_profiles.py), the same every day of the year,
    scaled to the year's use per household (residential grid electricity / households, AES Table F3; the 2016 level
    is the profiles' own). A household type takes the profile of its mean size (EVA ahc_equivalence_VIC.csv, census
    2016), interpolated between integer sizes (5 or more = 5).
  PV: the model's rooftop output per kW in each half-hour of the year (Gr4spSim.getRooftopExposure x rooftop
    efficiency, dumped by the DumpRooftop harness), times the year's mean system size of new installations.
  Homes that adopt PV use more than the average home of their size: in Figure 4.8 the PV homes' night-time import
    (their whole load) is 1.20-1.24 times the non-PV homes' at 21:00-23:00 and 1.4-1.7 times in the early morning
    (probably off-peak water heating). Factor 1.2 (default; sensitivity 1.0 to 1.42, the mean night ratio).
Assumptions: one daily load shape all year (no seasonal or day-to-day variation: real loads are spikier, which lowers
  the share, and higher in winter when PV is low); PV homes have the load shape of homes without PV, scaled.
Checks: the households-weighted share against the two Victorian anchors (0.67 for 2.03 kW in FY2011-12, HECS 2012;
  0.37-0.52 in 2017-19, AER RIN, whose lower end depends on the non-household share).
Result (factor 1.2): FY2011-12 0.664; 2017 0.381, 2018 0.343, 2019 0.303. Factor 1.0: 0.594 and 0.26-0.33; factor 1.42:
  0.728 and 0.34-0.43. Main heating fuel moves the share by about 3 points (Figure 4.9 profiles, 4 kW, 2016: gas 0.325,
  all-electric 0.355), less than household size, so it is left out.

Usage: python self_consumption.py EXPOSURE_CSV [--write] [--pv-home-factor F]
"""
import argparse, os, subprocess
import numpy as np
import pandas as pd

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, os.pardir, os.pardir))
SIZES = {"1 person": 1, "2 people": 2, "3 people": 3, "4 people": 4, "5 or more people": 5}
PSQL = r"C:\Program Files\PostgreSQL\18\bin\psql.exe"


def profiles():
    p = pd.read_csv(os.path.join(HERE, "data", "esc2019_profiles.csv"), comment="#", dtype={"figure": str})
    f = p[p.figure == "4.2"].pivot(index="series", columns="period", values="kwh")
    return {SIZES[k]: f.loc[k].to_numpy() for k in SIZES}, p


def load_for_size(prof, n):
    lo = int(np.clip(np.floor(n), 1, 5))
    hi = min(lo + 1, 5)
    w = float(np.clip(n - lo, 0, 1))
    return (1 - w) * prof[lo] + w * prof[hi]


def pv(ex):
    kwh = (ex.poa_kw_m2 * ex.efficiency * 0.5).to_numpy()
    period = (ex.time.dt.hour * 2 + ex.time.dt.minute // 30).to_numpy()
    return kwh, period


def share(load48, kwh_per_kw, period, size_kw):
    out = kwh_per_kw * size_kw
    return np.minimum(load48[period], out).sum() / out.sum()


def use_level():
    """Use per household by calendar year relative to 2016 (AES residential / model households, as step 2)."""
    res = pd.read_csv(os.path.join(ROOT, "simulationSettings", "residential_electricity_VIC.csv"), comment="#")
    r = subprocess.run([PSQL, "-U", "postgres", "-d", "gr4spdb", "-A", "-t", "-c",
                        "select extract(year from date)::int, domesticconsumers from domestic_consumers "
                        "where date >= '2005-01-01' and extract(month from date) = 1"], capture_output=True, text=True)
    hh = pd.Series({int(l.split("|")[0]): float(l.split("|")[1]) for l in r.stdout.split()})
    fy = res.set_index(res.columns[0])[[c for c in res.columns if "resid" in c.lower()][0]]
    cal = pd.Series({y: (fy[y] + fy[y + 1]) / 2 if y + 1 in fy.index else fy[y] for y in range(2006, 2020)})
    per = cal / hh.reindex(cal.index)
    return per / per[2016]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("exposure")
    ap.add_argument("--write", action="store_true")
    ap.add_argument("--pv-home-factor", type=float, default=1.2,
                    help="load of homes that adopt PV relative to the average home of their size (Figure 4.8 night ratio)")
    a = ap.parse_args()
    prof, allp = profiles()
    ex = pd.read_csv(a.exposure, parse_dates=["time"])
    q = pd.read_csv(os.path.join(HERE, os.pardir, "assesmentData", "ahc_equivalence_VIC.csv"))
    q = q[(q.census == 2016) & (q.dwelling != "ALL")]
    size = (q.households * q.n_mean).groupby(q.type).sum() / q.groupby("type").households.sum()
    coh = pd.read_csv(os.path.join(ROOT, "simulationSettings", "household_cohorts_VIC.csv"), comment="#")
    level = use_level() * a.pv_home_factor
    reg = pd.read_csv(os.path.join(ROOT, "simulationSettings", "solar_installation_cer_VIC.csv"))
    new_kw = reg.groupby("year").aggregated_capacity_kw.sum() / reg.groupby("year").number_installations.sum()
    for y, (kw, n) in {2007: (1109, 828), 2008: (2724, 2037), 2009: (10508, 8433), 2010: (59168, 35685)}.items():
        new_kw[y] = kw / n   # APVI (solar_installation_monthly) before the CER file
    new_kw = new_kw.sort_index()
    rows = []
    print("year, new system kW, use level vs 2016, households-weighted share, by type")
    for y in range(2007, 2020):
        kwh, period = pv(ex[ex.time.dt.year == y])
        w = coh[coh.year == y].groupby("type").share.sum()
        lv = level[max(y, 2006)]
        s = {t: share(load_for_size(prof, n) * lv, kwh, period, new_kw[y]) for t, n in size.items()}
        for t in s:
            rows.append((t, f"{y}-01-01", round(s[t], 3)))
        print(y, round(new_kw[y], 2), round(lv, 3), round(sum(s[t] * w[t] for t in s), 3),
              {t.split()[0] + " " + t.split()[-1][:5]: round(v, 2) for t, v in s.items()})
    # anchor 1: the fleet in service in FY2011-12 (mean 2.03 kW), use level 2011-12
    e1 = ex[(ex.time >= "2011-07-01") & (ex.time < "2012-07-01")]
    kwh, period = pv(e1)
    w = coh[coh.year == 2012].groupby("type").share.sum()
    lv = (level[2011] + level[2012]) / 2
    a1 = sum(share(load_for_size(prof, size[t]) * lv, kwh, period, 2.03) * w[t] for t in size.index)
    print("anchor FY2011-12, fleet in service (2.03 kW): %.3f (HECS 2012: 0.67)" % a1)
    # anchor 2: fleet in service 2017-19 (mean kW of the fleet, not of new systems)
    cum = (reg.groupby("year").aggregated_capacity_kw.sum().cumsum() + 72e3 * 0 + 213917 - reg[reg.year == 2011].aggregated_capacity_kw.sum())
    cumn = (reg.groupby("year").number_installations.sum().cumsum() + 107239 - reg[reg.year == 2011].number_installations.sum())
    for y in (2017, 2018, 2019):
        fleet_kw = cum[y] / cumn[y]
        kwh, period = pv(ex[ex.time.dt.year == y])
        w = coh[coh.year == y].groupby("type").share.sum()
        a2 = sum(share(load_for_size(prof, size[t]) * level[y], kwh, period, fleet_kw) * w[t] for t in size.index)
        print("anchor %d, fleet in service (%.2f kW): %.3f (AER RIN: 0.37-0.52)" % (y, fleet_kw, a2))
    g = allp[allp.figure == "4.9"].pivot(index="series", columns="period", values="kwh")
    kwh, period = pv(ex[ex.time.dt.year == 2016])
    for f in g.index:
        print("fuel sensitivity, Figure 4.9 %s (%.1f kWh/day), 2016, 4 kW: %.3f" % (f, g.loc[f].sum(), share(g.loc[f].to_numpy(), kwh, period, 4.0)))
    if a.write:
        out = pd.DataFrame(rows, columns=["type", "date_from", "share"])
        out["note"] = "min(load, PV) per half-hour; ESC 2019 profile of the type's size; model PV; year's mean new system"
        out["status"] = "constructed"
        path = os.path.join(ROOT, "simulationSettings", "rooftop_self_consumption_VIC.csv")
        with open(path, "w", newline="\n") as fh:
            fh.write("# Self-consumed share of a household rooftop system's output (the rest is exported), by household type and\n"
                     "# year of installation. Built by experiments/solarAdoption/self_consumption.py: each half-hour of the year the\n"
                     "# home uses its PV output up to its load. Load: ACIL Allen (2019) for the ESC, Victorian usage profiles by\n"
                     "# household size (Figure 4.2, mostly 2016 smart-meter data), at the type's mean size (census 2016), scaled to\n"
                     "# the year's use per household (AES Table F3) x %.2f for homes that adopt PV (Figure 4.8: their night-time\n"
                     "# import is 1.2-1.7 times that of homes without PV); one daily shape all year. PV: the model's rooftop output\n"
                     "# per kW x the year's mean new system (installation register). Checked against Victorian anchors: HECS 2012\n"
                     "# (0.67 for the 2.03 kW fleet of FY2011-12; computed 0.66) and AER RIN exports (0.37-0.52 in 2017-19,\n"
                     "# depending on the non-household share; computed 0.30-0.38).\n" % a.pv_home_factor)
            out.to_csv(fh, index=False)
        print("written", path)


if __name__ == "__main__":
    main()
