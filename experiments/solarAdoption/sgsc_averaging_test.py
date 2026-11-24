"""How much does replacing a home's real half-hourly load by an average profile bias its PV self-consumed share?
(solar step 3, D4b.)

Data: sgsc_sample.py output (Smart Grid Smart City trial, Ausgrid, NSW), one calendar year per household with at
least 330 complete days. PV: (a) for every household, the model's own rooftop output per kW for Melbourne on the same
dates (DumpRooftop exposure dump) at 2, 4 and 6 kW; (b) for gross-metered solar homes, also their own measured PV.

Self-consumed share = sum over half-hours of min(load, PV) / sum of PV (net metering at half-hour resolution).
Load variants, all with the household's own annual use:
  exact     the home's measured load
  own_ann   the home's own average day (48 values), every day of the year
  own_mdt   the home's own average day by month and day type (weekday / weekend)
  pop_ann   the sample's mean daily shape (all homes of the group), scaled to the home's annual use: what the ESC
            profile by household size is in self_consumption.py
  pop_mdt   the sample's mean shape by month and day type, scaled to the home's annual use
  pop_day   the sample's mean shape on each calendar day, scaled to the home's annual use: the best any population
            profile driven by weather and calendar can do (a perfect model of the average home's day)
The ratio variant / exact over all homes (shares summed with PV weights) is the bias of each approximation.
Usage: python sgsc_averaging_test.py SAMPLE_DIR EXPOSURE_CSV [--year 2013]
"""
import argparse, os
import numpy as np
import pandas as pd


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dir")
    ap.add_argument("exposure")
    ap.add_argument("--year", type=int, default=2013)
    a = ap.parse_args()
    hh = pd.read_csv(os.path.join(a.dir, "sgsc_sample_households.csv"), low_memory=False).set_index("CUSTOMER_KEY")
    s = pd.read_csv(os.path.join(a.dir, "sgsc_sample.csv.gz"), dtype={"customer": "int64"},
                    usecols=["customer", "time", "general_kwh", "gross_gen_kwh"])
    s = s[s.time.str.startswith(str(a.year))]
    s["time"] = pd.to_datetime(s.time, format="%Y-%m-%d %H:%M:%S")
    s["date"] = s.time.dt.normalize()
    s["hh"] = s.time.dt.hour * 2 + s.time.dt.minute // 30
    # timing check: mean PV of the gross-solar homes and mean load, by half-hour (stamps are period start, standard
    # time: PV peaks at period 24-25, 12:00-13:00, in January and 23-24 in July; load peaks at 38-39, 19:00-20:00)
    gs = s[s.customer.isin(hh.index[hh.group == "gross_solar"])]
    print("PV peak period (Jan, Jul):", [int(gs[gs.time.dt.month == m].groupby("hh").gross_gen_kwh.mean().idxmax())
                                         for m in (1, 7)], " load peak period:", int(s.groupby("hh").general_kwh.mean().idxmax()))
    L = s.pivot_table(index=["customer", "date"], columns="hh", values="general_kwh", aggfunc="sum")
    G = s.pivot_table(index=["customer", "date"], columns="hh", values="gross_gen_kwh", aggfunc="sum")
    L = L.dropna()
    ndays = L.groupby(level=0).size()
    ok = ndays[ndays >= 330].index
    L = L.loc[ok]
    print(f"{a.year}: households with >= 330 complete days: {len(ok)} "
          f"({hh.loc[hh.index.intersection(ok)].group.value_counts().to_dict()})")
    ex = pd.read_csv(a.exposure, parse_dates=["time"])
    ex = ex[ex.time.dt.year == a.year]
    ex["date"] = ex.time.dt.normalize()
    ex["hh"] = ex.time.dt.hour * 2 + ex.time.dt.minute // 30
    pv = ex.assign(k=ex.poa_kw_m2 * ex.efficiency * 0.5).pivot_table(index="date", columns="hh", values="k")
    dates = L.index.get_level_values(1)
    month = dates.month
    wkend = (dates.dayofweek >= 5).astype(int)
    grp = hh.reindex(L.index.get_level_values(0)).group.to_numpy()
    Lv = L.to_numpy()
    cust = L.index.get_level_values(0).to_numpy()
    df = pd.DataFrame(Lv, index=L.index)
    # own averages
    own_ann = df.groupby(level=0).transform("mean").to_numpy()
    key = pd.MultiIndex.from_arrays([cust, month, wkend])
    own_mdt = pd.DataFrame(Lv, index=key).groupby(level=[0, 1, 2]).transform("mean").to_numpy()
    # population shapes (normalised per home: each home's day / its annual mean daily use), by group
    use = df.sum(axis=1).groupby(level=0).transform("mean").to_numpy()  # mean daily kWh per home
    norm = Lv / use[:, None]
    pop_ann = pd.DataFrame(norm).groupby(grp).transform("mean").to_numpy() * use[:, None]
    pk = pd.MultiIndex.from_arrays([grp, month, wkend])
    pop_mdt = pd.DataFrame(norm, index=pk).groupby(level=[0, 1, 2]).transform("mean").to_numpy() * use[:, None]
    dk = pd.MultiIndex.from_arrays([grp, dates])
    pop_day = pd.DataFrame(norm, index=dk).groupby(level=[0, 1]).transform("mean").to_numpy() * use[:, None]
    variants = {"exact": Lv, "own_ann": own_ann, "own_mdt": own_mdt, "pop_ann": pop_ann, "pop_mdt": pop_mdt,
                "pop_day": pop_day}
    rows = []
    pvd = pv.reindex(dates).to_numpy()
    for kw in (2, 4, 6):
        P = pvd * kw
        for name, X in variants.items():
            sc = np.minimum(X, P)
            for gname in ("gross_solar", "no_solar_surveyed"):
                m = grp == gname
                rows.append(("Melbourne model PV", kw, gname, name, np.nansum(sc[m]) / np.nansum(P[m])))
    # own measured PV of the gross-solar homes
    Gv = G.reindex(L.index).to_numpy()
    m = (grp == "gross_solar") & (np.nansum(Gv, axis=1) > 0)
    for name, X in variants.items():
        rows.append(("own measured PV", 0, "gross_solar", name, np.nansum(np.minimum(X[m], Gv[m])) / np.nansum(Gv[m])))
    # bias as a function of the PV-to-load ratio x = annual PV / annual load, per home (Melbourne PV, 1-8 kW)
    per = []
    for kw in (1, 1.5, 2, 3, 4, 5, 6, 8):
        P = pvd * kw
        for name in ("exact", "pop_ann", "pop_mdt", "pop_day"):
            num = pd.Series(np.nansum(np.minimum(variants[name], P), axis=1)).groupby(cust).sum()
            den = pd.Series(np.nansum(P, axis=1)).groupby(cust).sum()
            per.append(pd.DataFrame({"kw": kw, "load": name, "share": num / den,
                                     "x": den / pd.Series(np.nansum(Lv, axis=1)).groupby(cust).sum()}))
    per = pd.concat(per).reset_index(names="customer")
    w = per.pivot_table(index=["customer", "kw"], columns="load", values=["share", "x"])
    b = pd.DataFrame({"x": w[("x", "exact")], "exact": w[("share", "exact")], "pop_ann": w[("share", "pop_ann")],
                      "pop_mdt": w[("share", "pop_mdt")], "pop_day": w[("share", "pop_day")]}).dropna()
    b["xbin"] = pd.cut(b.x, [0, .1, .2, .3, .4, .5, .6, .8, 1.0, 1.5, 3])
    t = b.groupby("xbin", observed=True).agg(n=("x", "size"), exact=("exact", "mean"), pop_ann=("pop_ann", "mean"),
                                             pop_mdt=("pop_mdt", "mean"), pop_day=("pop_day", "mean"))
    t["ratio_ann"] = t.pop_ann / t.exact
    t["ratio_mdt"] = t.pop_mdt / t.exact
    t["ratio_day"] = t.pop_day / t.exact
    print("\nper home, by PV-to-load ratio x (annual PV / annual load):")
    print(t.round(3).to_string())
    t.to_csv(os.path.join(os.path.dirname(os.path.abspath(__file__)), "data", "sgsc_averaging_bias_by_x.csv"),
             float_format="%.4f")
    r = pd.DataFrame(rows, columns=["pv", "kw", "group", "load", "share"])
    piv = r.pivot_table(index=["pv", "kw", "group"], columns="load", values="share")
    piv = piv[["exact", "own_ann", "own_mdt", "pop_ann", "pop_mdt", "pop_day"]]
    print("\nself-consumed share by load variant:")
    print(piv.round(3).to_string())
    print("\nratio to exact:")
    print(piv.div(piv.exact, axis=0).round(3).to_string())
    print("\nmean daily use (kWh): ", pd.Series(use, index=cust).groupby(level=0).first().groupby(
        hh.reindex(ok).group).mean().round(2).to_dict())
    out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data", "sgsc_averaging_test.csv")
    with open(out, "w", newline="\n") as fh:
        fh.write(f"# Self-consumed PV share with measured vs averaged household loads, Smart Grid Smart City trial "
                 f"(Ausgrid), calendar {a.year}; built by sgsc_averaging_test.py\n")
        r.to_csv(fh, index=False, float_format="%.4f")
    print("written", out)


if __name__ == "__main__":
    main()
