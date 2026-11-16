"""Self-consumed share of a household rooftop system's output by household type (solar step 3, D4 and D4b).

Method: each half-hour, the household uses its own PV output up to its load; self-consumed share = sum over the year
of min(load, PV) / sum of PV (net metering at half-hour resolution).
  Load of a home of size n on day d, half-hour h:
      L(d, h) = ESC_n(h) x m(d, h) x level(year) x pv_home_factor
    ESC_n: the Victorian average daily profile by household size (ACIL Allen 2019 for the ESC, Figure 4.2, homes of
      1-5+ persons, mostly 2016 smart-meter data; extract_esc_profiles.py); a household type takes the profile of its
      mean size (EVA ahc_equivalence_VIC.csv, census 2016), interpolated between integer sizes (5 or more = 5).
    m(d, h): day-to-day shape from weather and calendar (D4b), the relative load of Victorian small customers
      predicted from the day's temperature, the previous day's, day type, night length and the Christmas period
      (load_temperature_response.py, AEMO NSLP 2002-2010, ERA5 Melbourne temperature), divided by its mean over the
      calendar year in each half-hour so that the year's average day stays the ESC profile. --shape flat gives
      m = 1 (one daily shape all year, the D4 method).
    level: use per household by calendar year relative to 2016 (AES residential electricity, Table F3 / census
      households, households_VIC.csv as in the model since N30).
    pv_home_factor: homes that adopt PV use more than the average home of their size: in Figure 4.8 the PV homes'
      night-time import (their whole load) is 1.20-1.24 times the non-PV homes' at 21:00-23:00 and 1.4-1.7 times in
      the early morning. Factor 1.2 (default; sensitivity 1.0 to 1.42).
  PV: the model's rooftop output per kW in each half-hour (Gr4spSim.getRooftopExposure x rooftop efficiency, dumped by
    the DumpRooftop harness), times the year's mean system size of new installations.
  Averaging bias (D4b): a profile averaged over many homes is smoother than any one home's load, so min(load, PV) is
    overstated. Measured on 1,217 Smart Grid Smart City homes (sgsc_averaging_test.py): the share with the sample's
    mean shape of each day (a perfect model of the average home's day) is 1.05-1.28 times the share with the homes'
    own measured loads, depending on x = annual PV / annual load (data/sgsc_averaging_bias_by_x.csv, ratio_day). The
    computed share is divided by that ratio at the home's x (linear between bin centres). --bias none skips it.
Checks: the households-weighted share against the two Victorian anchors (0.67 for 2.03 kW in FY2011-12, HECS 2012;
  0.37-0.52 in 2017-19, AER RIN, whose lower end depends on the non-household share); the seasonal split of the
  modulated load against the AER 2020 benchmarks for Victorian climate zone 6 (Table 17).

Usage: python self_consumption.py EXPOSURE_CSV [--write] [--pv-home-factor F] [--shape nslp|mrim|flat] [--bias day|mdt|none]
"""
import argparse, os
import numpy as np
import pandas as pd
from load_temperature_response import daily_temperature, day_features, design, load_response

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, os.pardir, os.pardir))
SIZES = {"1 person": 1, "2 people": 2, "3 people": 3, "4 people": 4, "5 or more people": 5}
SHORT = {"Couple family with children": "couple+kids", "Couple family without children": "couple", "Group household": "group",
         "Lone person": "lone", "One parent family": "one parent", "Other family": "other"}
SHAPES = {"nslp": "2002-2010 NSLP", "mrim": "2018-2019 MRIM"}
# AER 2020 residential benchmarks, Table 17, Victoria climate zone 6 (Melbourne), kWh per season, by household size
AER_CZ6 = {1: (674, 684, 927, 668), 2: (1112, 1125, 1493, 1110), 3: (1143, 1165, 1606, 1163),
           4: (1324, 1338, 1821, 1322), 5: (1686, 1718, 2245, 1702)}  # summer, autumn, winter, spring


def profiles(pv_mix):
    """ESC Figure 4.2 profiles by size, made gross. They average grid import over a sample in which a share pv_mix of
    homes has PV (Table 3.5: 4,984 kWh all, 5,241 without PV, 4,671 with PV -> 0.45), so their midday is net of
    self-use. Each half-hour is multiplied by NoPV(h) / [(1 - pv_mix) NoPV(h) + pv_mix PV(h)] (Figure 4.8) and the
    day rescaled to its original total: the shape of homes without PV, the size's own level."""
    p = pd.read_csv(os.path.join(HERE, "data", "esc2019_profiles.csv"), comment="#", dtype={"figure": str})
    f = p[p.figure == "4.2"].pivot(index="series", columns="period", values="kwh")
    g = p[p.figure == "4.8"].pivot(index="series", columns="period", values="kwh")
    r = (g.loc["No PV"] / ((1 - pv_mix) * g.loc["No PV"] + pv_mix * g.loc["PV"])).to_numpy()
    out = {}
    for k in SIZES:
        v = f.loc[k].to_numpy()
        out[SIZES[k]] = v * r * v.sum() / (v * r).sum()
    return out, p


