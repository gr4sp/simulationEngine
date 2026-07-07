"""
Fetch monthly generation, revenue, and price data for VIC1 and load into
gr4spdb.generation_consumption_historic.

Two sources spliced together to get real per-fueltech revenue across the
whole 2020-10 to 2025-12 gap (the paid v4 API alone can't reach back that
far, and doesn't expose per-fueltech revenue anyway):

  - OpenElectricity's free static historical stats file
    (data.openelectricity.org.au/v3/stats/au/all/monthly.json) — no auth,
    no plan limit. Covers energy + per-fueltech market_value (= total AUD
    revenue that month — despite the *_dollargwh column names, these are
    NOT a $/GWh rate, they match the absolute-dollar convention the
    pre-existing rows already use) + imports/exports, from 1998 through
    whatever month it was last regenerated (currently ~2024-11).
  - The paid v4 API (OPENELECTRICITY_API_KEY, 730-day plan limit) for the
    months after the static file's coverage ends, through END. v4's
    network-data endpoint has no imports/exports market_value/energy, so
    those two columns (and their *_dollargwh) stay NULL for that tail
    period only.

Price (volumeweightedprice_dollarmwh) is computed the same way for both
sources — total $ divided by total energy — rather than taken from v4's
separate "price" metric, which turned out not to be volume-weighted (it
diverged ~15-20% from the demand-value-based ratio on cross-check).

Setup:
  Copy .env.example to ../../.env and fill in OPENELECTRICITY_API_KEY and DB_URL.
  pip install -r requirements.txt

Run from project root:
  python scripts/data/fetch_openelectricity.py
"""

import os
import requests
import pandas as pd
import psycopg2
from dotenv import load_dotenv

load_dotenv()

API_KEY = os.environ["OPENELECTRICITY_API_KEY"]
DB_URL  = os.environ["DB_URL"]

BASE_URL   = "https://api.openelectricity.org.au/v4"
HEADERS    = {"Authorization": f"Bearer {API_KEY}", "Accept": "application/json"}
STATIC_URL = "https://data.openelectricity.org.au/v3/stats/au/all/monthly.json"
REGION     = "VIC1"

START = "2020-10-01"
END   = "2025-12-31"

# fuel type → (gwh column, dollargwh column) in generation_consumption_historic
FUEL_MAP = {
    "solar_rooftop":      ("solar_roofpv_gwh",    "solar_roofpv_dollargwh"),
    "solar_utility":      ("solar_utility_gwh",   "solar_utility_dollargwh"),
    "wind":               ("wind_gwh",             "wind_dollargwh"),
    "hydro":              ("hydro_gwh",            "hydro_dollargwh"),
    "battery_discharging":("battery_disch_gwh",   "battery_disch_dollargwh"),
    "gas_ocgt":           ("gas_ocgt_gwh",         "gas_ocgt_dollargwh"),
    "gas_steam":          ("gas_steam_gwh",        "gas_steam_dollargwh"),
    "coal_brown":         ("browncoal_gwh",        "browncoal_dollargwh"),
    "imports":            ("imports_gwh",          "imports_dollargwh"),
    "exports":            ("exports_gwh",          "exports_dollargwh"),
}


def _series_to_monthly(record):
    """Turn one static-file {history: {start, data}} record into a
    {(year, month): value} dict."""
    months = pd.date_range(
        start=pd.Timestamp(record["history"]["start"]).tz_localize(None),
        periods=len(record["history"]["data"]),
        freq="MS",
    )
    return {(m.year, m.month): v for m, v in zip(months, record["history"]["data"])}


def fetch_static_monthly():
    """Fetch the free static stats file and return
    ({(year, month): {"price":..., "fuels": {fuel: {"gwh":..., "dollar":...}}}},
     last_month_covered) for VIC1, restricted to [START, END]."""
    print("Fetching free static OpenElectricity historical stats...")
    r = requests.get(STATIC_URL, timeout=60)
    r.raise_for_status()
    records = {d["id"]: d for d in r.json()["data"] if d.get("region") == REGION}

    demand_energy = _series_to_monthly(records["au.nem.vic1.demand.energy"])
    demand_value  = _series_to_monthly(records["au.nem.vic1.demand.market_value"])
    last_month = max(demand_energy)

    fuel_series = {}
    for fuel in FUEL_MAP:
        energy_id = f"au.nem.vic1.fuel_tech.{fuel}.energy"
        value_id  = f"au.nem.vic1.fuel_tech.{fuel}.market_value"
        if energy_id in records:
            fuel_series[fuel] = (_series_to_monthly(records[energy_id]),
                                  _series_to_monthly(records[value_id]))

    start_ym = (pd.Timestamp(START).year, pd.Timestamp(START).month)
    end_ym   = (pd.Timestamp(END).year, pd.Timestamp(END).month)

    monthly = {}
    for ym, energy_gwh in demand_energy.items():
        if not (start_ym <= ym <= end_ym):
            continue
        price = (demand_value[ym] / (energy_gwh * 1000)) if energy_gwh else None
        fuels = {}
        for fuel, (energy_s, value_s) in fuel_series.items():
            if ym not in energy_s:
                continue
            # The static file's imports/exports market_value is always ~0 —
            # a "not computed" placeholder, not a real figure — so leave
            # dollar NULL for those two rather than insert a misleading 0.
            dollar = None if fuel in ("imports", "exports") else value_s.get(ym)
            fuels[fuel] = {"gwh": energy_s[ym], "dollar": dollar}
        monthly[ym] = {"price": price, "fuels": fuels}

    return monthly, last_month


