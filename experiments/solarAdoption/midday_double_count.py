"""Does the model take rooftop PV out of the wholesale demand twice at midday?

How the base case builds the half-hourly wholesale demand (VIC.yaml shapeDemandUplift: true):
  1. Op(t)  AEMO VIC1 TOTALDEMAND (`total_demand_halfhour`). Rooftop PV is behind the meter, so Op is already net of
            rooftop output, midday dip included.
  2. LoadData.calibrate30minHistoricDemandFromConsumption adds a flat monthly uplift U_m (+ the net-export shape) so the
     month sums to `total_consumption_gwh`, which INCLUDES the recorded rooftop generation (fetch_openelectricity.py).
     The recorded rooftop energy of the month is therefore added back spread evenly over every half-hour.
  3. Arena (lines 559-599) subtracts the model's rooftop output R(t) = capacity x GHI/1000 x efficiency in each half-hour.

Net effect against the correct series (Op plus the uplift without the rooftop part):
  delta(t) = roof_rec_m / n_m  -  R_model(t)
           = [Rbar_model_m - R_model(t)]            shape error: too low at midday, too high at night (sums to 0)
           + [roof_rec_m - E_model_m] / n_m          level error: recorded vs modelled rooftop energy (flat)

The script replicates step 3 outside the model: capacity = cumulative APVI kW (`solar_installation_monthly`, the
model adds each month's installations as new generators), GHI from `solar_ghi` at the model's timestamps, efficiency
from VIC.yaml (fman x fdirt x ftemp x npv_inv x ninv x ninv_sb = 0.716; technologicalImprovement is 0 in the base
case). Small off-market solar farms are left out (they are not rooftop).

Output: data/midday_double_count.csv (per year) and the printed table.
Usage: python midday_double_count.py [--years 2010-2019]
"""
import argparse, io, os, subprocess

import numpy as np
import pandas as pd

HERE = os.path.dirname(os.path.abspath(__file__))
PSQL = r"C:\Program Files\PostgreSQL\18\bin\psql.exe"
EFF = 0.97 * 0.95 * 0.9 * 0.97 * 0.9 * 0.99


def query(sql):
    out = subprocess.run([PSQL, "-U", "postgres", "-d", "gr4spdb", "-A", "-F", ",", "-P", "footer=off", "-c", sql],
                         capture_output=True, text=True, check=True).stdout
    return pd.read_csv(io.StringIO(out))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--years", default="2010-2019")
    ap.add_argument("--out", default=os.path.join(HERE, "data", "midday_double_count.csv"))
    a = ap.parse_args()
    y0, y1 = map(int, a.years.split("-"))
    op = query(f"select settlement_date t, total_demand op from total_demand_halfhour "
               f"where settlement_date > '{y0}-01-01' and settlement_date <= '{y1 + 1}-01-01'")
    ghi = query(f"select time t, ghi from solar_ghi where time > '{y0}-01-01' and time <= '{y1 + 1}-01-01'")
    cap = query("select year, month, aggregated_capacity_kw from solar_installation_monthly order by year, month")
    rec = query(f"select date, solar_roofpv_gwh from generation_consumption_historic "
                f"where date >= '{y0}-01-01' and date < '{y1 + 1}-01-01'")
    # `solar_ghi` holds identical copies of most half-hours; the model keeps one value per timestamp (a HashMap,
    # loaded ORDER BY time, ghi, so the last = largest); the demand register keeps the first row per timestamp
    ghi = ghi.assign(t=pd.to_datetime(ghi.t)).groupby("t", as_index=False).ghi.max()
    op = op.assign(t=pd.to_datetime(op.t)).drop_duplicates("t", keep="first")
    d = op.merge(ghi, on="t", how="left")
    d["ghi"] = d.ghi.fillna(0.0)  # the model uses 0 when a timestamp is missing
    # the model's month of a half-hour: the calendar month of the period end, as in Arena's monthly loop
    d["ym"] = d.t.dt.to_period("M")
    cap["ym"] = pd.PeriodIndex.from_fields(year=cap.year, month=cap.month, freq="M")
    cap["cum_mw"] = cap.aggregated_capacity_kw.cumsum() / 1000.0
    # installations added at the start of their month; months after the last record keep the last capacity
    cm = cap.set_index("ym").cum_mw
    allm = pd.period_range(cm.index.min(), f"{y1}-12", freq="M")
    cm = cm.reindex(allm).ffill()
    d["cap_mw"] = d.ym.map(cm).fillna(0.0)
    d["r_model"] = d.cap_mw * d.ghi / 1000.0 * EFF
    rec["ym"] = pd.to_datetime(rec.date).dt.to_period("M")
    d["n_m"] = d.groupby("ym").t.transform("size")
    d["roof_rec_flat"] = d.ym.map(rec.set_index("ym").solar_roofpv_gwh).fillna(0.0) * 2000.0 / d.n_m
    d["rbar_model"] = d.groupby("ym").r_model.transform("mean")
    d["delta"] = d.roof_rec_flat - d.r_model
    d["delta_shape"] = d.rbar_model - d.r_model
    # reference wholesale demand (rooftop counted once): Op + uplift without rooftop; uplift and export shape are
    # common to both series, so the comparison uses Op as the reference level
    d["ref"] = d.op
    d["model"] = d.op + d.delta
    start = d.t - pd.Timedelta(minutes=30)  # interval start, local market time
    hr = start.dt.hour + start.dt.minute / 60
    mid = (hr >= 10) & (hr < 15)
    eve = (hr >= 17) & (hr < 21)
    summer = start.dt.month.isin([12, 1, 2])
    rows = []
    for y, g in d.groupby(start.dt.year):
        if not (y0 <= y <= y1):
            continue
        m, e, s = mid[g.index], eve[g.index], summer[g.index]
        rows.append(dict(
            year=y,
            rooftop_mw_end=g.cap_mw.max(),
            roof_model_gwh=g.r_model.sum() / 2000 * 1000 / 1000,
            roof_recorded_gwh=g.roof_rec_flat.sum() / 2000 * 1000 / 1000,
            delta_midday_mean_mw=g.delta[m].mean(),
            delta_midday_summer_mean_mw=g.delta[m & s].mean(),
            delta_min_mw=g.delta.min(),
            delta_evening_mean_mw=g.delta[e].mean(),
            delta_midday_pct_of_ref=100 * g.delta[m].mean() / g.ref[m].mean(),
            shape_part_midday_mw=g.delta_shape[m].mean(),
            min_demand_ref_mw=g.ref.min(),
            min_demand_model_mw=g.model.min(),
            max_demand_ref_mw=g.ref.max(),
            max_demand_model_mw=g.model.max(),
            hh_dip_over_5pct=100 * (g.delta < -0.05 * g.ref).mean(),
        ))
    r = pd.DataFrame(rows)
    os.makedirs(os.path.dirname(a.out), exist_ok=True)
    r.to_csv(a.out, index=False, float_format="%.1f")
    pd.set_option("display.width", 250)
    print(f"efficiency {EFF:.4f}")
    print(r.round(1).to_string(index=False))


if __name__ == "__main__":
    main()