def load_for_size(prof, n):
    lo = int(np.clip(np.floor(n), 1, 5))
    hi = min(lo + 1, 5)
    w = float(np.clip(n - lo, 0, 1))
    return (1 - w) * prof[lo] + w * prof[hi]


class Shape:
    """Day-to-day relative load m(d, h), mean 1 over each calendar year in each half-hour."""

    SEASONAL = [1, 2, 3, 4]  # HDD, CDD, CDD1, night_h: the terms scaled by the amplitude k

    def __init__(self, kind, era5, response, k=1.0):
        self.kind = kind
        if kind != "flat":
            self.temp = daily_temperature(era5)
            self.coef, self.th, self.tc = load_response(response, SHAPES[kind])
        self.set_amplitude(k)

    def set_amplitude(self, k):
        self.k = k
        self.cache = {}

    def year(self, y):
        if y not in self.cache:
            idx = pd.date_range(f"{y}-01-01", f"{y}-12-31", freq="D")
            if self.kind == "flat":
                m = pd.DataFrame(1.0, index=idx, columns=range(48))
            else:
                f = day_features(idx, self.temp)
                assert len(f) == len(idx), f"temperature missing for {y}"
                m = pd.DataFrame(index=idx, columns=range(48), dtype=float)
                for dt in ("weekday", "weekend"):
                    k = f.dtype == dt
                    b = self.coef[dt].copy()
                    b[self.SEASONAL] *= self.k
                    m.loc[k] = design(f[k], self.th, self.tc) @ b
                m = m / m.mean()
            self.cache[y] = m
        return self.cache[y]

    def days(self, dates):
        return pd.concat([self.year(y) for y in sorted(set(dates.year))]).reindex(dates).to_numpy()


