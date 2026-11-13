"""Residential electricity by Victorian distribution network (DNSP), 2006-2019, from the AER economic benchmarking
regulatory information notice (RIN) responses.

Source: Australian Energy Regulator, economic benchmarking RIN responses of CitiPower, Powercor, Jemena, United Energy
and AusNet Services (distribution): the 2006-13 back-cast response (submitted April 2014) and the annual responses for
2014-2019 (submitted the following April), as published on aer.gov.au. Victorian DNSPs report calendar years.
Downloads are saved in Sandbox/papers/GR4SPArticle_ERSS/data/aer_dnsp_benchmarking/ (README there).

Variables (AER RIN codes, definitions in the AER "Economic benchmarking RIN for distribution network service
providers - Instructions and Definitions", 2013):
  DOPED0501  residential customers energy deliveries (GWh): energy delivered to residential customers from the network
  DOPCN0101  residential customer numbers (average over the year)
  DREV0201   revenue from residential customers ($'000, nominal): network (distribution) revenue, not retail bills
  DOPED0405-0408  energy into the network from residential embedded generation (GWh): rooftop PV exports
  DOPED01    total energy delivered (GWh)

Output (long): simulationSettings/residential_dnsp_VIC.csv with dnsp, year, residential_gwh, residential_customers,
residential_mwh_per_customer, residential_revenue_kaud, residential_export_gwh, total_gwh, source_file.

Usage: python fetch_aer_dnsp_rin.py DOWNLOAD_DIR OUT_CSV [--no-download]
"""
import argparse, csv, os, re, urllib.parse, urllib.request

import openpyxl

