"""Temperature response of Victorian small-customer load, by half-hour (solar step 3, D4b).

Data: Victorian small-customer half-hourly load (aemo_small_customers.py): NSLP 2002-2010 (accumulation-metered homes
and small businesses, before the AMI rollout; 2008 missing) and MRIM Jul 2018-Dec 2019 (smart-metered sites under
160 MWh a year; 2020 left out: COVID-19 lockdowns). Sum of the five networks. ERA5 hourly 2 m temperature for Melbourne
(fetch_era5_temperature.py), AEST.

Definitions:
  Relative load y(d, h): load in half-hour h of day d divided by the mean load of half-hour h over that calendar year
    (removes customer numbers and the level trend; leaves the shape across days).
  Heating degree days HDD(d) = max(0, Th - T(d)), cooling degree days CDD(d) = max(0, T(d) - Tc), T(d) the day's
    mean temperature (00:00-24:00 AEST); thresholds Th, Tc chosen by least squares on daily relative load
    (grid 10-20 and 14-26 deg C). CDD1 = previous day's CDD (heat build-up in heatwaves).
  Model, for each half-hour h and day type (weekday; weekend or public holiday) separately:
    y(d, h) = c + bH HDD + bC CDD + bC1 CDD1 + bN N(d) + bX Xmas(d)
    N(d): night length (hours between sunset and sunrise at Melbourne, astronomical), the seasonal driver that is not
    weather (lighting, time indoors); Xmas: 24 Dec - 6 Jan (holiday period).
Fit 2002-2007, test 2009-2010 (out of sample); MRIM fitted on Jul 2018-Dec 2019 with the same thresholds.

Output: data/load_temperature_response.csv (period, daytype, period_hh, term, value) and data/load_temperature_fit.png.
Usage: python load_temperature_response.py AEMO_CSV_GZ ERA5_CSV
"""
import argparse, os
import numpy as np
import pandas as pd
import holidays

HERE = os.path.dirname(os.path.abspath(__file__))
P = [f"p{i:02d}" for i in range(1, 49)]
LAT = -37.67


def night_hours(doy):
    """Astronomical night length (h) at Melbourne's latitude (solar declination, no refraction)."""
    decl = np.radians(23.44) * np.sin(2 * np.pi * (284 + doy) / 365.0)
    cosw = -np.tan(np.radians(LAT)) * np.tan(decl)
    return 24 - 2 * np.degrees(np.arccos(np.clip(cosw, -1, 1))) / 15.0


def daily_temperature(era5):
    t = pd.read_csv(era5, comment="#", parse_dates=["time_aest"])
    d = t.set_index("time_aest").t2m_c.resample("D").agg(["mean", "max", "min", "count"])
    return d[d["count"] >= 20]


def frame(load, temp, src, years):
    v = load[(load.source == src)].groupby("date")[P].sum()
    v.index = pd.to_datetime(v.index)
    v = v[v.index.year.isin(years)]
    y = v / v.groupby(v.index.year).transform("mean")
    f = day_features(v.index, temp)
    return y.loc[f.index], f


def day_features(index, temp):
    """Explanatory variables of each day: temperature, previous day's temperature, day type, night length, Christmas."""
    vic = holidays.AU(subdiv="VIC", years=range(index.year.min() - 1, index.year.max() + 1))
    f = pd.DataFrame(index=index)
    f["T"] = temp["mean"].reindex(f.index)
    f["Tprev"] = temp["mean"].shift(1, freq="D").reindex(f.index)
    f["dtype"] = np.where((f.index.day_of_week >= 5) | f.index.isin(pd.to_datetime(list(vic))), "weekend", "weekday")
    f["N"] = night_hours(f.index.day_of_year.values)
    md = f.index.month * 100 + f.index.day
    f["X"] = ((md >= 1224) | (md <= 106)).astype(float)
    return f.dropna()