def pv_matrix(ex, start, end):
    e = ex[(ex.time >= start) & (ex.time < end)]
    k = e.assign(k=e.poa_kw_m2 * e.efficiency * 0.5, date=e.time.dt.normalize(),
                 hh=e.time.dt.hour * 2 + e.time.dt.minute // 30).pivot_table(index="date", columns="hh", values="k")
    k = k.dropna()
    return k.to_numpy(), k.index


class Bias:
    def __init__(self, kind):
        self.kind = kind
        if kind != "none":
            t = pd.read_csv(os.path.join(HERE, "data", "sgsc_averaging_bias_by_x.csv"))
            edges = t.xbin.str.strip("(]").str.split(",", expand=True).astype(float)
            self.x = edges.mean(axis=1).to_numpy()
            self.r = t[f"ratio_{kind}"].to_numpy()

    def ratio(self, x):
        return 1.0 if self.kind == "none" else float(np.interp(x, self.x, self.r))


def share(load, pvk, size_kw, bias):
    """load, pvk: days x 48 (kWh, kWh per kW). Returns (corrected share, raw share, x)."""
    out = pvk * size_kw
    raw = np.minimum(load, out).sum() / out.sum()
    x = out.sum() / load.sum()
    return raw / bias.ratio(x), raw, x


def use_level():
    """Use per household by calendar year relative to 2016 (AES residential / census households, as the model)."""
    res = pd.read_csv(os.path.join(ROOT, "simulationSettings", "residential_electricity_VIC.csv"), comment="#")
    hh = pd.read_csv(os.path.join(ROOT, "simulationSettings", "households_VIC.csv"), comment="#").set_index("year").households
    fy = res.set_index("fy").residential_twh
    cal = pd.Series({y: (fy[y] + fy[y + 1]) / 2 if y + 1 in fy.index else fy[y] for y in range(2007, 2020)})
    per = cal * 1e9 / hh.reindex(cal.index)
    return per / per[2016], per


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("exposure")
    ap.add_argument("--write", action="store_true")
    ap.add_argument("--pv-home-factor", type=float, default=1.2,
                    help="load of homes that adopt PV relative to the average home of their size (Figure 4.8 night ratio)")
    ap.add_argument("--shape", choices=["nslp", "mrim", "flat"], default="nslp")
    ap.add_argument("--bias", choices=["day", "mdt", "none"], default="day")
    ap.add_argument("--amplitude", default="1", help="seasonal amplitude k of the weather terms (1 = as fitted), or auto (match AER Table 17)")
    ap.add_argument("--esc-pv-mix", type=float, default=0.45,
                    help="share of PV homes in the ESC sample (Table 3.5), removed from the size profiles; 0 keeps them as published")
    ap.add_argument("--era5", default=os.path.join(HERE, "data", "era5_t2m_melbourne_hourly.csv"))
    ap.add_argument("--response", default=os.path.join(HERE, "data", "load_temperature_response.csv"))
    a = ap.parse_args()
    prof, allp = profiles(a.esc_pv_mix)
    shape = Shape(a.shape, a.era5, a.response)
    bias = Bias(a.bias)
    ex = pd.read_csv(a.exposure, parse_dates=["time"])
    q = pd.read_csv(os.path.join(HERE, os.pardir, "assesmentData", "ahc_equivalence_VIC.csv"))
    q = q[(q.census == 2016) & (q.dwelling != "ALL")]
    size = (q.households * q.n_mean).groupby(q.type).sum() / q.groupby("type").households.sum()
    coh = pd.read_csv(os.path.join(ROOT, "simulationSettings", "household_cohorts_VIC.csv"), comment="#")
    rel, per_hh = use_level()
    level = rel * a.pv_home_factor
    reg = pd.read_csv(os.path.join(ROOT, "simulationSettings", "solar_installation_cer_VIC.csv"))
    new_kw = reg.groupby("year").aggregated_capacity_kw.sum() / reg.groupby("year").number_installations.sum()
    for y, (kw, n) in {2007: (1109, 828), 2008: (2724, 2037), 2009: (10508, 8433), 2010: (59168, 35685)}.items():
        new_kw[y] = kw / n   # APVI (solar_installation_monthly) before the CER file
    new_kw = new_kw.sort_index()
    print(f"load shape {a.shape}, averaging-bias correction {a.bias}, PV-home factor {a.pv_home_factor}, "
          f"ESC PV mix removed {a.esc_pv_mix}")

    # seasonal split of the modulated load (2014-2019 weather) against AER 2020 Table 17 (climate zone 6). The NSLP
    # mixes homes and small businesses, whose load swings less with the seasons; with --amplitude auto the weather and
    # night-length terms are scaled by k so that the mean winter/summer ratio of daily use over the five sizes matches
    seas = {12: 0, 1: 0, 2: 0, 3: 1, 4: 1, 5: 1, 6: 2, 7: 2, 8: 2, 9: 3, 10: 3, 11: 3}

    def seasons(n):
        tot = np.zeros(4)
        days = np.zeros(4)
        for y in range(2014, 2020):
            m = shape.year(y)
            d = pd.Series((m.to_numpy() * prof[n]).sum(axis=1), index=m.index).groupby(m.index.month.map(seas))
            tot += d.sum().to_numpy()
            days += d.size().to_numpy()
        return tot, days

    def winter_summer():
        return np.mean([(lambda t, d: t[2] / d[2] / (t[0] / d[0]))(*seasons(n)) for n in prof])

    aer_ws = np.mean([AER_CZ6[n][2] / AER_CZ6[n][0] * 90.25 / 92 for n in prof])
    if a.shape != "flat" and a.amplitude == "auto":
        lo, hi = 0.5, 4.0
        for _ in range(30):
            shape.set_amplitude((lo + hi) / 2)
            lo, hi = (shape.k, hi) if winter_summer() < aer_ws else (lo, shape.k)
    elif a.shape != "flat":
        shape.set_amplitude(float(a.amplitude))
    print(f"\nseasonal amplitude k = {shape.k:.2f}; seasonal share of annual use (summer, autumn, winter, spring), "
          f"weather 2014-2019, vs AER 2020 CZ6:")
    for n in prof:
        tot, days = seasons(n)
        aer = np.array(AER_CZ6[n], float)
        print(f"  size {n}: model {np.round(tot / tot.sum() * 100, 1)}  AER {np.round(aer / aer.sum() * 100, 1)}  "
              f"winter/summer per day model {tot[2] / days[2] / (tot[0] / days[0]):.2f} AER {aer[2] / aer[0] * 90.25 / 92:.2f}")

    def type_share(y0, y1, kw, w):
        """households-weighted share over [y0, y1) for system size kw; returns weighted share and by type."""
        pvk, dates = pv_matrix(ex, y0, y1)
        m = shape.days(dates)
        lv = level.reindex(dates.year).to_numpy()[:, None]
        res = {t: share(load_for_size(prof, n)[None, :] * m * lv, pvk, kw, bias) for t, n in size.items()}
        return sum(res[t][0] * w[t] for t in res), res

    rows = []
    print("\nyear, new system kW, use level vs 2016, households-weighted share (raw before bias correction), by type")
    for y in range(2007, 2020):
        w = coh[coh.year == y].groupby("type").share.sum()
        tot, res = type_share(f"{y}-01-01", f"{y + 1}-01-01", new_kw[y], w)
        raw = sum(res[t][1] * w[t] for t in res)
        for t in res:
            rows.append((t, f"{y}-01-01", round(res[t][0], 3)))
        print(y, round(new_kw[y], 2), round(level[y] / a.pv_home_factor, 3), round(tot, 3), f"({raw:.3f})",
              {SHORT.get(t, t): round(float(v[0]), 2) for t, v in res.items()})
    # anchor 1: the fleet in service in FY2011-12 (mean 2.03 kW)
    w = coh[coh.year == 2012].groupby("type").share.sum()
    a1, res = type_share("2011-07-01", "2012-07-01", 2.03, w)
    xs = {SHORT.get(t, t): round(float(v[2]), 2) for t, v in res.items()}
    print("\nanchor FY2011-12, fleet in service (2.03 kW): %.3f (raw %.3f; HECS 2012: 0.67); x by type %s"
          % (a1, sum(res[t][1] * w[t] for t in res), xs))
    # anchor 2: fleet in service 2017-19 (mean kW of the fleet, not of new systems)
    cum = reg.groupby("year").aggregated_capacity_kw.sum().cumsum() + 213917 - reg[reg.year == 2011].aggregated_capacity_kw.sum()
    cumn = reg.groupby("year").number_installations.sum().cumsum() + 107239 - reg[reg.year == 2011].number_installations.sum()
    for y in (2017, 2018, 2019):
        w = coh[coh.year == y].groupby("type").share.sum()
        a2, res = type_share(f"{y}-01-01", f"{y + 1}-01-01", cum[y] / cumn[y], w)
        print("anchor %d, fleet in service (%.2f kW): %.3f (raw %.3f; AER RIN: 0.37-0.52)"
              % (y, cum[y] / cumn[y], a2, sum(res[t][1] * w[t] for t in res)))
    g = allp[allp.figure == "4.9"].pivot(index="series", columns="period", values="kwh")
    pvk, dates = pv_matrix(ex, "2016-01-01", "2017-01-01")
    m = shape.days(dates)
    for f in g.index:
        s = share(g.loc[f].to_numpy()[None, :] * m, pvk, 4.0, bias)
        print("fuel sensitivity, Figure 4.9 %s (%.1f kWh/day), 2016, 4 kW: %.3f" % (f, g.loc[f].sum(), s[0]))
    if a.write:
        out = pd.DataFrame(rows, columns=["type", "date_from", "share"])
        out["note"] = ("min(load, PV) per half-hour; ESC 2019 profile of the type's size shaped by day (weather, "
                       "calendar); model PV; year's mean new system; / SGSC averaging bias")
        out["status"] = "constructed"
        path = os.path.join(ROOT, "simulationSettings", "rooftop_self_consumption_VIC.csv")
        with open(path, "w", newline="\n") as fh:
            fh.write("# Self-consumed share of a household rooftop system's output (the rest is exported), by household type and\n"
                     "# year of installation. Built by experiments/solarAdoption/self_consumption.py: each half-hour of the year the\n"
                     "# home uses its PV output up to its load. Load: ACIL Allen (2019) for the ESC, Victorian usage profiles by\n"
                     "# household size (Figure 4.2, mostly 2016 smart-meter data), at the type's mean size (census 2016), shaped by\n"
                     "# day from Melbourne temperature, day type, night length and Christmas (temperature response of Victorian\n"
                     "# small-customer load, AEMO NSLP 2002-2010, ERA5), scaled to the year's use per household (AES Table F3 /\n"
                     "# census households) x %.2f for homes that adopt PV (Figure 4.8). PV: the model's rooftop output per kW x the\n"
                     "# year's mean new system (installation register). Divided by the averaging bias measured on 1,217 Smart Grid\n"
                     "# Smart City homes (an average profile overstates the share 5-28 %% against measured loads, by PV/load ratio).\n"
                     % a.pv_home_factor)
            out.to_csv(fh, index=False)
        print("written", path)


if __name__ == "__main__":
    main()
