"""
Victorian small-scale solar installations by month, 2011-01 to 2019-12, from the Clean Energy Regulator's postcode
workbooks, as a CSV the model reads over the database table (N23, 14 Nov 2026).

Why: solar_installation_monthly (APVI, from the same CER registrations) stops in September 2019, and September is
partial: 270 MW of 2019 installations against CER's 461 MW. 2011-2018 agree within 0.5 % a year.

Inputs (download by hand; plain GET, no auth):
  https://cer.gov.au/document/sres-postcode-data-capacity-2011-to-present-and-totals       (sheet SGU-Solar)
  https://cer.gov.au/document/sres-postcode-data-installations-2011-to-present-and-totals  (sheet SGU-Solar)
Each is a postcode-by-month matrix of new capacity (kW, rated power output) or new installations, one column per
month. Victoria: postcodes 3000-3999 and 8000-8999 (as fetch_cer_solar.py).

The workbooks are revised as late registrations arrive (up to 12 months), so the values are the final record, not what
was published in early 2020. For the 1999-2019 validation the final record is the observation.

Usage: python scripts/data/cer_solar_vic_csv.py CAPACITY_XLSX INSTALLATIONS_XLSX OUT_CSV
"""
import re
import sys

import pandas as pd

FIRST, LAST = "2011-01", "2019-12"


def vic_monthly(path):
    x = pd.ExcelFile(path)
    df = pd.read_excel(x, sheet_name="SGU-Solar", header=None)
    hr = df.index[df.apply(lambda r: r.astype(str).str.contains("Postcode", case=False).any(), axis=1)][0]
    df.columns = df.iloc[hr]
    df = df.iloc[hr + 1:]
    pc = pd.to_numeric(df.iloc[:, 0], errors="coerce")
    vic = df[((pc >= 3000) & (pc <= 3999)) | ((pc >= 8000) & (pc <= 8999))]
    out = {}
    for c in df.columns[1:]:
        m = re.match(r"(\w{3}) (\d{4}) - ", str(c))
        if m:
            out[pd.Timestamp(f"{m.group(1)} {m.group(2)}")] = pd.to_numeric(vic[c], errors="coerce").sum()
    return pd.Series(out).sort_index()


def main(cap_xlsx, inst_xlsx, out_csv):
    kw = vic_monthly(cap_xlsx)[FIRST:LAST]
    n = vic_monthly(inst_xlsx)[FIRST:LAST]
    assert len(kw) == len(n) == 108, (len(kw), len(n))
    df = pd.DataFrame({"year": kw.index.year, "month": kw.index.month,
                       "number_installations": n.round().astype(int).values,
                       "aggregated_capacity_kw": kw.round().astype(int).values})
    df["system_capacity"] = (df.aggregated_capacity_kw / df.number_installations).round(2)
    df["source"] = "CER SRES postcode data, SGU-Solar, VIC postcodes"
    df.to_csv(out_csv, index=False)
    print(df.groupby("year")[["number_installations", "aggregated_capacity_kw"]].sum())


if __name__ == "__main__":
    main(*sys.argv[1:4])