def load_response(path, period="2002-2010 NSLP"):
    """Read the coefficients and thresholds written by this script: ({daytype: terms x 48 array}, Th, Tc)."""
    r = pd.read_csv(path, comment="#")
    th = r[(r.term == "Th")].value.iloc[0]
    tc = r[(r.term == "Tc")].value.iloc[0]
    r = r[(r.period == period) & r.term.isin(TERMS)]
    coef = {dt: g.pivot(index="term", columns="period_hh", values="value").loc[TERMS].to_numpy()
            for dt, g in r.groupby("daytype")}
    return coef, th, tc


def design(f, th, tc):
    return np.column_stack([np.ones(len(f)), np.maximum(0, th - f["T"]), np.maximum(0, f["T"] - tc),
                            np.maximum(0, f["Tprev"] - tc), f["N"], f["X"]])


TERMS = ["const", "HDD", "CDD", "CDD1", "night_h", "xmas"]


def fit(y, f, th, tc):
    coef = {}
    for dt in ("weekday", "weekend"):
        m = f.dtype == dt
        X = design(f[m], th, tc)
        b, *_ = np.linalg.lstsq(X, y[m].to_numpy(), rcond=None)
        coef[dt] = b  # terms x 48
    return coef


def predict(coef, f, th, tc):
    out = pd.DataFrame(index=f.index, columns=P, dtype=float)
    for dt in ("weekday", "weekend"):
        m = f.dtype == dt
        out.loc[m] = design(f[m], th, tc) @ coef[dt]
    return out


def r2(obs, pred):
    o, p = np.asarray(obs, float).ravel(), np.asarray(pred, float).ravel()
    return 1 - ((o - p) ** 2).sum() / ((o - o.mean()) ** 2).sum()