def fetch_v4_monthly(start, end):
    """Fetch energy + market_value by fueltech, plus network-level
    market_value (for the price ratio), from the paid v4 API."""
    print(f"Fetching v4 API data for {start} to {end}...")

    def get(metrics, grouped):
        params = {
            "metrics": metrics, "interval": "1M",
            "date_start": start, "date_end": end,
            "network_region": REGION,
        }
        if grouped:
            params["secondary_grouping"] = "fueltech"
        r = requests.get(f"{BASE_URL}/data/network/NEM", headers=HEADERS, params=params, timeout=60)
        r.raise_for_status()
        return (r.json().get("data") or [{}])[0].get("results", [])

    def to_fuel_dict(results):
        out = {}
        for series in results:
            fuel = series["columns"].get("fueltech")
            out[fuel] = {pd.to_datetime(ts).date(): (v or 0.0) for ts, v in series["data"]}
        return out

    energy_by_fuel = to_fuel_dict(get("energy", grouped=True))
    value_by_fuel  = to_fuel_dict(get("market_value", grouped=True))
    network_value_results = get("market_value", grouped=False)
    network_value = ({pd.to_datetime(ts).date(): v for ts, v in network_value_results[0]["data"]}
                      if network_value_results else {})

    all_dates = sorted({d for series in energy_by_fuel.values() for d in series})
    monthly = {}
    for d in all_dates:
        fuels = {}
        total_gwh = 0.0
        for fuel in FUEL_MAP:
            if fuel in energy_by_fuel and d in energy_by_fuel[fuel]:
                gwh = energy_by_fuel[fuel][d] / 1000.0
                fuels[fuel] = {"gwh": gwh, "dollar": value_by_fuel.get(fuel, {}).get(d)}
                if fuel not in ("imports", "exports"):
                    total_gwh += gwh
        price = (network_value.get(d) / (total_gwh * 1000)) if total_gwh else None
        monthly[(d.year, d.month)] = {"price": price, "fuels": fuels}

    return monthly


def build_rows(monthly):
    """Combine the merged monthly dict into one row per month matching the DB schema."""
    rows = []
    for (year, month), info in sorted(monthly.items()):
        row = {
            "date": pd.Timestamp(year=year, month=month, day=1).date(),
            "temperaturec": None,
            "volumeweightedprice_dollarmwh": info["price"],
        }

        # Initialise all fuel columns to None
        for gwh_col, dollar_col in FUEL_MAP.values():
            row[gwh_col]    = None
            row[dollar_col] = None

        for fuel, vals in info["fuels"].items():
            gwh_col, dollar_col = FUEL_MAP[fuel]
            row[gwh_col]    = vals["gwh"]
            row[dollar_col] = vals["dollar"]

        # total_consumption_gwh = all generation - exports + imports
        gen_cols = [c for c in row if c.endswith("_gwh") and c not in ("imports_gwh", "exports_gwh")]
        total = sum(row[c] or 0 for c in gen_cols)
        total += (row.get("imports_gwh") or 0)
        total -= (row.get("exports_gwh") or 0)
        row["total_consumption_gwh"] = total if total != 0 else None

        rows.append(row)

    return rows


def load_to_db(rows):
    """Delete existing rows in range and insert new ones."""
    if not rows:
        print("No rows to insert.")
        return

    conn = psycopg2.connect(DB_URL)
    cur  = conn.cursor()

    print(f"Deleting existing rows from {START}...")
    cur.execute("DELETE FROM generation_consumption_historic WHERE date >= %s", (START,))

    cols = list(rows[0].keys())
    placeholders = ", ".join(["%s"] * len(cols))
    col_names    = ", ".join(cols)
    insert_sql   = f"INSERT INTO generation_consumption_historic ({col_names}) VALUES ({placeholders})"

    for row in rows:
        values = [v.item() if hasattr(v, "item") else v for v in (row[c] for c in cols)]
        cur.execute(insert_sql, values)

    conn.commit()
    cur.close()
    conn.close()
    print(f"Inserted {len(rows)} rows into generation_consumption_historic.")


def update_temperature(temp_csv_path):
    """
    Update temperaturec column using monthly averages produced by fetch_era5.py.
    Call this after fetch_era5.py has run and written monthly_temp.csv.
    """
    if not os.path.exists(temp_csv_path):
        print(f"Temperature file not found: {temp_csv_path}. Run fetch_era5.py first.")
        return

    df   = pd.read_csv(temp_csv_path, parse_dates=["date"])
    conn = psycopg2.connect(DB_URL)
    cur  = conn.cursor()

    for _, row in df.iterrows():
        cur.execute(
            "UPDATE generation_consumption_historic SET temperaturec = %s WHERE date = %s",
            (row["temp_c"], row["date"].date()),
        )

    conn.commit()
    cur.close()
    conn.close()
    print(f"Updated temperaturec for {len(df)} months.")


if __name__ == "__main__":
    static_monthly, static_last_month = fetch_static_monthly()
    print(f"Static file covers through {static_last_month[0]}-{static_last_month[1]:02d}")

    v4_start = pd.Timestamp(year=static_last_month[0], month=static_last_month[1], day=1) + pd.DateOffset(months=1)
    v4_monthly = {}
    if v4_start.date().isoformat() <= END:
        v4_monthly = fetch_v4_monthly(v4_start.date().isoformat(), END)

    monthly = {**static_monthly, **v4_monthly}
    rows = build_rows(monthly)
    load_to_db(rows)

    # Update temperature if ERA5 output already exists
    temp_csv = os.path.join(os.path.dirname(__file__), "monthly_temp.csv")
    update_temperature(temp_csv)

    print(f"Done. {len(rows)} months loaded "
          f"({len(static_monthly)} from static file, {len(v4_monthly)} from v4 API).")