AER = "https://www.aer.gov.au"
# (dnsp, first year of the file, path on aer.gov.au). 2006-13 = back-cast; the rest one year each.
FILES = [
    ("CIT", 2006, "/system/files/2026-01/Citipower%202006-13%20-%20Economic%20Benchmarking%20-%20RIN%20response%20%28D15%2068453.3%29.XLSX"),
    ("PCR", 2006, "/system/files/2026-01/Powercor%202006-13%20-%20Economic%20Benchmarking%20RIN%20response%20%28D14-68446.4%29.XLSX"),
    ("JEN", 2006, "/system/files/Jemena%202006-13%20-%20Economic%20Benchmarking%20-%20RIN%20Response%20-%20Consolidated%20-%2030%20April%202014%20-%20PUBLIC%20%28%238260505.3%29.xlsx"),
    ("UED", 2006, "/system/files/United%20Energy%202006-13%20-%20Economic%20Benchmarking%20RIN%20response%20-%20CONSOLIDATED%20-%20revised%205%20June%202016%20-%20PUBLIC%20-%20D14-68640%28V3%29.XLSX"),
    ("AND", 2006, "/system/files/2023-12/SP%20AusNet%20%28D%29%202006-13%20-%20Economic%20Benchmarking%20-%20RIN%20response%20-%20Consolidated%20-%20PUBLIC%288261635.7%29.xlsx"),
    ("CIT", 2014, "/system/files/CitiPower%202014%20-%20Economic%20Benchmarking%20-%20Consolidated%20Information%20-%205%20June%202015%20-%20PUBLIC%289990034.3%29.xlsx"),
    ("CIT", 2015, "/system/files/Copy%20of%20D16%2054034%20%20CitiPower%202015%20-%20Economic%20Benchmarking%20RIN%20-%20email%201%20of%202%20-%20Templates%20-%20CONSOLIDATED%20-%2028%20April%202016%20-%20PUBLIC_0.xlsm"),
    ("CIT", 2016, "/system/files/CitiPower%202016%20-%20Economic%20Benchmarking%20-%20Consolidated%20AMENDED%20-%20PUBLIC%20-%20%239%2C935%2C665.3.xlsx"),
    ("CIT", 2017, "/system/files/CitiPower%202017%20-%20Economic%20benchmarking%20RIN%20response%20-%20Template%20-%20Consolidated%20%281%20of%202%29%20-%2030%20April%202018%20-%20Public%20-%20%28%237599901.3%29.xlsx"),
    ("CIT", 2018, "/system/files/CitiPower%202018%20-%20RIN%20response%20-%20Economic%20Benchmarking%20-%20Consolidated%20-%20PUBLIC%20-%20D19-61787_0.xlsm"),
    ("CIT", 2019, "/system/files/CitiPower%202019%20-%20Economic%20Benchmarking%20-%20RIN%20Response%20-%20Consolidated%20-%2030%20April%202020%20-%20PUBLIC%20%28%2311001066%29.xlsx"),
    ("PCR", 2014, "/system/files/D15%2056695%28V2%29%20%20Powercor%202014%20-%20Economic%20Benchmarking%20-%20Consolidated%20Information%20-%205%20June%202015%20-%20PUBLIC%20PROTECTED.xlsm"),
    ("PCR", 2015, "/system/files/DORIS%20-%20D16-54022%5Bv3%5D%20PowerCor%202015%20-%20Economic%20Benchmarking%20RIN%20-%20email%201%20of%202%20-%20Templates%20-%20CONSOLIDATED%20-%2028%20April%202016%20-%20PUBLIC.XLSM"),
    ("PCR", 2016, "/system/files/DORIS%20-%20D17-54700%5Bv3%5D%20Powercor%202016%20-%20Economic%20Benchmarking%20RIN%20-%20templates%20-%20Consolidated%20Information%20PUBLIC.XLSM"),
    ("PCR", 2017, "/system/files/D18-60251%5Bv3%5D%20PowerCor%202017%20%E2%80%93%20Economic%20benchmarking%20RIN%20response%20%E2%80%93%20Template%20-%20Consolidated%20%281%20of%202%29%20-%2030%20April%202018%20-%20Public.XLSM"),
    ("PCR", 2018, "/system/files/Powercor%202018%20-%20RIN%20response%20-%20Economic%20Benchmarking%20-%20Consolidated%20-%20PUBLIC%20-%20D19-61788_0.xlsm"),
    ("PCR", 2019, "/system/files/Powercor%202019%20-%20Economic%20Benchmarking%20-%20RIN%20Response%20-%20Consolidated%20-%2030%20April%202020%20-%20PUBLIC%20%28%2311001098%29.xlsx"),
    ("JEN", 2014, "/system/files/Jemena%202014%20-%20Economic%20Benchmarking%20-%20Consolidated%20templates%20-%20PUBLIC%20-%20PROTECTED%20-%20%28%2310001867.3%29.xlsx"),
    ("JEN", 2015, "/system/files/Jemena%202015%20-%20Economic%20Benchmarking%20RIN%20-%20Attachment%201%20-%20Templates%20-%20CONSOLIDATED%20-%20revised%2016%20June%202016%20-%20PUBLIC%20%28%238160966.3%29.xlsx"),
    ("JEN", 2016, "/system/files/Jemena%202016%20-%20Economic%20benchmarking%20RIN%20-Attachment%201-1A%20-%20Information%20templates%20%28%289939111.5%29.xlsx"),
    ("JEN", 2017, "/system/files/Jemena%202017%20-%20Economic%20Benchmarking%20RIN%20response%20-%20Attachment%201-1A%20-%20Information%20templat%287593911.5%29.xlsx"),
    ("JEN", 2018, "/system/files/Jemena%202018%20-%20Economic%20Benchmarking%20-%20RIN%20response%20-%20Consolidated%20-%2030%20April%202019%20-%20PUBLIC%2810210663.5%29.xlsx"),
    ("JEN", 2019, "/system/files/Jemena%202019%20-%20Economic%20Benchmarking%20-%20RIN%20Response%20-%20Consolidated%20-%2030%20April%202020%20-%20PUBL%2811002674.3%29.xlsx"),
    ("UED", 2014, "/system/files/United%20Energy%202014%20-%20RIN%20Benchmarking%20response%20-%20Consolidated%20-%20revised%205%20June%202015%20-%20PUBLIC%28%239990750.6%29.xlsx"),
    ("UED", 2015, "/system/files/United%20Energy%202015%20-%20Economic%20Benchmarking%20RIN%20response%20-%20CONSOLIDATED%20-%20revised%202%288165932.6%29.xlsx"),
    ("UED", 2016, "/system/files/United%20Energy%202016%20-%20Economic%20Benchmarking%20RIN%20-%20templates%20-%20PUBLIC%289944676.5%29.xlsx"),
    ("UED", 2017, "/system/files/United%20Energy%202017%20%E2%80%93%20Economic%20benchmarking%20RIN%20response%20%E2%80%93%20Template%20-%20Consolidated%20%281%20of%202%29%20-%2030%20April%202018%20-%20Public%20-%20D18-60357%28v5%29.XLSM"),
    ("UED", 2018, "/system/files/United%20Energy%202018%20-%20Economic%20Benchmarking%20-%20Consolidated%20AMENDED%20-%20PUBLIC%20-%2010203475%20V4.xlsx"),
    ("UED", 2019, "/system/files/United%20Energy%202019%20-%20Economic%20Benchmarking%20-%20RIN%20Response%20-%20Consolidated%20-%2030%20April%202020%20-%20PUBLIC%20%28%2311000999%29.xlsx"),
    ("AND", 2014, "/system/files/2026-01/AusNet%20Services%20%28ED%29%202014%20-%20Economic%20Benchmarking%20-%20RIN%20response%20%289991474.8%29.xlsx"),
    ("AND", 2015, "/system/files/2026-01/AusNet%20Services%20%28ED%29%202015%20-%20Economic%20Benchmarking%20-%20RIN%20Response%20%288177528.9%29.xlsx"),
    ("AND", 2016, "/system/files/2026-01/AusNet%20Services%20%28ED%29%202016%20-%20Economic%20Benchmarking%20RIN%20response%20%289940149.7%29.xlsx"),
    ("AND", 2017, "/system/files/2026-01/AusNet%20Services%20%28ED%29%202017%20-%20Economic%20Benchmarking%20RIN%20%288395607.7%29.xlsx"),
    ("AND", 2018, "/system/files/AusNet%20Electricity%20Services%202018%20-%20Economic%20Benchmarking%20-%20Templates%20-%20Consolidated%20-%2030%20April%202019%20-%20PUBLIC%2810209703.6%29.xlsx"),
    ("AND", 2019, "/system/files/2025-09/Ausnet%20%28D%29%202019%20-%20Economic%20Benchmarking%20-%20RIN%20Response%20-%20Consolidated%20-%2029%20April%202020%20-%20PUBLIC%2810999436.6%29.xlsx"),
]
UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36"