def thresholds(y, f):
    best = None
    yd = y.mean(axis=1)
    for th in np.arange(10, 20.5, 0.5):
        for tc in np.arange(14, 26.5, 0.5):
            if tc < th:
                continue
            res = 0
            for dt in ("weekday", "weekend"):
                m = f.dtype == dt
                X = design(f[m], th, tc)
                b, *_ = np.linalg.lstsq(X, yd[m].to_numpy(), rcond=None)
                res += ((yd[m].to_numpy() - X @ b) ** 2).sum()
            if best is None or res < best[0]:
                best = (res, th, tc)
    return best[1], best[2]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("aemo")
    ap.add_argument("era5")
    a = ap.parse_args()
    load = pd.read_csv(a.aemo)
    temp = daily_temperature(a.era5)
    rows, report = [], []
    # period 1: NSLP, fit 2002-2007, test 2009-2010; final coefficients on 2002-2010
    y, f = frame(load, temp, "NSLP", [2002, 2003, 2004, 2005, 2006, 2007, 2009, 2010])
    tr, te = f.index.year <= 2007, f.index.year >= 2009
    th, tc = thresholds(y[tr], f[tr])
    c = fit(y[tr], f[tr], th, tc)
    pr = predict(c, f, th, tc)
    report.append(("NSLP 2002-07 fit", th, tc, r2(y[tr], pr[tr]), r2(y[tr].sum(axis=1), pr[tr].sum(axis=1))))
    report.append(("NSLP 2009-10 test", th, tc, r2(y[te], pr[te]), r2(y[te].sum(axis=1), pr[te].sum(axis=1))))
    # baseline without weather (day type, night length, Christmas only), to show what temperature adds
    c0 = {}
    for dt in ("weekday", "weekend"):
        m = (f.dtype == dt) & tr
        X = design(f[m], th, tc)[:, [0, 4, 5]]
        c0[dt], *_ = np.linalg.lstsq(X, y[m].to_numpy(), rcond=None)
    p0 = pd.DataFrame(index=f.index, columns=P, dtype=float)
    for dt in ("weekday", "weekend"):
        m = f.dtype == dt
        p0.loc[m] = design(f[m], th, tc)[:, [0, 4, 5]] @ c0[dt]
    report.append(("NSLP 2009-10 test, no weather", th, tc, r2(y[te], p0[te]), r2(y[te].sum(axis=1), p0[te].sum(axis=1))))
    cfin = fit(y, f, th, tc)
    for dt in cfin:
        for i, term in enumerate(TERMS):
            for h in range(48):
                rows.append(("2002-2010 NSLP", dt, h + 1, term, cfin[dt][i, h]))
    # period 2: MRIM Jul 2018 - Dec 2019, same thresholds
    y2, f2 = frame(load, temp, "MRIM", [2018, 2019])
    c2 = fit(y2, f2, th, tc)
    p2 = predict(c2, f2, th, tc)
    report.append(("MRIM 2018-19 fit", th, tc, r2(y2, p2), r2(y2.sum(axis=1), p2.sum(axis=1))))
    for dt in c2:
        for i, term in enumerate(TERMS):
            for h in range(48):
                rows.append(("2018-2019 MRIM", dt, h + 1, term, c2[dt][i, h]))
    print("thresholds: heating below %.1f C, cooling above %.1f C (daily mean temperature)" % (th, tc))
    print(pd.DataFrame(report, columns=["set", "Th", "Tc", "R2 half-hourly", "R2 daily"]).round(3).to_string(index=False))
    rows += [("thresholds", "all", 0, "Th", th), ("thresholds", "all", 0, "Tc", tc)]
    out = pd.DataFrame(rows, columns=["period", "daytype", "period_hh", "term", "value"])
    path = os.path.join(HERE, "data", "load_temperature_response.csv")
    with open(path, "w", newline="\n") as fh:
        fh.write("# Temperature response of Victorian small-customer load by half-hour (period_hh 1 = 00:00-00:30) and day type:\n"
                 "# relative load (half-hour load / that half-hour's mean over the year) = const + HDD x b + CDD x b + CDD1 x b\n"
                 "# + night_h x b + xmas x b. HDD = max(0, %.1f - T), CDD = max(0, T - %.1f), T = daily mean 2 m temperature\n"
                 "# (ERA5, Melbourne, deg C); CDD1 = previous day's CDD; night_h = night length (h); xmas = 24 Dec-6 Jan.\n"
                 "# Weekend includes Victorian public holidays. Rows with term Th and Tc hold the two thresholds.\n"
                 "# NSLP: AEMO net system load profile, 5 Victorian networks, 2002-2010 (no 2008); MRIM: AEMO type 5 meter\n"
                 "# data, Jul 2018-Dec 2019. Built by experiments/solarAdoption/load_temperature_response.py.\n" % (th, tc))
        out.to_csv(fh, index=False, float_format="%.6f")
    print("written", path)
    # summary of the responses (per degree day, % of the half-hour's annual mean), selected times
    piv = out[out.term.isin(["HDD", "CDD", "CDD1"])].pivot_table(index=["period", "daytype", "term"], columns="period_hh", values="value")
    print("\nresponse per degree day (% of annual mean), at 03:00, 07:30, 12:00, 15:00, 18:30, 21:00:")
    print((piv[[7, 16, 25, 31, 38, 43]] * 100).round(2).to_string())
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    fig, ax = plt.subplots(1, 3, figsize=(13, 3.6), sharex=True)
    for k, term in enumerate(["HDD", "CDD", "CDD1"]):
        for (per, dt), g in out[out.term == term].groupby(["period", "daytype"]):
            ax[k].plot(g.period_hh, g.value * 100, label=f"{per}, {dt}", ls="-" if dt == "weekday" else "--")
        ax[k].set_title(f"{term}: % of half-hour mean per degree day"); ax[k].axhline(0, c="k", lw=.5); ax[k].grid(alpha=.3)
        ax[k].set_xlabel("Half-hour period")
    ax[0].legend(fontsize=7)
    plt.tight_layout(); plt.savefig(os.path.join(HERE, "data", "load_temperature_fit.png"), dpi=110)


if __name__ == "__main__":
    main()
