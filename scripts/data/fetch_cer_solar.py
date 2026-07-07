"""
Download CER postcode solar installation data and load into
gr4spdb.solar_installation_monthly.

CER publishes two cumulative workbooks (no separate quarterly files anymore,
and no auth needed — a plain GET on the document page returns the .xlsx):
  - sres-postcode-data-capacity-2011-to-present-and-totals
  - sres-postcode-data-installations-2011-to-present-and-totals

Each is a wide postcode-by-month matrix (sheet "SGU-Solar"): one row per
postcode, one column per month from Jan 2011 to present holding that
month's new capacity (kW) or new installation count — not a running total
(the workbook's own "Total" column is the running total, used here only
implicitly since it's not part of the target range).

Victoria postcodes: 3000-3999 and 8000-8999.

Run from project root:
  python scripts/data/fetch_cer_solar.py

Gap filled: 2020-01 to 2025-12.
"""

import io
import os
import re
import requests
import pandas as pd
import psycopg2
from dotenv import load_dotenv

load_dotenv()

DB_URL = os.environ["DB_URL"]
DELETE_FROM_YEAR = 2020
END_YEAR = 2025

CER_URLS = {
    "capacity":      "https://cer.gov.au/document/sres-postcode-data-capacity-2011-to-present-and-totals",
    "installations": "https://cer.gov.au/document/sres-postcode-data-installations-2011-to-present-and-totals",
}

# Victoria postcode ranges
VIC_POSTCODE_RANGES = [(3000, 3999), (8000, 8999)]

MONTH_COL_RE = re.compile(r"^([A-Za-z]{3} \d{4}) - ")


def is_vic_postcode(postcode):
    try:
        pc = int(postcode)
        return any(lo <= pc <= hi for lo, hi in VIC_POSTCODE_RANGES)
    except (ValueError, TypeError):
        return False


def download_workbook(url):
    print(f"  Downloading {url}...", end=" ", flush=True)
    r = requests.get(url, timeout=120)
    r.raise_for_status()
    print(f"{len(r.content) // 1024} KB")
    return r.content


def monthly_vic_totals(xlsx_bytes, value_label):
    """Read the wide postcode-by-month sheet and return VIC-wide monthly
    totals as a (year, month) -> total Series."""
    df = pd.read_excel(io.BytesIO(xlsx_bytes), sheet_name="SGU-Solar", header=3, engine="openpyxl")

    postcode_col = df.columns[0]
    df[postcode_col] = pd.to_numeric(df[postcode_col], errors="coerce")
    vic = df[df[postcode_col].apply(is_vic_postcode)]

    month_cols = [c for c in df.columns if MONTH_COL_RE.match(str(c))]
    totals = vic[month_cols].apply(pd.to_numeric, errors="coerce").sum()

    rows = []
    for col, total in totals.items():
        month_label = MONTH_COL_RE.match(col).group(1)
        period = pd.Period(month_label, freq="M")
        if DELETE_FROM_YEAR <= period.year <= END_YEAR:
            rows.append({"year": period.year, "month": period.month, value_label: total})

    return pd.DataFrame(rows).set_index(["year", "month"])[value_label]


def load_to_db(monthly):
    conn = psycopg2.connect(DB_URL)
    cur  = conn.cursor()

    print(f"\nDeleting solar_installation_monthly rows from year >= {DELETE_FROM_YEAR}...")
    cur.execute("DELETE FROM solar_installation_monthly WHERE year >= %s", (DELETE_FROM_YEAR,))

    print(f"Inserting {len(monthly)} monthly rows...")
    for (year, month), row in monthly.iterrows():
        cur.execute(
            """INSERT INTO solar_installation_monthly
               (year, month, number_installations, aggregated_capacity_kw, system_capacity)
               VALUES (%s, %s, %s, %s, %s)""",
            (int(year), int(month),
             int(row["number_installations"]), int(row["aggregated_capacity_kw"]),
             float(row["system_capacity"])),
        )

    conn.commit()
    cur.close()
    conn.close()
    print("solar_installation_monthly loaded successfully.")


if __name__ == "__main__":
    print("Fetching CER postcode workbooks...")
    capacity_bytes      = download_workbook(CER_URLS["capacity"])
    installations_bytes = download_workbook(CER_URLS["installations"])

    print("Aggregating Victoria monthly totals...")
    capacity_monthly      = monthly_vic_totals(capacity_bytes, "aggregated_capacity_kw")
    installations_monthly = monthly_vic_totals(installations_bytes, "number_installations")

    monthly = pd.concat([installations_monthly, capacity_monthly], axis=1).dropna()
    monthly["aggregated_capacity_kw"] = monthly["aggregated_capacity_kw"].round(0).astype(int)
    monthly["number_installations"]   = monthly["number_installations"].round(0).astype(int)
    monthly["system_capacity"] = (
        monthly["aggregated_capacity_kw"] / monthly["number_installations"]
    ).round(4)

    load_to_db(monthly)
    print(f"Done. {len(monthly)} months loaded.")