def local_name(dnsp, year, path):
    ext = os.path.splitext(urllib.parse.unquote(path))[1].lower()
    span = "2006-13" if year == 2006 else str(year)
    return f"{dnsp}_{span}_EB_RIN{ext}"


def download(d):
    for dnsp, year, path in FILES:
        out = os.path.join(d, local_name(dnsp, year, path))
        if os.path.exists(out) and os.path.getsize(out) > 10000:
            continue
        req = urllib.request.Request(AER + path, headers={"User-Agent": UA})
        with urllib.request.urlopen(req, timeout=120) as r, open(out, "wb") as f:
            f.write(r.read())
        print("downloaded", out, os.path.getsize(out))


def as_num(v):
    if isinstance(v, (int, float)):
        return float(v)
    try:
        return float(str(v).replace(",", ""))
    except ValueError:
        return None


# Matched on the variable label, because the 2006-13 templates of some DNSPs use older codes (ED_OPD514_0000 ...).
# Exports: the four rows "... from residential embedded generation" are summed.
LABELS = [("residential_gwh", lambda s: s.startswith("residential customers energy deliver")),
          ("residential_customers", lambda s: s.startswith("residential customer numbers")),
          ("residential_revenue_kaud", lambda s: s.startswith("revenue from residential customers")),
          ("residential_export_gwh", lambda s: "from residential embedded generation" in s),
          ("total_gwh", lambda s: s.startswith("total energy delivered"))]
# the current codes, for files whose labels are worded differently
CODES = {"doped0501": "residential_gwh", "dopcn0101": "residential_customers", "drev0201": "residential_revenue_kaud",
         "doped0405": "residential_export_gwh", "doped0406": "residential_export_gwh",
         "doped0407": "residential_export_gwh", "doped0408": "residential_export_gwh", "doped01": "total_gwh"}


