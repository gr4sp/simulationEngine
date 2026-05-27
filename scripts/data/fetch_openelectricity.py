"""
Fetch monthly generation data from Open Electricity API (api.openelectricity.org.au)
and load into gr4spdb.generation_consumption_historic for 2020-10-01 to 2025-12-31.

Setup:
  Copy .env.example to ../../.env and fill in OPENELECTRICITY_API_KEY and DB_URL.
  pip install -r requirements.txt

Run from project root:
  python scripts/data/fetch_openelectricity.py
"""

import os
import sys
import requests
import pandas as pd
import psycopg2
from dotenv import load_dotenv

load_dotenv()

API_KEY = os.environ["OPENELECTRICITY_API_KEY"]
DB_URL  = os.environ["DB_URL"]

BASE_URL = "https://api.openelectricity.org.au/v4"
HEADERS  = {"Authorization": f"Bearer {API_KEY}", "Accept": "application/json"}

START = "2020-10-01"
END   = "2025-12-31"

# Open Electricity fuel type → (gwh column, dollargwh column)
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


def fetch_energy():
    """Fetch monthly energy (GWh) by fuel type for VIC1."""
    print("Fetching energy data from Open Electricity...")
    r = requests.get(
        f"{BASE_URL}/energy/network/NEM/VIC1",
        headers=HEADERS,
        params={"interval": "month", "start": START, "end": END},
        timeout=60,
    )
    r.raise_for_status()
    return r.json()


def fetch_price():
    """Fetch monthly volume-weighted price for VIC1."""
    print("Fetching price data from Open Electricity...")
    r = requests.get(
        f"{BASE_URL}/price/network/NEM/VIC1",
        headers=HEADERS,
        params={"interval": "month", "start": START, "end": END},
        timeout=60,
    )
    r.raise_for_status()
    return r.json()


def parse_energy(data):
    """
    Parse API response into a DataFrame indexed by date with one column per fuel type.
    Handles both {'data': [...]} and {'results': [...]} response shapes.
    """
    records = data.get("data") or data.get("results") or []
    if not records:
        print("WARNING: No energy records returned. Check API response shape:")
        print(list(data.keys()))
        return pd.DataFrame()

    rows = []
    for rec in records:
        date = pd.to_datetime(rec.get("date") or rec.get("interval") or rec.get("time"))
        fuel = rec.get("fuel_tech") or rec.get("fuel_type") or rec.get("type")
        energy_gwh  = rec.get("energy")   or rec.get("value") or 0.0
        revenue     = rec.get("revenue")  or rec.get("cost")  or None
        dollar_gwh  = (revenue / energy_gwh) if (revenue and energy_gwh) else None
        rows.append({"date": date, "fuel": fuel, "gwh": energy_gwh, "dollar_gwh": dollar_gwh})

    df = pd.DataFrame(rows)
    return df


def parse_price(data):
    """Return a Series of volume-weighted price indexed by date."""
    records = data.get("data") or data.get("results") or []
    if not records:
        print("WARNING: No price records returned.")
        return pd.Series(dtype=float)

    rows = []
    for rec in records:
        date  = pd.to_datetime(rec.get("date") or rec.get("interval") or rec.get("time"))
        price = rec.get("price") or rec.get("value") or None
        rows.append({"date": date, "price": price})

    s = pd.DataFrame(rows).set_index("date")["price"]
    return s


def build_rows(energy_df, price_series):
    """Combine energy and price into one row per month matching the DB schema."""
    if energy_df.empty:
        return []

    dates = energy_df["date"].unique()
    rows  = []

    for date in sorted(dates):
        month_df = energy_df[energy_df["date"] == date]
        row = {"date": date.date(), "temperaturec": None,
               "volumeweightedprice_dollarmwh": price_series.get(date)}

        # Initialise all fuel columns to None
        for gwh_col, dollar_col in FUEL_MAP.values():
            row[gwh_col]    = None
            row[dollar_col] = None

        for _, rec in month_df.iterrows():
            fuel = rec["fuel"]
            if fuel in FUEL_MAP:
                gwh_col, dollar_col = FUEL_MAP[fuel]
                row[gwh_col]    = rec["gwh"]
                row[dollar_col] = rec["dollar_gwh"]

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
        cur.execute(insert_sql, [row[c] for c in cols])

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
    energy_data  = fetch_energy()
    price_data   = fetch_price()
    energy_df    = parse_energy(energy_data)
    price_series = parse_price(price_data)
    rows         = build_rows(energy_df, price_series)
    load_to_db(rows)

    # Update temperature if ERA5 output already exists
    temp_csv = os.path.join(os.path.dirname(__file__), "monthly_temp.csv")
    update_temperature(temp_csv)

    print("Done. Run fetch_era5.py if temperature column is still NULL.")
