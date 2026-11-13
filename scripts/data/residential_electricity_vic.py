"""Victorian residential electricity use by financial year, from the Australian Energy Statistics 2020, Table F3.

Source: Department of Industry, Science, Energy and Resources, Australian Energy Statistics 2020, Table F
("Energy consumption in Victoria, by industry and fuel type, energy units"), energy.gov.au, Australian Energy Update
2020 page (published September 2020, data to 2018-19). Saved as
Sandbox/papers/GR4SPArticle_ERSS/data/aes/Australian Energy Statistics 2020 Table F.xlsx.

Columns written (TWh, 1 PJ = 1/3.6 TWh):
  fy                     financial year ending 30 June (2019 = 2018-19)
  residential_twh        Residential sector, Electricity
  residential_solar_twh  Residential sector, Solar energy (solar hot water and rooftop PV used in the home)
  total_twh              all sectors, Electricity
  electricity_sector_twh Division D (electricity, gas, water and waste services), Electricity: power stations' own use
                         and network losses
  final_twh              total minus Division D: electricity used by final consumers
  residential_share      residential_twh / final_twh

Usage: python residential_electricity_vic.py XLSX OUT_CSV
"""
import argparse, csv

import openpyxl

SKIP = ("Fuels consumed", "Derived fuels produced", "Energy consumption")


def read(path, sheet="VIC"):
    rows = list(openpyxl.load_workbook(path, read_only=True, data_only=True)[sheet].iter_rows(values_only=True))
    years = rows[6]
    found, sec = {}, None
    for r in rows:
        lab = str(r[1]) if r[1] else ""
        if lab and not lab.startswith("    ") and lab.strip() not in SKIP:
            sec = lab.strip()
        if sec and lab.startswith("    ") and lab.strip() in ("Electricity", "Solar energy"):
            found.setdefault((sec, lab.strip()), r)
    div_d = next(k for k in found if k[0].startswith("Div. D") and k[1] == "Electricity")
    out = []
    for j, y in enumerate(years):
        if not y or not str(y)[:4].isdigit():
            continue
        twh = lambda key: float(found[key][j] or 0) / 3.6
        res, sol, tot, d = (twh(("Residential", "Electricity")), twh(("Residential", "Solar energy")),
                            twh(("Total", "Electricity")), twh(div_d))
        out.append(dict(fy=int(str(y)[:4]) + 1, residential_twh=res, residential_solar_twh=sol, total_twh=tot,
                        electricity_sector_twh=d, final_twh=tot - d, residential_share=res / (tot - d)))
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("xlsx")
    ap.add_argument("out_csv")
    a = ap.parse_args()
    rows = read(a.xlsx)
    with open(a.out_csv, "w", newline="") as f:
        f.write("# Victorian residential electricity, AES 2020 Table F3 (scripts/data/residential_electricity_vic.py)\n")
        w = csv.DictWriter(f, fieldnames=list(rows[0]))
        w.writeheader()
        for r in rows:
            w.writerow({k: (f"{v:.4f}" if isinstance(v, float) else v) for k, v in r.items()})
    for r in rows:
        if r["fy"] >= 1999:
            print(r["fy"], f"{r['residential_twh']:.2f} TWh", f"share {r['residential_share']:.3f}")


if __name__ == "__main__":
    main()