def extract(path, first_year):
    """{year: {variable: value}}. Back-cast files: the first column block headed by years (actuals; a later block
    of the same years holds estimates). Annual files: the first number after the label, for the file's year."""
    wb = openpyxl.load_workbook(path, data_only=True)
    found = {}
    for ws in wb.worksheets:
        years = None
        for r in ws.iter_rows(values_only=True):
            cells = list(r)
            yr = [(j, int(v)) for j, v in enumerate(cells) if isinstance(v, (int, float)) and 2005 <= v <= 2020
                  and float(v).is_integer()]
            if len(yr) >= 3 and years is None:
                years = {}
                for j, y in yr:
                    years.setdefault(y, j)  # first block = actuals
            k, var = None, None
            for j, v in enumerate(cells[:4]):
                if isinstance(v, str):
                    s = " ".join(v.lower().split())
                    var = CODES.get(s) or next((n for n, test in LABELS if test(s)), None)
                    if var:
                        k = j
                        break
            if var is None or (var != "residential_export_gwh" and var in found):
                continue
            if first_year == 2006 and years:
                series = {y: as_num(cells[j]) for y, j in years.items() if j < len(cells)}
            else:
                nums = [as_num(v) for v in cells[k + 2:] if as_num(v) is not None]
                series = {first_year: nums[0]} if nums else {}
            if var == "residential_export_gwh":
                prev = found.get(var, {})
                series = {y: (prev.get(y) or 0) + (v or 0) for y, v in series.items()}
            found[var] = series
    out = {}
    for var, series in found.items():
        for y, v in series.items():
            out.setdefault(y, {})[var] = v
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("download_dir")
    ap.add_argument("out_csv")
    ap.add_argument("--no-download", action="store_true")
    a = ap.parse_args()
    os.makedirs(a.download_dir, exist_ok=True)
    if not a.no_download:
        download(a.download_dir)
    rows = []
    for dnsp, year, path in FILES:
        name = local_name(dnsp, year, path)
        for y, v in sorted(extract(os.path.join(a.download_dir, name), year).items()):
            if not (2006 <= y <= 2019) or (year == 2006 and y > 2013):
                continue
            exp = v.get("residential_export_gwh")
            # some files report revenue in dollars, not $'000: put all in $'000
            rev = v.get("residential_revenue_kaud")
            if rev is not None and rev > 5e6:
                rev /= 1000
            gwh, cust = v.get("residential_gwh"), v.get("residential_customers")
            rows.append(dict(dnsp=dnsp, year=y, residential_gwh=gwh, residential_customers=cust,
                             residential_mwh_per_customer=(1000 * gwh / cust if gwh and cust else None),
                             residential_revenue_kaud=rev, residential_export_gwh=exp, total_gwh=v.get("total_gwh"),
                             source_file=name))
    rows.sort(key=lambda r: (r["dnsp"], r["year"]))
    with open(a.out_csv, "w", newline="") as f:
        f.write("# Residential electricity by Victorian DNSP, AER economic benchmarking RIN responses "
                "(scripts/data/fetch_aer_dnsp_rin.py). Calendar years.\n")
        w = csv.DictWriter(f, fieldnames=list(rows[0]))
        w.writeheader()
        for r in rows:
            w.writerow({k: (f"{v:.4f}" if isinstance(v, float) else v) for k, v in r.items()})
    by_year = {}
    for r in rows:
        t = by_year.setdefault(r["year"], [0, 0, 0, 0])
        t[0] += r["residential_gwh"] or 0
        t[1] += r["residential_customers"] or 0
        t[2] += r["residential_export_gwh"] or 0
        t[3] += 1
    for y, (g, c, e, n) in sorted(by_year.items()):
        print(y, f"{n} DNSPs  {g / 1000:.2f} TWh  {c / 1e6:.3f} M customers  {1000 * g / c if c else 0:.2f} MWh/cust"
              f"  exports {e:.0f} GWh")


if __name__ == "__main__":
    main()
